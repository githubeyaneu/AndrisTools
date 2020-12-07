package eu.eyan.duplicates


import java.io.File
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import eu.eyan.log.Log
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import eu.eyan.util.rx.lang.scala.ObservablePlus
import eu.eyan.util.rx.lang.scala.ObservablePlus.{ObservableImplicitBoolean, not}
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicitT
import eu.eyan.util.rx.lang.scala.ObserverPlus.ObserverImplicit
import eu.eyan.util.scala.LongPlus.LongImplicit
import eu.eyan.util.scala.{TryCatch, TryCatchThrowable}
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JProgressBarPlus.ResetProgress
import eu.eyan.util.swing.SwingPlus
import eu.eyan.util.swing.panelbuilder.{Click, JPanelBuilder}
import eu.eyan.util.time.Timer.timerStart
import javax.swing.{JFrame, SwingUtilities}
import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

import scala.collection.mutable

object DeleteDuplicatesDirs {
  val TITLE = "Delete duplicates dirs"
}

class DeleteDuplicatesDirs {

  case class DirsToSearch(dirsToSearch: List[(String, Boolean, Boolean)])
  private val dirsToSearch$$ = BehaviorSubject[DirsToSearch]()
  private val minSizeText$$ = BehaviorSubject[String]()
  private val fullHash$$ = BehaviorSubject[Boolean]()

  private val progressInput$$ = BehaviorSubject[ResetProgress]()
  private val progressFormat$$ = BehaviorSubject[String]()
  private val progressMaximum$$ = BehaviorSubject[Int]()
  private val progressValue$$ = BehaviorSubject[Int]()
  def resetProgress(format: Observable[String], maxValue: Int, progressValue$: Observable[Int]) = { //TODO put somehow into JProgressBarPlus
    format.subscribe(onNext = format => progressFormat$$.onNext(format))
    progressMaximum$$.onNext(maxValue)
    progressValue$$.onNext(0)
    progressValue$.subscribe(onNext = value => progressValue$$.onNext(value))
  }
  private val allowDelete$$ = BehaviorSubject[Boolean]()
  private val logsAppender$$ = BehaviorSubject[String]()
  private val logsSetter$$ = BehaviorSubject[String]()
  private def clearLogs() = logsSetter$$.onNext("")
  private val isInProgress$$ = BehaviorSubject(false)
  private val isAllowDeleteSelected$$ = BehaviorSubject(false)

  private val findDuplicatesClicked$$ = BehaviorSubject[Click]() // no BehSub
  private val deleteDuplicatesClicked$$ = BehaviorSubject[Click]()


  val dirsMultiFieldComponent = new MultiFieldJTextFieldWithCheckbox("dirsToSearch", 50)
  val panel = JPanelBuilder().withBorders.withSeparators
    .newColumnFPG
    .newRow
    .addSeparatorWithTitle("Directories to search")
    .newRow.addMultiEditor(dirsMultiFieldComponent).setValues(List(("", false, false))).remember("dirsToSearch").onChanged(dirsToSearch$$.pam(mapper = DirsToSearch))
    .newRow.addPanelBuilder(_.withSeparators
      .addLabel().text("Min bytes:")
      .newColumn.addTextField().text("").size(7).remember("minSize").onTextChanged(minSizeText$$)
      .newColumnFPG.addButton().text("Find duplicates").onAction(findDuplicatesClicked$$).enabled(not(isInProgress$$))
      .newColumn.addCheckBox().text("Full hash").onSelectionChanged(fullHash$$)
    )
    .newRow.addPanelBuilder(_.withSeparators
      .addCheckBox().text("Enable").tooltipText("Enable \"Delete duplicates\" button").onSelectionChanged(allowDelete$$)
      .newColumnFPG.addButton().text("Delete duplicates").onAction(deleteDuplicatesClicked$$).enabled(allowDelete$$.and(not(isInProgress$$)))
    )
    .newRow.addProgressBar().format(progressFormat$$).value(progressValue$$).maximum(progressMaximum$$)
    .newRowFPGForTextArea.addTextArea().textAppender(logsAppender$$).text(logsSetter$$)
    .getPanel

  dirsToSearch$$.subscribe( _ => Option(SwingUtilities.windowForComponent(panel)).map(_.asInstanceOf[JFrame]).foreach(_.resizeAndBack))//TODO better way???

