package eu.eyan.duplicates

import java.io.File

import eu.eyan.util.awt.MultiField
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.swing.AbstractButtonPlus.AbstractButtonImplicit
import eu.eyan.util.swing.JButtonPlus.JButtonImplicit
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JTextAreaPlus.JTextAreaImplicit
import eu.eyan.util.swing.JTextFieldPlus.JTextFieldPlusImplicit
import eu.eyan.util.swing.SwingPlus.invokeLater
import eu.eyan.util.swing.{ JPanelWithFrameLayout, SwingPlus }
import javax.swing.JFrame
import rx.lang.scala.subjects.BehaviorSubject

import scala.collection.mutable
import eu.eyan.util.scala.LongPlus.LongImplicit
import javax.swing.SwingUtilities
import eu.eyan.util.time.Timer
import scala.collection.mutable.Map
import java.util.concurrent.Executors
import eu.eyan.util.java.lang.RunnablePlus
import java.util.concurrent.TimeUnit
import scala.collection.mutable.MutableList
import eu.eyan.log.Log
import scala.concurrent.duration.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object DeleteDuplicatesDirs {
  val TITLE = "Delete duplicates dirs"
}
class DeleteDuplicatesDirs {
  val panel = new JPanelWithFrameLayout().withBorders.withSeparators
  panel.newColumn.newColumnFPG
  val N = "\n"
  panel.addSeparatorWithTitle("Directories to search")
  val dirsMultiField = new MultiFieldJTextFieldWithCheckbox("dirsToSearch", 50)
  dirsMultiField.setValues(List(("", false, false)))
  dirsMultiField.rememberValueInRegistry("dirsToSearch")

  panel.newRow.add(dirsMultiField)

  val findPanel = new JPanelWithFrameLayout().withSeparators
  //val minSize = findPanel.addLabelFluent("Min bytes:").newColumn.addTextField("", 7).rememberValueInRegistry("minSize")
  val findPanelButton = findPanel.newColumnFPG.addButton("Find duplicates").onAction(findDuplicates(false))
  val fullHashCheckbox = findPanel.newColumn.addCheckBox("Full hash")
  panel.newRow.add(findPanel)

  val deletePanel = new JPanelWithFrameLayout().withSeparators

  val allowDeleteButton = deletePanel.newRow.addCheckBox("Enable").tooltipText("Enable \"Delete duplicates\" button")
  val deleteDuplicatesButton = deletePanel.newColumnFPG.addButton("Delete duplicates").onAction(findDuplicates(true))
  panel.newRow.add(deletePanel)

  val progress = panel.newRow.addProgressBar(0, 1, "%dMB")

  val logs = panel.newRowFPGForTextArea.span(1).addTextArea()

  val hashFastCache = mutable.Map[File, String]()

  val isInProgress = BehaviorSubject(false)
  val isAllowDeleteSelected = BehaviorSubject(false)

  allowDeleteButton.onActionPerformed(isAllowDeleteSelected.onNext(allowDeleteButton.isSelected))
  isAllowDeleteSelected.subscribe(allowDeleteButton.setSelected _)

  isInProgress.subscribe(pr => invokeLater(findPanelButton.setEnabled(!pr)))
  isInProgress.combineLatest(isAllowDeleteSelected).subscribe(prAll => invokeLater(deleteDuplicatesButton.setEnabled(prAll._2 && !prAll._1)))

  dirsMultiField.onChanged(() => Option(SwingUtilities.windowForComponent(panel)).map(_.asInstanceOf[JFrame]).foreach(_.resizeAndBack))

  //  val p1 = """C:\Users\NemAdmin\Desktop\Osztott"""
  //  val p2 = """C:\Users\NemAdmin\Desktop\2020 02 29 régi Win7 120GB"""
  //  Timer.timerStart
  //  val l1 = p1.asDir.fileTreeWithItself.toList.distinct
  //  println(Timer.timerElapsed, p1, l1.size)
  //  val l2 = p2.asDir.fileTreeWithItself.toList.distinct
  //  println(Timer.timerElapsed, p2, l2.size)
  //  (10s,C:\Users\NemAdmin\Desktop\Osztott,176229)
  //(8514ms,C:\Users\NemAdmin\Desktop\2020 02 29 régi Win7 120GB,130620)

