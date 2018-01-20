package eu.eyan.pvtools

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
import scala.io.Source
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.ZoneId
import eu.eyan.util.java.time.InstantPlus.InstantImplicit
import java.io.File
import java.util.concurrent.ScheduledThreadPoolExecutor
import eu.eyan.util.awt.AwtHelper
import java.util.concurrent.TimeUnit
import scala.util.Try
import java.util.concurrent.Future
import scala.collection.JavaConverters
import eu.eyan.util.scala.TryCatchFinallyClose
import scala.io.BufferedSource
import eu.eyan.util.swing.JProgressBarPlus
import javax.swing.JTextField

/**
 * TODO: konfig másodperc -> change, dont react immediately
 * TODO: auto import
 * TODO: read only -> nem sikerül videót másolni vagy képet másolni
 */
object PVTools extends App {
  Log.activateInfoLevel.redirectSystemOutAndError
  val TITLE = "Photo and video import"

  val panel = new JPanelWithFrameLayout().withBorders.withSeparators
    .newColumn.newColumnFPG

  panel.newRow.addLabel("Import path: ").cursor_HAND_CURSOR.onClicked(importPathTextField.getText.openAsFile)
  val importPathTextField = panel.nextColumn.addTextField("").rememberValueInRegistry("importPath")

  panel.newRow.addLabel("Export path: ").cursor_HAND_CURSOR.onClicked(exportPathTextField.getText.openAsFile)
  val exportPathTextField = panel.nextColumn.addTextField("").rememberValueInRegistry("exportPath")

  panel.newRow.addLabel("Files to import: ")
  val extensionsToImportTextField = panel.nextColumn.addTextField("JPG,MTS").rememberValueInRegistry("extensionsToImport")

  panel.newRow.addLabel("Files to convert: ")
  val extensionsToConvertTextField = panel.nextColumn.addTextField("MTS").rememberValueInRegistry("extensionsToConvert")

  panel.newRow.addLabel("ffmpeg.exe location: ")
  val ffmpegPathTextField = panel.nextColumn.addTextField("""C:\private\ffmpeg\bin\ffmpeg.exe""").rememberValueInRegistry("extensionsToConvert")

  panel.newRow.addLabel("exiftool.exe location: ")
  val exiftoolPathTextField = panel.nextColumn.addTextField("""C:\private\exiftool.exe""").rememberValueInRegistry("exiftool")

  panel.newRow.addLabel("Check interval (s): ")
  val checkIntervalTextField = panel.nextColumn.addTextField("3600").rememberValueInRegistry("checkInterval")

  val checkToImportButton = panel.newRow.addButton("Check to import").onAction_disableEnable(checkFilesToImport)
  val checkToImportLabel = panel.nextColumn.addLabel("")

  panel.newRow.addButton("Import").onAction_disableEnable(importFiles)
  val importLabel = panel.nextColumn.addLabel("")

  panel.newRow.addLabel("Progress:")
  val progressBar = panel.nextColumn.addProgressBar()

  panel.newRow.addLabel("More import pathes:")
  val moreImport = panel.nextColumn.addMore(new JTextField(""))

  val frame = new JFrame().title(TITLE).onCloseHide.iconFromChar('I', Color.YELLOW).addToSystemTray().withComponent(panel)
    .menuItem("File", "Exit", System.exit(0))
    .menuItem("Debug", "Open log window", LogWindow.show(panel))
    .menuItem("Debug", "Copy logs to clipboard", ClipboardPlus.copyToClipboard(Log.getAllLogs))
    .menuItem("Debug", "Clear registry values", RegistryPlus.clear(TITLE))
    .menuItem("Help", "Write email", writeEmail)
    .menuItem("Help", "About", alert("This is not an official tool, no responsibilities are taken. Use it at your own risk."))
    .packAndSetVisible

  val pool = new ScheduledThreadPoolExecutor(1)
  val future = pool.scheduleAtFixedRate(AwtHelper.runnable(checkFilesToImport), 3600, checkIntervalTextField.getText.toIntOrElse(3600), TimeUnit.SECONDS)

  def writeEmail =
    Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=Photo%20and%20video%20import&body=" + URLEncoder.encode(Log.getAllLogs, "utf-8").replace("+", "%20")))

