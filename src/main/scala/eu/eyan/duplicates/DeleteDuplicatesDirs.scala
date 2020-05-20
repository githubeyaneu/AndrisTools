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

object DeleteDuplicatesDirs {
  val TITLE = "Delete duplicates dirs"
}
class DeleteDuplicatesDirs {
  val panel = new JPanelWithFrameLayout().withBorders.withSeparators
  panel.newColumn.newColumnFPG
  val N = "\n"
  panel.addSeparatorWithTitle("Directories to search")
  val dirs = new MultiFieldJTextFieldWithCheckbox("dirsToSearch", 50)
  dirs.setValues(List(("", false, false)))
  dirs.rememberValueInRegistry("dirsToSearch")

  panel.newRow.add(dirs)

  val findPanel = new JPanelWithFrameLayout().withSeparators
  //val minSize = findPanel.addLabelFluent("Min bytes:").newColumn.addTextField("", 7).rememberValueInRegistry("minSize")
  val findPanelButton = findPanel.newColumnFPG.addButton("Find duplicates").onAction(findDuplicates(false))
  val fullHash = findPanel.newColumn.addCheckBox("Full hash")
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

  dirs.onChanged(() => Option(SwingUtilities.windowForComponent(panel)).map(_.asInstanceOf[JFrame]).foreach(_.resizeAndBack))

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
    if (withDelete) dirs.clearAllowDelete
    isInProgress.onNext(true)
    isAllowDeleteSelected.onNext(false)
    val deletablePaths = dirs.getValues.filter(_._2).map(_._1)
    val dirPaths = dirs.getValues.filter(_._3).map(_._1).toStream

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
    class DirFil()
    case class Dir(dir: File) extends DirFil {}
    case class Fil(fil: File) extends DirFil {
      val size = fil.length
    }
    class DirsAndFiles() {
      val dirs = Map[File, Dir]()
      val fils = Map[File, Fil]()
      def add(file: File) = this.synchronized {
        if (file.existsAndDir && !dirs.contains(file)) dirs.put(file, Dir(file))
        else if (file.existsAndFile && !fils.contains(file)) fils.put(file, Fil(file))
      }
    }

    val dirsAndFiles = new DirsAndFiles()

    var fileCt = 0L
    var errorCt = 0L
    var activeJobs = 0
    var sum = 0L
    val lock = new Object()
    progress.setFormat("%d files and dirs")
    progress.setMaximum(Int.MaxValue)

    val maxJobs = Runtime.getRuntime.availableProcessors * 2
    //with    dirsAndFiles.add: 1-92s, 2-69s, 3-66s, 4-67s
    //without dirsAndFiles.add: 1-42s, 2-27s, 3-19s, 4-18s, 8-13s, 12-13s, 24-11s, 48-11s
    val executor = Executors.newFixedThreadPool(maxJobs)

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

    val chunks = MutableList[Array[File]]()
    val slideSize = 100

    def explore(dirsOrFiles: Array[File]): Array[File] = {
      val ret = dirsOrFiles.map(dirOrFile => {
        lock.synchronized { fileCt += 1 }
        if (dirOrFile.isFile) {
          Array(dirOrFile)
        } else {
          val slided = dirOrFile.listFiles.grouped(slideSize).toArray
          val slides = slided.map(slide => {
            if (activeJobs < maxJobs) {
              exploreAsync(slide)
              Array[File]()
            } else {
              explore(slide)
            }
          }).flatten

          Array(dirOrFile) ++ slides
        }
      })
      progress.valueChanged(fileCt.toInt)
      ret.flatten
    }

    def exploreAsync(dirsOrFiles: Array[File]): Unit = {
      lock.synchronized { activeJobs += 1 }
      executor.execute(RunnablePlus.runnable({
        try {
          val chunk = explore(dirsOrFiles)
          lock.synchronized { chunks += chunk }
        } finally lock.synchronized { activeJobs -= 1 }
      }))
    }

    exploreAsync(dirPaths.map(_.asDir).toArray)
    while (activeJobs > 0) {
      Thread.sleep(1000)
      log(activeJobs + "", Thread.activeCount() + "")
    }
    progress.valueChanged(fileCt.toInt)
    executor.shutdown
    executor.awaitTermination(1, TimeUnit.HOURS)
    log("Explore done", Timer.timerElapsed)
    log("Errors: ", errorCt)
    log("Chunks: ", chunks.size)

    val files = chunks.flatten
    log("Flatten", Timer.timerElapsed)
    val filesL = files.toList
    log("Flattened toList", Timer.timerElapsed)
    log("Size (678009 ?)", filesL.size)
    log("Files (596734 ?)", filesL.filter(_.isFile).size)
    log("Dirs (81275 ?)", filesL.filter(_.isDirectory).size)
    log("fileCt: ", fileCt)
    log("Windows - Desktop", 596734, 81275, 678009)

    //    log("Dirs found: " + dirsAndFiles.dirs.size, Timer.timerElapsed);
    //    log("Files found: " + dirsAndFiles.fils.size, Timer.timerElapsed);
    //
    //    log("Files size: " + dirsAndFiles.fils.values.map(_.size).sum.toSize, Timer.timerElapsed);
    ////ÚJ vége

    //    val dirsSum = filesAndDirs.filter(_.isDirectory()).map(_.length).sum
    //		log("Dirs size: " + dirsSum.toSize, Timer.timerElapsed);

    /*
 * Orig
    Stream 11ms
    List 39s
    Distinct 2s
    Files found: 678010 15ms
    Files size: 236GB 1min
    Dirs size: 293MB 35s
*/
  }, isInProgress.onNext(false))

  private def log(msg: Any*) = logs.appendLater(msg.mkString(" ") + N)

  //  private def findDuplicates(withDelete: Boolean) = SwingPlus.runInWorker ({
  //
  //    val fileGroupsByLength = files.groupBy(_.length())
  //
  //    val filesSingle = fileGroupsByLength.filter(_._2.size == 1).values.flatten.toList
  //
  //    val filesMultiGroups = fileGroupsByLength.filter(_._2.size != 1).values.toList.sortWith((l1, l2) => l1.head.length < l2.head.length)
  //
  //    val filesSingleSum = filesSingle.map(_.length).sum
  //    val filesMultiGroupsSum = filesMultiGroups.flatten.map(_.length).sum
  //
  //    logs.appendLater("Files found: " + files.size + ", Size: " + filesSum.toSize + N)
  //    logs.appendLater("Single: " + filesSingle.size + ", Size single: " + filesSingleSum.toSize +  N)
  //    logs.appendLater("Multi : " + filesMultiGroups.flatten.size + " (" + filesMultiGroups.size + " groups), Size multi:" + filesMultiGroupsSum.toSize +  N)
  //
  //    progress.setFormat("")
  //    progress.valueChanged(0)
  //
  //    var sum = 0L
  //
  //    def updateProgress(readBytes: Long) = {
  //      sum += readBytes
  //      progress.setString(sum.toSize)
  //    }
  //
  //    var remainingFilesCt = 0
  //    var deleteCt = 0
  //
  //    val dirsContainingDuplicates = mutable.Set[File]()
  //
  //    filesMultiGroups.foreach { group =>
  //      val hashGroups = group.groupBy(f =>
  //        if (fullHash.isSelected) f.hashFull(updateProgress)
  //        else if (hashFastCache.contains(f)) hashFastCache({updateProgress(f.length); f})
  //        else hashFastCache.getOrElseUpdate(f, f.hashFast(updateProgress))
  //      )
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
}