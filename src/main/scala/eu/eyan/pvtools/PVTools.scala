package eu.eyan.pvtools

import eu.eyan.log.Log
import eu.eyan.log.LogWindow
import eu.eyan.util.awt.clipboard.ClipboardPlus
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import eu.eyan.util.registry.RegistryPlus
import eu.eyan.util.scala.collection.TraversableOncePlus.TraversableOnceImplicit
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.swing.JButtonPlus.JButtonImplicit
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JLabelPlus.JLabelImplicit
import eu.eyan.util.swing.JPanelWithFrameLayout
import eu.eyan.util.swing.JTextFieldPlus.JTextFieldPlusImplicit
import javax.swing.JFrame
import javax.swing.JOptionPane
import java.awt.Color
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder

import eu.eyan.util.java.time.InstantPlus.InstantImplicit
import java.io.File

import javax.swing.JScrollPane

import scala.io.Codec

/**
 * TODO: konfig másodperc -> change, dont react immediately
 * TODO: auto import
 * TODO: read only -> nem sikerül videót másolni vagy képet másolni
 * TODO: fájlonként mentse az exportot
 */
object PVTools extends App {
  Log.activateInfoLevel
  LogWindow.redirectSystemOutAndErrToLogWindow

/***************************  UI  ***************************************************/

  val TITLE = "Photo and video import"

  val panel = new JPanelWithFrameLayout().withBorders.withSeparators
    .newColumn.newColumnFPG

  panel.newRow("f:p").addLabel("Import path: ").cursor_HAND_CURSOR
  val importPathTextField = panel.nextColumn.addTextFieldMulti("importPathTextField", 30, List()).rememberValueInRegistry("importPathTextFields")
  importPathTextField.onChanged(() => frame.resizeAndBack)

  panel.newRow.addLabel("Export path: ").cursor_HAND_CURSOR.onClicked(exportPathTextField.getText.openAsFile)
  val exportPathTextField = panel.nextColumn.addTextField("").rememberValueInRegistry("exportPath")

  panel.newRow.addLabel("Local temp path: ").cursor_HAND_CURSOR.onClicked(tempPathTextField.getText.openAsFile)
  val tempPathTextField = panel.nextColumn.addTextField("").rememberValueInRegistry("tempPath")

  panel.newRow.addLabel("Files to import: ")
  val extensionsToImportTextField = panel.nextColumn.addTextField("JPG,MTS,m2ts,mp4,jpeg").rememberValueInRegistry("extensionsToImport")

  panel.newRow.addLabel("Files to convert: ")
  val extensionsToConvertTextField = panel.nextColumn.addTextField("MTS,m2ts").rememberValueInRegistry("extensionsToConvert")

  panel.newRow.addLabel("ffmpeg.exe location: ")
  val ffmpegPathTextField = panel.nextColumn.addTextField("""C:\private\ffmpeg\bin\ffmpeg.exe""").rememberValueInRegistry("ffmpeg")

  panel.newRow.addLabel("exiftool.exe location: ")
  val exiftoolPathTextField = panel.nextColumn.addTextField("""C:\private\exiftool.exe""").rememberValueInRegistry("exiftool")

  panel.newRow.addLabel("Check interval (s): ")
  val checkIntervalTextField = panel.nextColumn.addTextField("3600").rememberValueInRegistry("checkInterval")

  val checkToImportButton = panel.newRow.addButton("Check to import").onAction_disableEnable(checkFilesToImport)
  val checkToImportLabel = panel.nextColumn.addLabel("")

  val doImport = panel.newRow.addCheckBox("Do Import")

  panel.newRow.addButton("Import").onAction_disableEnable(importFiles)
  val importLabel = panel.nextColumn.addLabel("")

  panel.newRow.addLabel("Progress:")
  val progressBar = panel.nextColumn.addProgressBar()

  val logArea = panel.newRow("f:100px:g").span.addTextArea()

  val frame =
    new JFrame()
      .title(TITLE)
      .onCloseHide
      .iconFromChar('I', Color.ORANGE)
      .addToSystemTray()
      .withComponent(new JScrollPane(panel))
      .menuItem("File", "Exit", System.exit(0))
      .menuItem("Debug", "Open log window", LogWindow.show(panel))
      .menuItem("Debug", "Copy logs to clipboard", ClipboardPlus.copyToClipboard(LogWindow.getAllLogs))
      .menuItem("Debug", "Clear registry values", RegistryPlus.clear(TITLE))
      .menuItem("Help", "Write email", writeEmail())
      .menuItem("Help", "About", alert("This is not an official tool, no responsibilities are taken. Use it at your own risk."))
      .packAndSetVisible
      .center