  def convertVideo(in: File, out: File, progress: Int => Unit) = {
    val targetVideo = out.withoutExtension + ".mp4"
    Log.info("Converting " + in + " to " + targetVideo)
    val convertBat = s"""${ffmpegPathTextField.getText} -i "$in" -vf yadif -vcodec mpeg4 -b:v 17M -acodec libmp3lame -b:a 192k -y "$targetVideo""""
    val exitCode = convertBat.executeAsProcessWithResultAndOutputLineCallback(s => s.findGroup("size= *(\\d*)kB".r).foreach(kB => progress(kB.toInt * 1024)))
    exitCode == 0
  }

  def isVideoToConvert(file: File) = file.endsWith(extensionsToConvertTextField.getText.split(","): _*)

  def convertOrCopy(fileToImport: File, targetFile: File, progress: Int => Unit) =
    if (isVideoToConvert(fileToImport)) convertVideo(fileToImport, targetFile, progress)
    else fileToImport.copyTo(targetFile)

  def importFiles = {
    importLabel.text("Importing")
    val files = checkFilesToImport
    val filesToImportSizeSum = files.map(_.length).sum
    var progressBytes = 0L
    progressBar.setString("0%")
    def setProgress(bytes: Long) = progressBar.percentChanged(if (filesToImportSizeSum == 0) 0 else (bytes * 100 / filesToImportSizeSum).toInt)

    val importedFiles = for (fileToImport <- files) yield {
      val fileName = fileToImport.getName
      val fileDateTime = getDateTime(fileToImport)
      val targetFile = (exportPathTextField.getText + "\\" + fileDateTime + " " + fileName).asFile
      Log.info("Convert or copy " + fileToImport + " to " + targetFile)
      val result = if (convertOrCopy(fileToImport, targetFile, bytes => setProgress(progressBytes + bytes))) Option(fileToImport) else None
      progressBytes += fileToImport.length
      setProgress(progressBytes)
      result
    }

    val newList = alreadyImportedFiles ++ importedFiles.flatten
    newList.mkStringNL.writeToFile(alreadyImportedFile)
    importLabel.text("Import finished")
    progressBar.finished
  }

  def alreadyImportedFile = (exportPathTextField.getText + "\\alreadyImported.txt").asFile
  def alreadyImportedFiles = alreadyImportedFile.linesList.map(_.asFile)

  def checkFilesToImport: List[File] = {
    Log.info("Checking files to import")
    checkToImportButton.setEnabled(false)
    try if (!importPathTextField.getText.asDir.existsAndDir) { alert("Import dir does not exists."); List() }
    else if (!exportPathTextField.getText.asDir.existsAndDir) { alert("Export dir does not exists."); List() }
    else if (ffmpegPathTextField.getText.asFile.notExists) { alert("ffmpeg does not exists."); List() }
    else {
      val allFiles = importPathTextField.getText.asDir.subFiles.toList.filter(_.endsWith(extensionsToImportTextField.getText.split(","): _*))
      Log.trace("allFiles\r\n" + allFiles.mkStringNL)
      Log.trace("\r\nalreadyImportedFiles\r\n" + alreadyImportedFiles.mkStringNL)
      val filesToImport = allFiles.diff(alreadyImportedFiles)
      Log.trace("filesToImport " + filesToImport.mkStringNL)
      val filesToImportSizeSum = filesToImport.map(_.length).sum

      checkToImportLabel.setText(filesToImport.size + " files to import, " + (filesToImportSizeSum / 1024 / 1024) + "MB")
      Log.info("Checking files to import: " + filesToImport.size + " files.")
      if (filesToImport.size > 0) frame.state_Normal.visible.toFront

      filesToImport
    }
    finally checkToImportButton.setEnabled(true)
  }

  def alert(msg: String) = JOptionPane.showMessageDialog(null, msg)

  def getDateTime(file: File) = {
    //    video
    //    Date/Time Original              : 2017:12:28 11:53:48+02:00 DST
    //
    //    kép
    //    Date/Time Original              : 2017:12:28 11:48:14
    val exifCmd = exiftoolPathTextField.getText + " -T -DateTimeOriginal \"" + file + "\""
    val dateTime = exifCmd.executeAsProcessWithResult.output.trim.replace(":", "").replace(" ", "_").replace("\\+\\d*_DST","")
    if (dateTime.matches("\\d+_\\d+")) dateTime
    else file.lastModifiedTime.toString("yyyyMMdd_HHmmss")
  }
}