  case class HandleDuplicatesJobParams(private val params: (DirsToSearch, Boolean, String)) {
    def dirPaths = dirsToSearch.dirsToSearch.filter(_._3).map(_._1).toStream
    def fullHash = params._2
    def minSize = TryCatch(minSizeText.toLong, 0L)

    private def dirsToSearch = params._1
    private def minSizeText = params._3
  }

  val params$ = ObservablePlus.combineLatest(dirsToSearch$$, fullHash$$, minSizeText$$).map(HandleDuplicatesJobParams)

  trait DeleteOption
  case object WithDelete extends DeleteOption
  case object DontDelete extends DeleteOption

  findDuplicatesClicked$$.takeLatestOf(params$).subscribe(p => startJobAsync(findDuplicates(p, DontDelete, logsAppender$$, resetProgress)))
  deleteDuplicatesClicked$$.takeLatestOf(params$).subscribe(p => startJobAsync(findDuplicates(p, WithDelete, logsAppender$$, resetProgress)))
  deleteDuplicatesClicked$$.subscribe( _ => dirsMultiFieldComponent.clearAllowDelete())

  private def startJobAsync(job: => Unit) = {
    clearLogs()
    isInProgress$$.onNext(true)
    isAllowDeleteSelected$$.onNext(false)
    SwingPlus.runInWorker(job, isInProgress$$.onNext(false))
  }