  def importFiles = {
    importLabel.text("Importing")

    val files = checkFilesToImport

    // calculate size
    val filesToImportSizeSum = files.map(_.length).sum
    var progressBytes = 0L
    progressBar valueChanged 0


    def setProgress(bytes: Long) = progressBar.valueChanged(if (filesToImportSizeSum == 0) 0 else (bytes * 100 / filesToImportSizeSum).toInt)

    def fileProgress(bytes: Long) = {
      progressBytes += bytes
      setProgress(progressBytes)
      Log.trace("progressBytes " + progressBytes)
      Log.trace("progressBytes " + (bytes * 100 / filesToImportSizeSum))
    }

    val importedFiles = files.map(importFile(fileProgress))

    log("Imported files:\n"+importedFiles.mkString("\n")+".")


    importLabel.text("Import finished")
    progressBar.finished
  }

  def importFile(progressCallback: Long => Unit)(fileToImport:File) = {

    try {
      Log.info("---------------------------------------------")
      //val tempFile = fileToImport.copyToDir(tempPathTextField.getText.asDir)

      var bytesSum = 0L

      val importResult =
      //if (tempFile.nonEmpty)
      {
        //val fileName = tempFile.get.getName
        val fileName = fileToImport.getName

        val dateInFileName = "\\d{8}[_-]?\\d{6}"
        val fileNameDTs = fileToImport.getName.findAll(dateInFileName)
        val lastModifiedTime = fileToImport.lastModifiedTime.toString("yyyyMMdd_HHmmss")
        val exifDT = exifDateTime(fileToImport)

        val fileNameWithoutDate = fileName.replaceAll(dateInFileName,"").trim

        def extendWithSpaceIfNeeded(s:String) = {
          val ret = if(s.contains(" ")) s else s.patch(8," ",0)
          Log.info("extendWithSpaceIfNeeded "+ s+" "+s.contains(" ") + " ->"+ret)
          ret
        }


        val targetDate =
          if(fileNameDTs.nonEmpty) /*FIXME*/"same "+fileNameDTs.last
          else if(exifDT.nonEmpty) /*FIXME*/"exif " + exifDT.get
          else /*FIXME*/"last " + lastModifiedTime


        val targetFile = (exportPathTextField.getText + "\\"+extendWithSpaceIfNeeded(targetDate.replaceAll("[_-]"," ").trim)+" "+fileNameWithoutDate).asFile

        log(fileToImport.getName+"\t"+fileNameDTs+"\t"+lastModifiedTime+"\t"+exifDT+"\t"+targetFile.getName)

        val importResult =
          if(doImport.isSelected) {
            Log.info("Convert or copy " + fileToImport + " to " + targetFile)
            // Import File
            val importResult =
              if (isVideoToConvert(fileToImport)) convertVideo(fileToImport, targetFile, bytes => {bytesSum+=bytes; progressCallback(bytes)})
              else fileToImport.copyTo(targetFile)
            Log.info("Import result " + fileToImport + " = " + importResult)
            importResult
          }
          else false

        //val deleteTempSuccess = tempFile.get.delete
        //Log.info(s"Delete temp: $tempFile $deleteTempSuccess")

        importResult
      }
      //          else {
      //            Log.error("Cannot copy " + fileToImport + " to " + tempPathTextField.getText.asDir)
      //            false
      //          }

      progressCallback(fileToImport.length-bytesSum)

      if (importResult) {
        val newList = alreadyImportedFiles ++ List(fileToImport)
        Option(newList.mkStringNL.writeToFile(alreadyImportedFile))
      } else {
        None
      }
    } catch {
      case e: Throwable => Log.error(e)
        None
    }
  }

