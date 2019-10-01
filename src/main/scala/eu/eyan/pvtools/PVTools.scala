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
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Paths}

import eu.eyan.util.swing.JToggleButtonPlus.JToggleButtonImplicit
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

  val useAlreadyImported = panel.newRow.addCheckBox("Use already imported txt").rememberValueInRegistry("useAlreadyImported")

  val checkToImportButton = panel.newRow.addButton("Check to import").onAction_disableEnable(checkFilesToImport)
  val checkToImportLabel = panel.nextColumn.addLabel("")

  val doImport = panel.newRow.addCheckBox("Do Import").rememberValueInRegistry("doImport")

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

    Log.deactivate //FIXME
    def setProgress(bytes: Long) = {
      val newVal = if (filesToImportSizeSum == 0) 0 else (bytes * 100 / filesToImportSizeSum).toInt
      progressBar.valueChanged(newVal)
    }

    def fileProgress(file:File)(bytes: Long) = {
      progressBytes += bytes
      setProgress(progressBytes)
      Log.trace("progressBytes " + progressBytes)
      Log.trace("progressBytes " + (bytes * 100 / filesToImportSizeSum))
    }

    val importResults = files.par.map(file => importFile(fileProgress(file))(file)).toList.sortBy(_._2)
    val importSuccessful = importResults.filter(_._1.isEmpty)
    val importFailed     = importResults.filter(_._1.nonEmpty)

    log("Imported files:\n"+importSuccessful.mkString("\n")+".")
    log("NOT Imported files:\n"+importFailed.mkString("\n")+".")

    val newList = alreadyImportedFiles ++ importSuccessful.map(_._2)
    newList.mkStringNL.writeToFile(alreadyImportedFile)

    importLabel.text("Import finished")
    progressBar.finished
  }

  def importFile(progressCallback: Long => Unit)(fileToImport:File): (/*TODO refactor to try */Option[String], File) = {

    try {
      Log.info("---------------------------------------------")
      //val tempFile = fileToImport.copyToDir(tempPathTextField.getText.asDir)

      var previousProgressBytes = 0L
      val fileToImportLength = fileToImport.length

      def convertActualBytesToProgress(actualBytes:Long) =  {
        val nextActualBytes = if(actualBytes<fileToImportLength) actualBytes else fileToImportLength
        progressCallback(nextActualBytes-previousProgressBytes)
        previousProgressBytes = nextActualBytes
      }

      val importError:Option[String] =
      //if (tempFile.nonEmpty)
      {
        //val fileName = tempFile.get.getName

        lazy val fileToImportName = fileToImport.withoutExtension.getName
        lazy val fileToImportExtension = fileToImport.extension.toLowerCase
        lazy val dateInFileName = "\\d{8}[_-]?\\d{6}"
        lazy val fileNameDTs = fileToImportName.findAll(dateInFileName)
        lazy val lastModifiedTime = fileToImport.lastModifiedTime.toString("yyyyMMdd_HHmmss")
        lazy val exifDT = exifDateTime(fileToImport)

        def extendWithSpaceIfNeeded(s:String) = {
          val ret = if(s.contains(" ")) s else s.patch(8," ",0)
          Log.info("extendWithSpaceIfNeeded "+ s+" "+s.contains(" ") + " ->"+ret)
          ret
        }


        val targetDateRaw =
          if(fileNameDTs.nonEmpty) fileNameDTs.last
          else if(exifDT.nonEmpty)  exifDT.get
          else  lastModifiedTime

        val targetDate = extendWithSpaceIfNeeded(targetDateRaw.replaceAll("[_-]"," ").trim)

        val fileNameWithoutDate = fileToImportName.replaceAll(dateInFileName,"").trim

        val targetFileWithCorrectName = ((exportPathTextField.getText + "\\"+targetDate+" "+fileNameWithoutDate).trim+fileToImportExtension).asFile
        val targetFileMaybeDuplicate =
          if (isVideoToConvert(fileToImport)) (targetFileWithCorrectName.withoutExtension + ".mp4").asFile
          else targetFileWithCorrectName

        if (targetFileMaybeDuplicate.exists) Log.info("ALREADY EXISTS "+targetFileMaybeDuplicate)

        val targetFile =  targetFileMaybeDuplicate.generateNewNameIfExists()
//        log(fileToImport+" "+fileToImportName)
//        log(fileToImport+" "+fileToImportExtension)
//        log(fileToImport+" "+dateInFileName)
//        log(fileToImport+" "+fileNameDTs)
//        log(fileToImport+" "+lastModifiedTime)
//        log(fileToImport+" "+targetDateRaw)
//        log(fileToImport+" "+targetDate)
//        log(fileToImport+" "+fileNameWithoutDate)
//        log(fileToImport+" "+targetFileWithCorrectName)
//        log(fileToImport+" "+targetFileMaybeDuplicate)
        log((if (targetFile.exists) "ALREADY EXISTS" else "START IMPORT")+"\t"+fileToImport.getName+"\t"+fileNameDTs+"\t"+lastModifiedTime+"\t"+exifDT+"\t"+targetFile.getName)

        val importResult:Option[String] =
          if(targetFile.exists) Option("ALREADY EXISTS")
          else if(doImport.isSelected) {
            // Import File
            if (isVideoToConvert(fileToImport)){
              Log.info("Convert video " + fileToImport + " to " + targetFile)

              val res = convertVideoToMp4(fileToImport, targetFile, convertActualBytesToProgress)
              targetFile.setLastModified(fileToImport.lastModified)
              Files.setAttribute(targetFile.toPath, "creationTime", FileTime.from(fileToImport.creationTime))


              Log.info("Import result " + fileToImport + " = " + res)
              if(res) None
              else Option("Conversion not successful to "+targetFile)
            }
            else {
              Log.info("Copy " + fileToImport + " to " + targetFile)

              val res = fileToImport.copyTo(targetFile)
              targetFile.setLastModified(fileToImport.lastModified)
              Files.setAttribute(targetFile.toPath, "creationTime", FileTime.from(fileToImport.creationTime))

              Log.info("Import result " + fileToImport + " = " + res)
              if(res) None
              else Option("Copy not successful to "+targetFile)
            }
          }
          else Option("Do import not selected")

        //val deleteTempSuccess = tempFile.get.delete
        //Log.info(s"Delete temp: $tempFile $deleteTempSuccess")

        importResult
      }
      //          else {
      //            Log.error("Cannot copy " + fileToImport + " to " + tempPathTextField.getText.asDir)
      //            false
      //          }

      convertActualBytesToProgress( fileToImportLength )

      if (importError.isEmpty) {
        (None, fileToImport)
      } else {
        (Option("NOT IMPORTED "+importError.get), fileToImport)
      }
    } catch {
      case e: Throwable => Log.error(e)
        (Option("ERROR "+e.getClass.getName+": "+e.getMessage), fileToImport)
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
        val filesToImport = if(useAlreadyImported.isSelected) allFilesToImport.diff(alreadyImportedFiles) else allFilesToImport
        Log.trace("filesToImport " + filesToImport.mkStringNL)
        val filesToImportSizeSum = filesToImport.map(_.length).sum

        checkToImportLabel.setText(filesToImport.size + " files to import, " + (filesToImportSizeSum / 1024 / 1024) + "MB")
        Log.info("Checking files to import: " + filesToImport.size + " files.")
        if (filesToImport.nonEmpty) frame.state_Normal.visible.toFront()

        filesToImport.sortBy(_.length)
      }
    finally checkToImportButton.setEnabled(true)
  }

  def log(txt: String) = logArea.append(txt+"\n")

  def writeEmail() =
    Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=Photo%20and%20video%20import&body=" + URLEncoder.encode(LogWindow.getAllLogs, "utf-8").replace("+", "%20")))

  def convertVideoToMp4(in: File, out: File, progress: Long => Unit) = {
    Log.info("Converting " + in + " to " + out)
    val convertBat = s"""${ffmpegPathTextField.getText} -i "$in" -vf yadif -vcodec mpeg4 -b:v 17M -acodec libmp3lame -b:a 192k -y "$out""""
    val exitCode = convertBat.executeAsProcessWithResultAndOutputLineCallback(s => s.findGroup("size= *(\\d*)kB".r).foreach(kB => progress(kB.toLong * 1024)))
    Log.info("Converting exitCode=" + exitCode)
    exitCode == 0
  }

  def isVideoToConvert(file: File) = file.endsWith(extensionsToConvertTextField.getText.split(","): _*)

  def alreadyImportedFile = (exportPathTextField.getText + "\\alreadyImported.txt").asFile
  def alreadyImportedFiles = alreadyImportedFile.lift.map(_.linesList.map(_.asFile)).getOrElse(List())

  def alert(msg: String) = JOptionPane.showMessageDialog(null, msg)

  def exifDateTime(file: File) = {
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