  private def findDuplicates(params: HandleDuplicatesJobParams, deleteOption: DeleteOption, logsAppender: BehaviorSubject[String], resetProgress: ResetProgress) = {
    def log(msg: Any*) = logsAppender.onNext(msg.mkString(" ") + "\n")
    val timer = timerStart()

    // val deletablePaths = dirsMultiField.getValues.filter(_._2).map(_._1)



    type ParentDir = Option[Dir]

    implicit class DirFilImplicit(file: File) {
      def toDirFil(parent: ParentDir): DirFil = if (file.isFile) Fil(file, parent) else Dir(file, parent)
    }

    abstract class DirFil(val parent: ParentDir, val path: File) {

      parent.foreach(_.addChild(this))

      var visibleForDelete = true
      protected val size = new AtomicLong
      private var hash: Option[String] = None
      private var hashWithName: Option[String] = None

      //abstract
      def isFile: Boolean

      def isDirectory: Boolean

      def getSize: Long

      // public
      def asFileOption = if (isFile) Option(this.asFil) else None

      def asDirOption = if (isDirectory) Option(this.asDir) else None

      def hasParent = parent.nonEmpty

      def asDir = this.asInstanceOf[Dir]

      def asFil = this.asInstanceOf[Fil]

      def getParent = parent

      def getHash = hash

      def getHashWithName = hashWithName

      def isHashEmpty = hash.isEmpty

      def hashNonEmpty = !isHashEmpty

      def listFilesInGroups(groupSize: Int): Array[Array[DirFil]] = {
        val ret: Array[DirFil] = if (isDirectory) {
          val subFiles = asDirOption.map(_.path.listFiles).orNull
          if (subFiles == null) {
            Log.error("null received from listFiles of " + path)
            Array[DirFil]()
          }
          else subFiles.map(file => if (file.isFile) Fil(file, asDirOption) else Dir(file, asDirOption))
        }
        else Array[DirFil]()

        ret.grouped(groupSize).toArray
      }

      override def toString =
        "Vis: " + visibleForDelete + (if (visibleForDelete) " " else "") +
          ", Hash: " + hashShort + ", WithName: " + hashWithNameShort +
          ", Size: " + String.format("%10s", size.get.toSize) +
          ", name: " + String.format("%-30s", path.getName) +
          ", path: " + path +
          "   ,   parent: " + parent.map(_.path)

      //protected - private
      protected def setHash(h: Option[String]) = {
        hash = h
        hashWithName = hash.map(_.createHash(path.getName))
        parent.foreach(_.addSubHash(hash))
      }

      private def hashShort = hash.map(_.substring(0, 6) + "...").getOrElse("-no hash-")

      private def hashWithNameShort = hashWithName.map(_.substring(0, 6) + "...").getOrElse("-no hash-")
    }

    case class Dir(private val dir: File, private val par: ParentDir) extends DirFil(par, dir) {
      val subHashes = mutable.Set[String]()
      def addSubHash(hash: Option[String]):Unit = {
        hash.foreach(hash => subHashes.synchronized(subHashes+=hash))
        parent.foreach(_.addSubHash(hash))
      }

      parent.foreach(_.addSizeFromFile(size.get))
      parent.foreach(_.incrementSubDirCounter())

      val numberOfSubFiles = new AtomicInteger
      val numberOfSubDirs = new AtomicInteger

      def getSize = size.get

      def isDirectory = true

      def isFile = false

      val children = mutable.MutableList[DirFil]()

      def addChild(dirFil: DirFil) = {
        children.synchronized {
          children += dirFil
        }
      }

      def addSizeFromFile(fileSize: Long): Unit = {
        size.addAndGet(fileSize)
        parent.foreach(_.addSizeFromFile(fileSize))
      }

      def incrementSubFileCounter(): Unit = {
        numberOfSubFiles.incrementAndGet
        parent.foreach(_.incrementSubFileCounter())
      }

      def incrementSubDirCounter(): Unit = {
        numberOfSubDirs.incrementAndGet
        parent.foreach(_.incrementSubDirCounter())
      }

      override def toString = String.format("%-12s", "Dir(" + children.size + "," + numberOfSubFiles + "): ") + super.toString

      def hashCreate() = setHash(Some("DIR".createHash(children.map(_.getHashWithName.get).sorted: _*)))
    }

    case class Fil(private val fil: File, private val par: ParentDir) extends DirFil(par, fil) {
      def getSize = size.get

      def isDirectory = false

      def isFile = true

      def hashFull(bytesReadCallback: Long => Unit) = fil.hashFull(bytesReadCallback)

      def hashFast(bytesReadCallback: Long => Unit) = fil.hashFast(bytesReadCallback)

      size.addAndGet(fil.length)
      parent.foreach(_.addSizeFromFile(size.get))
      parent.foreach(_.incrementSubFileCounter())

      override def toString = String.format("%-12s", "Fil: ") + super.toString

      def hashCreateFast(bytesReadCallback: Long => Unit) = {
        setHash(TryCatchThrowable(
          Some("FIL FAST".createHash(fil.hashFast(bytesReadCallback))),
          (t: Throwable) => {
            Log.error(t)
            None
          }))
      }

      def hashCreateFull(bytesReadCallback: Long => Unit) = {
        setHash(TryCatchThrowable(
          Some("FIL FULL".createHash(fil.hashFull(bytesReadCallback))),
          (t: Throwable) => {
            Log.error(t)
            None
          }))
      }
    }

    type Chunk = Array[DirFil]
    val EMPTY_CHUNK = Array[DirFil]()
    val CHUNK_SIZE = 100


    val fileCt$ = BehaviorSubject(0)
    val fileCt = new AtomicInteger
    resetProgress("%,d files and dirs".just, Int.MaxValue, fileCt$)


    def explore(aChunkOfFilesAndDirs: Chunk, syncOrAsync: Chunk => Chunk): Chunk = {
      def listFilesAndExecuteAsync(theFileOrDirItself: DirFil) = theFileOrDirItself +:
        theFileOrDirItself
          .listFilesInGroups(CHUNK_SIZE)
          .map(syncOrAsync)
          .flatten

      try aChunkOfFilesAndDirs flatMap listFilesAndExecuteAsync
      finally fileCt$.onNext(fileCt.incrementAndGet)
    }


    log(params.dirPaths.mkString)
    val initialDirs = params.dirPaths.map(_.asDir.toDirFil(None)).toArray
    val chunks = new ParallelExecutor(explore, EMPTY_CHUNK).executeAndWaitUntilDone(initialDirs)
    fileCt$.onCompleted

    log("Explore done", timer.timerElapsed)
    //    log("Chunks: ", chunks.size)

    val dirFils = chunks.flatten
    //    log("ParentCount", dirFils.filter(_.hasParent).size.format)

    val files = dirFils.filter(_.isFile).map(_.asFil)
    val dirs = dirFils.filter(_.isDirectory).map(_.asDir)

    //    log("Chunks", "Size: ", dirFils.size.format, "Files: ", files.size.format, "Dirs: ", dirs.size.format)
    //    log("Chunks", "Bytes", dirFils.filter(_.isFile).map(_.asFil.size).sum.format, Timer.timerElapsed)

    //    log("children " + dirFils.filter(_.isDirectory).map(_.asDir.children.size).sum, Timer.timerElapsed)

    // REAL
    //        val filesAndDirsStreams = dirPaths.flatMap(_.asDir.fileTreeWithItself)
    //        val fds = filesAndDirsStreams.partition(_.isFile)
    //        val fSizes = fds._1.map(_.length).toList
    //        val ds = fds._2.toList
    //        log("REAL ", "Size: ", (fSizes.size + ds.size).format, "Files: ", fSizes.size.format, "Dirs: ", ds.size.format)
    //        log("REAL ", "Bytes", fSizes.sum.format)

    val filesBySizeGroups = files.groupBy(_.getSize)
    log("File size groups ", filesBySizeGroups.size.format, timer.timerElapsed)

    val filesSingle = filesBySizeGroups.filter(_._2.size == 1).values.flatten.toList
    log("Single files", filesSingle.size.format, "size", filesSingle.map(_.getSize).sum.toSize, timer.timerElapsed)

    val filesMultiGroups = filesBySizeGroups.filter(_._2.size != 1).values.toList.sortWith((l1, l2) => l1.head.getSize < l2.head.getSize)
    val multiFiles = filesMultiGroups.flatten
    val multiFilesCt = multiFiles.size
    val multiFilesSum = multiFiles.map(_.getSize).sum
    log("Multi files", multiFilesCt.format, "size", multiFilesSum.toSize, timer.timerElapsed)


    val hashReadCount = new AtomicInteger
    val hashReadCount$ = BehaviorSubject(0)
    val hashReadProgress = new AtomicLong
    val hashReadProgress$ = BehaviorSubject(0L)

    def updateProgress(readBytes: Long) = hashReadProgress$.onNext(hashReadProgress.addAndGet(readBytes))


    if (params.fullHash) {
      resetProgress(hashReadProgress$.map(_.toSize), (multiFilesSum / 1000000L).toInt, hashReadProgress$.map(hashReadProgress => (hashReadProgress / 1000000).toInt))
    } else {
      resetProgress("%,d".just, multiFilesCt, hashReadCount$)
    }

    multiFiles.par.foreach(f =>
      try if (params.fullHash) f.hashCreateFull(updateProgress)
      else f.hashCreateFast(updateProgress)
      finally hashReadCount$.onNext(hashReadCount.incrementAndGet))

    hashReadCount$.onCompleted
    hashReadProgress$.onCompleted
    log("Hashing done", timer.timerElapsed)

    @scala.annotation.tailrec
    def fillDirs() {
      val dirsToFill = dirs.filter(_.isHashEmpty) //.filter(_.children.nonEmpty): Empty dirs has to be hashed too
      val dirsAllChildrenWithHash = dirsToFill.filter(_.children.forall(_.hashNonEmpty))
      log("fillDirs", dirsAllChildrenWithHash.size)
      if (dirsAllChildrenWithHash.nonEmpty) {
        dirsAllChildrenWithHash.foreach(_.hashCreate())
        fillDirs()
      }
    }

    fillDirs()
    log("fillDirsDone", "")


    val fileHashGroups = dirFils.filter(_.isFile).map(_.asFil).filter(_.getHash.nonEmpty).groupBy(_.getHash.get)
//    val dirHashGroups = dirFils.filter(_.isDirectory).map(_.asDir).filter(_.getHashWithName.nonEmpty).groupBy(_.getHashWithName.get)
    val dirHashGroups = dirFils.filter(_.isDirectory).map(_.asDir).filter(_.getHash.nonEmpty).groupBy(_.getHash.get)

    log("Hashgroups done", fileHashGroups.size, dirHashGroups.size, timer.timerElapsed)

    val fileDuplicateGroups = fileHashGroups.values.filter(_.size > 1).toList
    val dirDuplicateGroups = dirHashGroups.values.filter(_.size > 1).toList

    val duplicateFiles = fileDuplicateGroups.flatten.toSet
    val duplicateDirs = dirDuplicateGroups.flatten.toSet
    log("Extract groups done", timer.timerElapsed)

    val duplicateDirFils: Set[DirFil] = duplicateFiles ++ duplicateDirs

    val visibleForDeleteCount = new AtomicInteger
    val visibleForDeleteCount$$ = BehaviorSubject(0)
    resetProgress("%,d".just, duplicateDirFils.size, visibleForDeleteCount$$)
    duplicateDirFils.foreach(dirFil => {
      dirFil.visibleForDelete = !dirFil.getParent.exists(duplicateDirs.contains)
      visibleForDeleteCount$$.onNext(visibleForDeleteCount.incrementAndGet)
    })
    log("Set visibility for deleting done", timer.timerElapsed)

    val duplicateGroups = /*fileDuplicateGroups ++ */ dirDuplicateGroups
    log("Duplicate groups", duplicateGroups.size, timer.timerElapsed)

    val visibleDuplicateGroups = duplicateGroups.filter(_.exists(_.visibleForDelete)).filter(_.exists(_.getSize >= params.minSize))
    log("Visible duplicate groups", visibleDuplicateGroups.size, timer.timerElapsed)

    val visibleDuplicateGroupsSorted = visibleDuplicateGroups.sortBy(l => l.head.getSize)
    log("Visible duplicate groups", visibleDuplicateGroups.size, timer.timerElapsed)

    visibleDuplicateGroupsSorted.map(_.mkString("\r\n")).foreach(l => println(l + "\r\n"))
    log("Printed groups", timer.timerElapsed)

    val dirsContainingDuplicates = visibleDuplicateGroupsSorted.flatten.map(_.path.getParentFile).distinct.sorted
    log("DirsContainingDuplicates", dirsContainingDuplicates.size, timer.timerElapsed)
    dirsContainingDuplicates.foreach(log(_))

    //dirHashGroups

    //NEXT TODO: find dirs that content same with the hash+name but maybe the root dir name is not the same
    //    println; dirFils.foreach(println)


    // TODO empty files
    // TODO dirs with empty files
    // TODO empty dirs ???
    // TODO file outside and in a duplicated dir

    //    System.exit(0)
  }