  def checkFilesToImport: List[File] = {
    logArea.setText("")
    Log.info("Checking files to import")
    checkToImportButton.setEnabled(false)
    try
      if (importPathTextField.getValues.size < 1) { alert("At least on import dir please."); List() }
      else if (!importPathTextField.getValues.map(_.asDir.existsAndDir).forall(d2 => d2)) { alert("One of the import dirs does not exists."); List() }
      else if (!exportPathTextField.getText.asDir.existsAndDir) { alert("Export dir does not exists."); List() }
      else if (!tempPathTextField.getText.asDir.existsAndDir) { alert("Temp dir does not exists."); List() }
      else if (ffmpegPathTextField.getText.asFile.notExists) { alert("ffmpeg does not exists."); List() }
      else {

        val dirs = importPathTextField.getValues
        val allFiles = dirs.flatMap(_.asDir.subFiles.toList)
        log("All files: "+allFiles.size)
        val allExtensions = allFiles.map(_.extension).distinct.map(_.toLowerCase.trim).map(_.substring(1)).toSet
        log("All extensions: "+allExtensions.mkString(", "))
        val extensionsToImport = extensionsToImportTextField.getText.split(",").map(_.toLowerCase.trim).toSet
        log("ExtensionsToImport: "+extensionsToImport.mkString(", "))
        log("NOT to import: "+allExtensions.diff(extensionsToImport).mkString(", "))
        val allFilesToImport = allFiles.filter(_.endsWith(extensionsToImport.toList: _*))
        log("Files to Import: "+allFilesToImport.size)

        Log.trace("allFiles\r\n" + allFilesToImport.mkStringNL)
        Log.trace("\r\nalreadyImportedFiles\r\n" + alreadyImportedFiles.mkStringNL)
        val filesToImport = allFilesToImport.diff(alreadyImportedFiles)
        Log.trace("filesToImport " + filesToImport.mkStringNL)
        val filesToImportSizeSum = filesToImport.map(_.length).sum

        checkToImportLabel.setText(filesToImport.size + " files to import, " + (filesToImportSizeSum / 1024 / 1024) + "MB")
        Log.info("Checking files to import: " + filesToImport.size + " files.")
        if (filesToImport.nonEmpty) frame.state_Normal.visible.toFront()

        filesToImport
      }
    finally checkToImportButton.setEnabled(true)
  }

  def log(txt: String) = logArea.append(txt+"\n")

  def writeEmail() =
    Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=Photo%20and%20video%20import&body=" + URLEncoder.encode(LogWindow.getAllLogs, "utf-8").replace("+", "%20")))

  def convertVideo(in: File, out: File, progress: Int => Unit) = {
    val targetVideo = out.withoutExtension + ".mp4"
    Log.info("Converting " + in + " to " + targetVideo)
    val convertBat = s"""${ffmpegPathTextField.getText} -i "$in" -vf yadif -vcodec mpeg4 -b:v 17M -acodec libmp3lame -b:a 192k -y "$targetVideo""""
    val exitCode = convertBat.executeAsProcessWithResultAndOutputLineCallback(s => s.findGroup("size= *(\\d*)kB".r).foreach(kB => progress(kB.toInt * 1024)))
    Log.info("Converting exitCode=" + exitCode)
    exitCode == 0
  }

  def isVideoToConvert(file: File) = file.endsWith(extensionsToConvertTextField.getText.split(","): _*)

  def alreadyImportedFile = (exportPathTextField.getText + "\\alreadyImported.txt").asFile
  // TODO implement File.lift...
  def alreadyImportedFiles = if (alreadyImportedFile.exists) alreadyImportedFile.linesList.map(_.asFile) else List()

  def alert(msg: String) = JOptionPane.showMessageDialog(null, msg)

  def exifDateTime(file: File) = {
    Log.activateAllLevel
    val exifCmd = exiftoolPathTextField.getText + " -T -DateTimeOriginal \"" + file + "\""

    val res = exifCmd.executeAsProcessWithResult(Codec.ISO8859)
    val exifDateTimeOption = res.output
    Log.debug("Result: "+exifDateTimeOption)
    if (exifDateTimeOption.nonEmpty) {
      val exifDateTime = exifDateTimeOption.get
      val clearedExifDateTime = exifDateTime.trim.replace(":", "").replace(" ", "_").replaceAll("\\+\\d*.DST", "")

      Log.debug("clearedExifDateTime "+clearedExifDateTime)
      val dateTime =
        if (clearedExifDateTime.matches("\\d+_\\d+")) Option(clearedExifDateTime)
        else None
      Log.debug(s"find out date time of $file exifDateTime=$exifDateTime clearedExifDateTime=$clearedExifDateTime dateTime=$dateTime")
      dateTime
    } else {
      Log.error("Error,        " + file+" "+exifCmd)
      Log.error("Error, exit   " + file+" "+res.exitValue)
      Log.error("Error, output " + file+" "+res.output)
      Log.error("Error, error  " + file+" "+res.errorOutput)
      None
    }
  }
}