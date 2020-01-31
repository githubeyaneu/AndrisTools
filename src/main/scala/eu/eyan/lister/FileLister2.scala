package eu.eyan.lister

import eu.eyan.util.swing.JPanelWithFrameLayout
import eu.eyan.log.Log
import eu.eyan.log.LogWindow
import eu.eyan.util.awt.clipboard.ClipboardPlus
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import eu.eyan.util.java.net.URLPlus.URLImplicit
import eu.eyan.util.registry.RegistryPlus
import eu.eyan.util.scala.collection.TraversableOncePlus.TraversableOnceImplicit
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.swing.JButtonPlus.JButtonImplicit
import eu.eyan.util.swing.JCheckBoxPlus.JCheckBoxImplicit
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JLabelPlus.JLabelImplicit
import eu.eyan.util.swing.JPanelWithFrameLayout
import eu.eyan.util.swing.JTextAreaPlus.JTextAreaImplicit
import eu.eyan.util.swing.JTextFieldPlus.JTextFieldPlusImplicit
import eu.eyan.util.swing.SwingPlus
import javax.swing.JFrame
import javax.swing.JOptionPane
import java.awt.Color
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import javax.swing.filechooser.FileSystemView
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import eu.eyan.util.scala.TryCatchFinally
import java.time.Instant
import eu.eyan.util.java.time.InstantPlus.InstantImplicit
import eu.eyan.util.compress.CompressPlus
import eu.eyan.util.compress.ZipPlus
import eu.eyan.util.compress.SevenZipPlus
import eu.eyan.util.scala.TryCatchFinallyClose
import eu.eyan.util.system.SystemPlus.KeepAlive
import eu.eyan.util.system.SystemPlus
import rx.lang.scala.subjects.BehaviorSubject
import eu.eyan.util.rx.lang.scala.ObservablePlus.ObservableImplicitBoolean
import javax.swing.SwingUtilities
import rx.lang.scala.Observable
import eu.eyan.util.rx.lang.scala.ObservablePlus

/**
 * To list all the files on all drives. Also search inside the compressed files.
 */
object FileLister2 extends App {

  Log.activateDebugLevel
  // TODO reimplement  LogWindow.redirectOutAndErrToFiles("""C:\temp""")
  LogWindow.redirectSystemOutAndErrToLogWindow
  "1".println
  "2".printlnErr

  val TITLE = "File Lister"

  val panel = new JPanelWithFrameLayout().withBorders.withSeparators.newColumn.newColumnFPG

  val targetTextFile = BehaviorSubject[File]
  val daysText = BehaviorSubject[String]
  val ignoreText = BehaviorSubject[String]
  val startButton = BehaviorSubject[String]
  val isWorkInProgress = BehaviorSubject(false)
  val counter = BehaviorSubject[Int]
      
  panel
    .newRow.addLabelFluent("Target file:")
    .nextColumn.addTextFieldFluent("C:\\temp\\AllDrivesFileList.txt", 15, text => targetTextFile.onNext(text.asFile), "targetPath")

    .newRow.addLabelFluent("Look into compressed files:")
    .nextColumn.addCheckBoxFluent("",false, selected => {}, "lookIntoCompressedFiles")

    .newRow.addLabelFluent("Execute after every x day:")
    .nextColumn.addTextFieldFluent("7", 15, daysText.onNext, "days")

    .newRow.addLabelFluent("Ignore if contains:")
    .nextColumn.addTextFieldFluent("I:,N:,P:,T:,Y:", 15, ignoreText.onNext, "ignore")

    .newRow.addLabelFluent("Start to list:")
    .nextColumn.addButtonFluent("Start to list.", startButton.onNext, isWorkInProgress.negate)

    .newRow.addLabelFluent("Count", counter.map(_.toString))

  
  val frame = new JFrame().title(TITLE).onCloseHide.iconFromChar('L', Color.CYAN).addToSystemTray().withComponent(panel)
    .menuItem("File", "Exit", System.exit(0))
    .menuItem("Debug", "Open log window", LogWindow.show(panel))
    .menuItem("Debug", "Copy logs to clipboard", ClipboardPlus.copyToClipboard(LogWindow.getAllLogs))
    .menuItem("Debug", "Clear registry values", RegistryPlus.clear(TITLE))
    .menuItem("Help", "Write email", writeEmail)
    .menuItem("Help", "About", alert("This is not an official tool, no responsibilities are taken. Use it at your own risk."))
    .packAndSetVisible

  ObservablePlus.toList(startButton, targetTextFile).map(_(1).asInstanceOf[File]).subscribe(startToList(_))
  def writeEmail = Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=FileLister&body=" + URLEncoder.encode(LogWindow.getAllLogs, "utf-8").replace("+", "%20")))
  def alert(msg: String) = JOptionPane.showMessageDialog(null, msg)

  var ct = 0
  
  
  def startToList(target: File) = {
    println("startToList "+target)
    SwingPlus.runInWorker({isWorkInProgress.onNext(true); listFilesOfAllDrives(target)}, isWorkInProgress.onNext(false))
  }
  
  def listFilesOfAllDrives(target: File) =  SystemPlus.keepAlive {
    ct = 0
    val fsv = FileSystemView.getFileSystemView
    //    val roots = List("""C:\DEV\projects\AndrisTools\src\test\resources\list""".asFile).map(_.fileTreeWithItself)
    val roots = File.listRoots /*.filter(_.getAbsolutePath.containsAnyIgnoreCase(ignoreTextField.getText.split(",")))*/ .map(_.fileTreeWithItself)
    if (target.exists) target.renameTo(target.extendFileNameWith("_old").generateNewNameIfExists())

    TryCatchFinallyClose(
      new BufferedWriter(new FileWriter(target)),
      (bw: BufferedWriter) => roots.foreach(_.foreach(file => bw.write(logFile(file) + "\r\n"))),
      t => Log.error(s"Error opening $target to write", t))

  }

  val DT = "yyyyMMdd HHmmss"
  def logFile(file: File) = {
    ct += 1
    counter.onNext(ct)
    val path = file.getAbsolutePath
    val size = file.length
    val lastModified = file.lastModified
    val date = Instant.ofEpochMilli(lastModified)

    val zipContent =
      if (file.isFile && file.endsWith("zip")) {
        ZipPlus.listFiles(file).map(zip => formatFileLog(path + "\\" + zip.getName, zip.getLastModifiedTime.toInstant, zip.getSize)).mkString("\r\n", "\r\n", "")
      } else if (file.isFile && file.endsWith("7z", "7zip")) {
        SevenZipPlus.listFiles(file).map(zip => formatFileLog(path + "\\" + zip.getName, zip.getLastModifiedDate.toInstant, zip.getSize)).mkString("\r\n", "\r\n", "")
      } else ""

    formatFileLog(path, date, size) + zipContent
  }

  def formatFileLog(path: String, instant: Instant, size: Long) = f"${instant.toString(DT)}  $size%10s  $path"

}