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

/**
 * To list all the files on all drives. Also search inside the compressed files.
 */
object FileLister extends App {
  
  Log.activateDebugLevel
  val TITLE = "File Lister"

  val panel = new JPanelWithFrameLayout().withBorders.withSeparators
    .newColumn.newColumnFPG

  panel.newRow.addLabel("Target file:")
  val targetTextField = panel.nextColumn.addTextField("C:\\temp\\AllDrivesFileList.txt").rememberValueInRegistry("targetPath")

  panel.newRow.addLabel("Look into compressed files:")
  val includeCompressedCheckbox = panel.nextColumn.addCheckBox("").rememberValueInRegistry("targetPath")

  panel.newRow.addLabel("Execute after every x day:")
  val daysTextField = panel.nextColumn.addTextField("7").rememberValueInRegistry("days")

  panel.newRow.addLabel("Ignore if contains:")
  val ignoreTextField = panel.nextColumn.addTextField("I:,N:,P:,T:,Y:").rememberValueInRegistry("ignore")

  panel.newRow.addLabel("Start to list:")
  panel.nextColumn.addButton("Start to list.").onAction_disableEnable(listFilesOfAllDrives)

  val counterLabel = panel.newRow.addLabel("Count")

  val frame = new JFrame().title(TITLE).onCloseHide.iconFromChar('L', Color.CYAN).addToSystemTray().withComponent(panel)
    .menuItem("File", "Exit", System.exit(0))
    .menuItem("Debug", "Open log window", LogWindow.show(panel))
    .menuItem("Debug", "Copy logs to clipboard", ClipboardPlus.copyToClipboard(Log.getAllLogs))
    .menuItem("Debug", "Clear registry values", RegistryPlus.clear(TITLE))
    .menuItem("Help", "Write email", writeEmail)
    .menuItem("Help", "About", alert("This is not an official tool, no responsibilities are taken. Use it at your own risk."))
    .packAndSetVisible

  def writeEmail = Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=FileLister&body=" + URLEncoder.encode(Log.getAllLogs, "utf-8").replace("+", "%20")))
  def alert(msg: String) = JOptionPane.showMessageDialog(null, msg)

  var ct = 0
  def listFilesOfAllDrives = {
    ct = 0
    val fsv = FileSystemView.getFileSystemView
    //    val roots = List("""C:\DEV\projects\AndrisTools\src\test\resources\list""".asFile).map(_.fileTreeWithItself)
    val roots = File.listRoots.filter(_.getAbsolutePath.containsAnyIgnoreCase(ignoreTextField.getText.split(","))).map(_.fileTreeWithItself)
    val target = targetTextField.getText.asFile
    if (target.exists) target.renameTo(target.extendFileNameWith("_old").generateNewNameIfExists())

    TryCatchFinallyClose(
      new BufferedWriter(new FileWriter(target)),
      (bw: BufferedWriter) => roots.foreach(_.foreach(file => bw.write(logFile(file) + "\r\n"))),
      t => Log.error(s"Error opening $target to write",t))
  }

  val DT = "yyyyMMdd HHmmss"
  def logFile(file: File) = {
    ct += 1
    counterLabel.text(ct + "")
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