  //  val start = System.currentTimeMillis
  //  new Thread(RunnablePlus.runnable {
  //    println("start1")
  //    val l1 = p1.asDir.fileTreeWithItself.toList.distinct
  //    println(Timer.timerElapsed, p1, l1.size, System.currentTimeMillis - start)
  //  }).start()
  //  new Thread(RunnablePlus.runnable {
  //	  println("start2")
  //    val l2 = p2.asDir.fileTreeWithItself.toList.distinct
  //    println(Timer.timerElapsed, p2, l2.size, System.currentTimeMillis - start)
  //  }).start()
  //  Thread.sleep(20000)
  //  println("weiter")
  //  (0ms,C:\Users\NemAdmin\Desktop\2020 02 29 régi Win7 120GB,130620,9616)
  //(1670ms,C:\Users\NemAdmin\Desktop\Osztott,176229,11284)
  //weiter

  //  val list = List(
  //    """C:\dev""",
  //    """C:\DEVELOPING_1""",
  //    """C:\Games""",
  //    """C:\Program Files""",
  //    """C:\Program Files (x86)""",
  //    """C:\Users""",
  //    """C:\Windows""",
  //    """C:\Users\NemAdmin\Desktop\Osztott""")

  //  Timer.timerStart
  //  list.foreach(path => {
  //    val l = path.asDir.fileTreeWithItself.toList.distinct
  //    println(Timer.timerElapsed, path, l.size)
  //  })
  //(2571ms,C:\dev,50501)
  //(7005ms,C:\DEVELOPING_1,128608)
  //(1057ms,C:\Games,21422)
  //(849ms,C:\Program Files,18168)
  //(2789ms,C:\Program Files (x86),64683)
  //(46s,C:\Users,777058)
  //(17s,C:\Windows,306822)
  //(10s,C:\Users\NemAdmin\Desktop\Osztott,176229)
  // 90sec

  //  val start = System.currentTimeMillis
  //  list.foreach(path => {
  //    new Thread(RunnablePlus.runnable {
  //      val l = path.asDir.fileTreeWithItself.toList.distinct
  //      println(Timer.timerElapsed, path, l.size, System.currentTimeMillis - start)
  //    }).start()
  //  })
  //(0ms,C:\Program Files,18168,1760)
  //(300ms,C:\Games,21422,2060)
  //(2253ms,C:\dev,50509,4314)
  //(490ms,C:\Program Files (x86),64683,4804)
  //(5373ms,C:\DEVELOPING_1,128608,10174)
  //(4630ms,C:\Users\NemAdmin\Desktop\Osztott,176229,14808)
  //(7047ms,C:\Windows,306822,21858)
  //(30s,C:\Users,777060,52004)
  // 52s