  //  private def findDuplicates(withDelete: Boolean) = SwingPlus.runInWorker ({
  //
  //    var remainingFilesCt = 0
  //    var deleteCt = 0
  //
  //    val dirsContainingDuplicates = mutable.Set[File]()
  //
  //
  //
  //      hashGroups.foreach { hashFiles =>
  //        if (hashFiles._2.size < 2) logs.appendLater(".")
  //        else {
  //          val files = hashFiles._2.sortBy(f => f.getName.length)
  //          files.map(_.getParentFile).foreach(dirsContainingDuplicates.add)
  //          //          logs.append(files.map(file => ("File", file.length, file.getAbsolutePath)).mkString(N)+N)
  //
  //          val filesToDeleteCandidates = files.filter(fileToDelete => deletablePaths.exists(fileToDelete.getAbsolutePath.contains(_)))
  //          //          logs.append(filesToDeleteCandidates.map(file => ("filesToDeleteCandidates", file.length, file.getAbsolutePath)).mkString(N)+N)
  //          val filesToKeepCandidates = files.filter(file => !filesToDeleteCandidates.contains(file))
  //          //          logs.append(filesToKeepCandidates.map(file => ("filesToKeepCandidates", file.length, file.getAbsolutePath)).mkString(N)+N)
  //
  //          val filesToKeep = if (filesToKeepCandidates.isEmpty) List(filesToDeleteCandidates.head) else filesToKeepCandidates
  //          remainingFilesCt += filesToKeep.size
  //          val filesToDelete = if (filesToKeepCandidates.isEmpty) filesToDeleteCandidates.tail else filesToDeleteCandidates
  //
  //
  //          logs.appendLater(filesToKeep.map(file => ("Keep", file.length.toSize, file.getAbsolutePath)).mkString(N) + N)
  //          logs.appendLater(filesToDelete.map(file => ("Delete", file.length.toSize, file.getAbsolutePath)).mkString(N) + N)
  //          deleteCt += filesToDelete.size
  //
  //          if (withDelete) filesToDelete.foreach(_.delete)
  //        }
  //      }
  //    }
  //
  //    logs.appendLater(N + N + "dirsContainingDuplicates: " + N + dirsContainingDuplicates.toList.sorted.mkString(N))
  //    logs.appendLater(N + N + "DeleteCt: " + deleteCt)
  //    logs.appendLater(N + N + "Expected: " + (filesSingle.size + remainingFilesCt))
  //
  //
  //  })

  implicit class IntPlus(i: Int) {
    def format = String.format("%,d", i: Integer)
  }

  implicit class LongPlus(l: Long) {
    //noinspection ScalaMalformedFormatString
    def format = String.format("%,d", l.asInstanceOf[Object])
  }

}