  private def findDuplicates(withDelete: Boolean) = SwingPlus.runInWorker({
    Timer.timerStart
    invokeLater(logs.setText(""))
    if (withDelete) dirsMultiField.clearAllowDelete
    isInProgress.onNext(true)
    isAllowDeleteSelected.onNext(false)
    val deletablePaths = dirsMultiField.getValues.filter(_._2).map(_._1)
    val dirPaths = dirsMultiField.getValues.filter(_._3).map(_._1).toStream

    //        val filesAndDirsStreams = dirPaths.flatMap(_.asDir.fileTreeWithItself)
    //        var fileCt2 = 0L
    //        progress.setFormat("%d files and dirs")
    //        progress.setMaximum(Int.MaxValue)
    //
    //        val filesAndDirsNotDistinctStream = filesAndDirsStreams.collect { case fileDir =>
    //          fileCt2 += 1
    //          if (fileCt2 % 100 == 0) progress.valueChanged(fileCt2.toInt)
    //          fileDir
    //        }
    //        log("Stream", Timer.timerElapsed);
    //        val filesAndDirsNotDistinctList = filesAndDirsNotDistinctStream.toList
    //    		log("List", Timer.timerElapsed);
    //
    //        val filesAndDirs = filesAndDirsNotDistinctList.distinct
    //    		log("Distinct", Timer.timerElapsed);
    //
    //        log("Files found: " + filesAndDirs.size, Timer.timerElapsed);
    //        progress.valueChanged(fileCt2.toInt)
    //
    //        val filesSum = filesAndDirs.filter(_.isFile()).map(_.length).sum
    //        log("Files size: " + filesSum.toSize, Timer.timerElapsed);
    //
    //        val dirsSum = filesAndDirs.filter(_.isDirectory()).map(_.length).sum
    //    		log("Dirs size: " + dirsSum.toSize, Timer.timerElapsed);

    //// ÚJ

    //// Egész jó chunkolós
    //    val chunks = MutableList[List[File]]()
    //
    //    def exploreDir(dir: File): List[File] = {
    //      //dirsAndFiles.add(dir)
    //      fileCt += 1
    //      val filesDirs = dir.listFiles
    //      if (filesDirs == null) {
    //        errorCt += 1
    //        List(dir)
    //      } else {
    //        //if (filesDirs.size > 1000) println(filesDirs.size)
    //        val list =  filesDirs.map(fileOrDir => {
    //          if (fileOrDir.isFile) {
    //            //dirsAndFiles.add(fileOrDir)
    //            val x = fileOrDir.length
    //            fileCt += 1
    //            List(fileOrDir)
    //          } else {
    //            if (activeJobs < maxJobs) {
    //              exploreDirAsync(fileOrDir)
    //              List()
    //            } else exploreDir(fileOrDir)
    //          }
    //        })
    //        progress.valueChanged(fileCt.toInt)
    //        List(dir) ++ (list.toList).flatten
    //      }
    //    }
    //
    //    def exploreDirAsync(dir: File): Unit = {
    //      lock.synchronized { activeJobs += 1 }
    //      executor.execute(RunnablePlus.runnable({
    //        try {
    //          val chunk = exploreDir(dir)
    //          lock.synchronized { chunks += chunk }
    //        } finally lock.synchronized { activeJobs -= 1 }
    //      }))
    //    }
    //// Egész jó chunkolós vége

    //    class DirsAndFiles() {
    //      val dirs = Map[File, Dir]()
    //      val fils = Map[File, Fil]()
    //      def add(file: File) = this.synchronized {
    //        if (file.existsAndDir && !dirs.contains(file)) dirs.put(file, Dir(file))
    //        else if (file.existsAndFile && !fils.contains(file)) fils.put(file, Fil(file))
    //      }
    //    }

    //    val dirsAndFiles = new DirsAndFiles()

    //    var sum = 0L

    progress.setFormat("%,d files and dirs")
    progress.setMaximum(Int.MaxValue)

    type ParentDir = Option[Dir]

    implicit class DirFilImplicit(file: File) {
      def toDirFil(parent: ParentDir): DirFil = if (file.isFile) Fil(file, parent) else Dir(file, parent)
    }
    //    implicit class ArrayPlusImplicit[T](array: Array[T]) {
    //      def appendIf(condition: => Boolean, toAppend: => Array[T]) = {
    //        val ar1 = array
    //        val ar2 = toAppend
    //        if(condition) (ar1.++(ar2)) else ar1
    //      }
    //      //    	  if(condition) (array ++ toAppend) else array
    //    }

    class DirFil(parent: ParentDir) {
      def isFile = this.isInstanceOf[Fil]
      def isFileOption = if (isFile) Option(this) else None
      def isDirectory = this.isInstanceOf[Dir]
      def isDirectoryOption = if (isDirectory) Option(this) else None
      def hasParent = parent.nonEmpty
      def asDir = this.asInstanceOf[Dir]
      def asFil = this.asInstanceOf[Fil]
      def listFiles = {
        val files = asDir.dir.listFiles;
        if (files == null) {
          Log.error("null received from listFiles of " + asDir.dir); Array[File]()
        } else files
      }
      def getParent = parent

      parent.foreach(_.addChild(this))
    }
    case class Dir(dir: File, parent: ParentDir) extends DirFil(parent) {
      val children = MutableList[DirFil]()
      def addChild(dirFil: DirFil) = children.synchronized { children += dirFil }
    }

    case class Fil(fil: File, parent: ParentDir) extends DirFil(parent) {
      val size = fil.length
    }

    //    def explore(dirsOrFiles: Array[(File, ParentDir)]): Array[DirFil] = {
    //    		val ret = dirsOrFiles.map(dirOrFile => {
    //    			lock.synchronized { fileCt += 1 }
    //    			if (dirOrFile._1.isFile) {
    //    				Array[DirFil](Fil(dirOrFile._1, dirOrFile._2))
    //    			} else {
    //    				val dirsParent = dirOrFile._2
    //    						val dir = Dir(dirOrFile._1, dirsParent)
    //    						val slided = dir.dir.listFiles.map(f => (f, Option(dir))).grouped(slideSize).toArray
    //    						val slides = slided.map(slide => {
    //    							if (activeJobs < maxJobs) {
    //    								exploreAsync(slide)
    //    								Array[DirFil]()
    //    							} else {
    //    								explore(slide)
    //    							}
    //    						}).flatten
    //
    //    						Array[DirFil](dir) ++ slides
    //    			}
    //    		})
    //    				progress.valueChanged(fileCt.toInt)
    //    				ret.flatten
    //    }
    //
    //    def exploreAsync(dirsOrFiles: Array[(File, ParentDir)]): Unit = {
    //    		lock.synchronized { activeJobs += 1 }
    //    		executor.execute(RunnablePlus.runnable({
    //    			try {
    //    				val chunk = explore(dirsOrFiles)
    //    						lock.synchronized { chunks += chunk }
    //    			} finally lock.synchronized { activeJobs -= 1 }
    //    		}))
    //    }

    val lock = new Object()
    type Chunk = Array[DirFil]
    val fileCt$ = BehaviorSubject(0)
    val fileCt = new AtomicInteger
    fileCt$.sample(Duration(30, TimeUnit.MILLISECONDS)).subscribe(progress.valueChanged(_))

    val chunkSize = 100
    def explore(dirsOrFiles: Chunk, syncOrAsync: Chunk => Option[Chunk]): Chunk = {
      dirsOrFiles
        .map(dirOrFile => {
          dirOrFile +:
            dirOrFile
            .isDirectoryOption
            .map(_
              .listFiles
              .map(_.toDirFil(Option(dirOrFile.asDir)))
              .grouped(chunkSize)
              .toArray
              .map(syncOrAsync)
              .map(_.getOrElse(Array[DirFil]()))
              .flatten)
            .getOrElse(Array())
        })
        .map(df => { fileCt$.onNext(fileCt.incrementAndGet); df })
        .flatten
    }

    log(dirPaths.mkString)
    val chunks = new ParallelExecutor(explore).executeAndWaitUntilDone(dirPaths.map(_.asDir.toDirFil(None)).toArray)

    log("Explore done", Timer.timerElapsed)
    //    log("Chunks: ", chunks.size)

    val dirFils = chunks.flatten.toList
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

    val filesBySizeGroups = files.groupBy(_.size)
    log("File size groups ", filesBySizeGroups.size.format, Timer.timerElapsed)

    val filesSingle = filesBySizeGroups.filter(_._2.size == 1).values.flatten.toList
    log("Single files", filesSingle.size.format, "size", filesSingle.map(_.size).sum.toSize, Timer.timerElapsed)

    val filesMultiGroups = filesBySizeGroups.filter(_._2.size != 1).values.toList.sortWith((l1, l2) => l1.head.size < l2.head.size)
    val multiFilesCt = filesMultiGroups.flatten.size
    val multiFilesSum = filesMultiGroups.flatten.map(_.size).sum
    log("Multi files", filesMultiGroups.flatten.size.format, "size", multiFilesSum.toSize, Timer.timerElapsed)

    val fullHash = fullHashCheckbox.isSelected
    progress.setMaximum(if (fullHash) (multiFilesSum / 1000000L).toInt else multiFilesCt)
    progress.setFormat("%,d")
    progress.valueChanged(0)

    val hashReadCount = new AtomicInteger
    val hashReadCount$ = BehaviorSubject(0)
    if (!fullHash) hashReadCount$.sample(Duration(30, TimeUnit.MILLISECONDS)).subscribe(progress.valueChanged(_))

    val hashReadProgress = new AtomicLong
    val hashReadProgress$ = BehaviorSubject(0L)
    def updateProgress(readBytes: Long) = hashReadProgress$.onNext(hashReadProgress.addAndGet(readBytes))
    if (fullHash) hashReadProgress$.sample(Duration(30, TimeUnit.MILLISECONDS)).subscribe(hashReadProgress => {
      progress.setString(hashReadProgress.toSize)
      progress.setValue((hashReadProgress / 1000000).toInt)
    })

    filesMultiGroups.foreach { group =>
      val hashGroups = group.groupBy(f =>
        try if (fullHash) f.fil.hashFull(updateProgress)
        else if (hashFastCache.contains(f.fil)) hashFastCache({ updateProgress(f.size); f.fil })
        else hashFastCache.getOrElseUpdate(f.fil, f.fil.hashFast(updateProgress))
        finally hashReadCount$.onNext(hashReadCount.incrementAndGet))
    }
    log("Hashing done", Timer.timerElapsed)

  }, isInProgress.onNext(false))

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
  //          logs.appendLater(N + N)
  //          val hash = hashFiles._1
  //          logs.appendLater(hash + N)
  //
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

  private def log(msg: Any*) = logs.appendLater(msg.mkString(" ") + N)
  implicit class IntPlus(i: Int) {
    def format = String.format("%,d", i: Integer)
  }
  implicit class LongPlus(l: Long) {
    def format = String.format("%,d", l.asInstanceOf[Object])
  }

}