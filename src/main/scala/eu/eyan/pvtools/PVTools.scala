package eu.eyan.pvtools

import java.awt.{ Color, Desktop }
import java.io.File
import java.net.{ URI, URLEncoder }
import java.nio.file.Files
import java.nio.file.attribute.FileTime

import eu.eyan.log.{ Log, LogWindow }
import eu.eyan.util.awt.clipboard.ClipboardPlus
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import eu.eyan.util.java.time.InstantPlus.InstantImplicit
import eu.eyan.util.registry.RegistryPlus
import eu.eyan.util.scala.collection.TraversableOncePlus.TraversableOnceImplicit
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.swing.JButtonPlus.JButtonImplicit
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JLabelPlus.JLabelImplicit
import eu.eyan.util.swing.JPanelWithFrameLayout
import eu.eyan.util.swing.JTextFieldPlus.JTextFieldPlusImplicit
import eu.eyan.util.swing.JToggleButtonPlus.JToggleButtonImplicit
import javax.swing.{ JFrame, JOptionPane, JScrollPane }

import scala.io.Codec
import eu.eyan.util.java.lang.IntPlus.IntImplicit
import eu.eyan.util.scala.Try
import eu.eyan.util.scala.TryCatch
import rx.lang.scala.subjects.BehaviorSubject
import eu.eyan.util.rx.lang.scala.ObservablePlus
import eu.eyan.util.rx.lang.scala.ObservablePlus.ObservableImplicitBoolean
import eu.eyan.util.swing.SwingPlus
import java.awt.Dimension
import eu.eyan.util.scala.LongPlus.LongImplicit
import eu.eyan.util.swing.panelbuilder.JPanelBuilder

/**
 * TODO: konfig másodperc -> change, dont react immediately
 * TODO: auto import
 * TODO: read only -> nem sikerül videót másolni vagy képet másolni
 * TODO: fájlonként mentse az exportot
 */
object PVTools extends App {
  Log.activateInfoLevel
  //LogWindow.redirectSystemOutAndErrToLogWindow

/***************************  UI  ***************************************************/

  val TITLE = "Photo and video import"

  val inProgress = BehaviorSubject(false)
  val enabledState = inProgress.not

  val importPathTexts = BehaviorSubject[List[String]]()
  
  val exportPathTextField = BehaviorSubject[String]()
  val exportPathClicked = BehaviorSubject[String]()
  
  val tempPathTextField  = BehaviorSubject[String]()
  val tempPathClicked = BehaviorSubject[String]()

  val extensionsToImportTextField  = BehaviorSubject[String]()
  val extensionsToConvertTextField  = BehaviorSubject[String]()
  val extensionsToResizeTextField  = BehaviorSubject[String]()
  val ffmpegPathTextField  = BehaviorSubject[String]()
  val ffprobePathTextField  = BehaviorSubject[String]()
  val exiftoolPathTextField  = BehaviorSubject[String]()
  val checkIntervalTextField   = BehaviorSubject[String]()
  val useAlreadyImported   = BehaviorSubject[Boolean]()

  val checkToImportClicked = BehaviorSubject[String]()

  val checkToImportLabel = BehaviorSubject[String]()

  val doImport   = BehaviorSubject[Boolean]()
  
  val importClicked = BehaviorSubject[String]()

  val importLabel = BehaviorSubject[String]()

  val progressBar = BehaviorSubject[String]()
  
  val filesToResize = BehaviorSubject[List[File]]()
  val filesToRotate0 = BehaviorSubject[List[File]]()
  val filesToRotate1 = BehaviorSubject[List[File]]()
  val filesToRotate2 = BehaviorSubject[List[File]]()
  val filesToRotate3 = BehaviorSubject[List[File]]()

  val logAreaAppender = BehaviorSubject[String]()
  
  exportPathClicked.switchMap(s => exportPathTextField).subscribe(_.openAsFile)
  importPathTexts.subscribe(v => frame.resizeAndBack)
  tempPathClicked.switchMap(s => tempPathTextField).subscribe(_.openAsFile)
  checkToImportClicked.subscribe(s => execute(checkFilesToImport))
  importClicked.subscribe(s => execute(importFiles))
  filesToResize.subscribe(files => execute(files.foreach(resizeFile)))
  filesToRotate0.subscribe(files => execute(files.foreach(rotateFile(0))))
  filesToRotate1.subscribe(files => execute(files.foreach(rotateFile(1))))
  filesToRotate2.subscribe(files => execute(files.foreach(rotateFile(2))))
  filesToRotate3.subscribe(files => execute(files.foreach(rotateFile(3))))
  
  val panel = JPanelBuilder().withBorders.withSeparators
    .newColumn.newColumnFPG

    .newRow("f:p").addLabel.text("Import path: ").cursor_HAND_CURSOR
    .nextColumn.addTextFieldMulti("importPathTextField", 30).remember("importPathTextFields").onChanged(importPathTexts)

    .newRow.addLabel.text("Export path: ").cursor_HAND_CURSOR.onMouseClicked(exportPathClicked)
    .nextColumn.addTextField.text("").onTextChanged(exportPathTextField).remember("exportPath")
//
//    .newRow.addLabel.text("Local temp path: ").cursor_HAND_CURSOR.onMouseClicked(tempPathClicked)
//    .nextColumn.addTextField.text("").onTextChanged(tempPathTextField).remember("tempPath")
//
//    .newRow.addLabel.text("Files to import: ")
//    .nextColumn.addTextField.text("JPG,MTS,m2ts,mp4,jpeg").onTextChanged(extensionsToImportTextField).remember("extensionsToImport")
//
//    .newRow.addLabel.text("Files to convert: ")
//    .nextColumn.addTextField.text("MTS,m2ts").onTextChanged(extensionsToImportTextField).remember("extensionsToConvert")
//
//    .newRow.addLabel.text("Files to resize: ")
//    .nextColumn.addTextField.text("MTS,m2ts,mp4").onTextChanged(extensionsToResizeTextField).remember("extensionsToResize")
//
//    .newRow.addLabel.text("ffmpeg.exe location: ")
//    .nextColumn.addTextField.text("""C:\private\ffmpeg\bin\ffmpeg.exe""").onTextChanged(ffmpegPathTextField).remember("ffmpeg")
//
//    .newRow.addLabel.text("ffprobe.exe location: ")
//    .nextColumn.addTextField.text("""C:\private\ffmpeg\bin\ffprobe.exe""").onTextChanged(ffprobePathTextField).remember("ffprobe")
//
//    .newRow.addLabel.text("exiftool.exe location: ")
//    .nextColumn.addTextField.text("""C:\private\exiftool.exe""").onTextChanged(exiftoolPathTextField).remember("exiftool")
//
//    .newRow.addLabel.text("Check interval (s): ")
//    .nextColumn.addTextField.text("3600").onTextChanged(checkIntervalTextField ).remember("checkInterval")
//
//    .newRow.addCheckBox.text("Use already imported txt").onSelectionChanged(useAlreadyImported).remember("useAlreadyImported")
//
//    .newRow.addButton.text("Check to import").onAction(checkToImportClicked).enabled(enabledState)
//    .nextColumn.addLabel.text("").text(checkToImportLabel)
//
//    .newRow.addCheckBox.text("Do Import").onSelectionChanged(doImport).remember("doImport")
//
//    .newRow.addButton.text("Import").onAction(importClicked).enabled(enabledState)
//    .nextColumn.addLabel.text(importLabel)
//
//    .newRow.addLabel.text("Progress:")
//    .nextColumn.addProgressBar()
//
//    .newRow.addButton.text("D&D to resize files").disabled.onDropFiles(filesToResize)
//    .nextColumn.addPanelBuilder(_//.withSeparators
//      .addLabel.text("D&D to rotate with: ")
//      .newColumn.addButton.text("-90VF").disabled.onDropFiles(filesToRotate0)
//      .newColumn.addButton.text("90").disabled.onDropFiles(filesToRotate1)
//      .newColumn.addButton.text("-90").disabled.onDropFiles(filesToRotate2)
//      .newColumn.addButton.text("90VF").disabled.onDropFiles(filesToRotate3))
//    
//
//    .newRow("f:100px:g").span.addTextArea.textAppender(logAreaAppender)
  
/***************************  ORIG PANEL   ***************************************************/
//  val panel = new JPanelWithFrameLayout().withBorders.withSeparators
//    .newColumn.newColumnFPG
//
//  panel.newRow("f:p").addLabel("Import path: ").cursor_HAND_CURSOR
//  val importPathTextField = panel.nextColumn.addTextFieldMulti("importPathTextField", 30, List()).rememberValueInRegistry("importPathTextFields")
//  importPathTextField.onChanged(() => frame.resizeAndBack)
//
//  panel.newRow.addLabel("Export path: ").cursor_HAND_CURSOR.onClicked(exportPathTextField.getText.openAsFile)
//  val exportPathTextField = panel.nextColumn.addTextField("").rememberValueInRegistry("exportPath")
//
//  panel.newRow.addLabel("Local temp path: ").cursor_HAND_CURSOR.onClicked(tempPathTextField.getText.openAsFile)
//  val tempPathTextField = panel.nextColumn.addTextField("").rememberValueInRegistry("tempPath")
//
//  panel.newRow.addLabel("Files to import: ")
//  val extensionsToImportTextField = panel.nextColumn.addTextField("JPG,MTS,m2ts,mp4,jpeg").rememberValueInRegistry("extensionsToImport")
//
//  panel.newRow.addLabel("Files to convert: ")
//  val extensionsToConvertTextField = panel.nextColumn.addTextField("MTS,m2ts").rememberValueInRegistry("extensionsToConvert")
//
//  panel.newRow.addLabel("Files to resize: ")
//  val extensionsToResizeTextField = panel.nextColumn.addTextField("MTS,m2ts,mp4").rememberValueInRegistry("extensionsToResize")
//
//  panel.newRow.addLabel("ffmpeg.exe location: ")
//  val ffmpegPathTextField = panel.nextColumn.addTextField("""C:\private\ffmpeg\bin\ffmpeg.exe""").rememberValueInRegistry("ffmpeg")
//
//  panel.newRow.addLabel("ffprobe.exe location: ")
//  val ffprobePathTextField = panel.nextColumn.addTextField("""C:\private\ffmpeg\bin\ffprobe.exe""").rememberValueInRegistry("ffprobe")
//
//  panel.newRow.addLabel("exiftool.exe location: ")
//  val exiftoolPathTextField = panel.nextColumn.addTextField("""C:\private\exiftool.exe""").rememberValueInRegistry("exiftool")
//
//  panel.newRow.addLabel("Check interval (s): ")
//  val checkIntervalTextField = panel.nextColumn.addTextField("3600").rememberValueInRegistry("checkInterval")
//
//  val useAlreadyImported = panel.newRow.addCheckBox("Use already imported txt").rememberValueInRegistry("useAlreadyImported")
//
//  val checkToImportButton = panel.newRow.addButton("Check to import").onAction(execute(checkFilesToImport)).enabled(enabledState)
//  val checkToImportLabel = panel.nextColumn.addLabel("")
//
//  val doImport = panel.newRow.addCheckBox("Do Import").rememberValueInRegistry("doImport")
//
//  panel.newRow.addButton("Import").onAction(execute(importFiles)).enabled(enabledState)
//  val importLabel = panel.nextColumn.addLabel("")
//
//  panel.newRow.addLabel("Progress:")
//  val progressBar = panel.nextColumn.addProgressBar()
//
//  panel.newRow.addButton("D&D to resize files").disabled.onDropFiles(files => execute(files.foreach(resizeFile)))
//  val rotatePanel = panel.nextColumn.addPanelWithFormLayout().withSeparators
//  rotatePanel.addLabel("D&D to rotate with: ")
//  rotatePanel.newColumn.addButton("-90VF").disabled.onDropFiles(files => execute(files.foreach(rotateFile(0))))
//  rotatePanel.newColumn.addButton("90").disabled.onDropFiles(files => execute(files.foreach(rotateFile(1))))
//  rotatePanel.newColumn.addButton("-90").disabled.onDropFiles(files => execute(files.foreach(rotateFile(2))))
//  rotatePanel.newColumn.addButton("90VF").disabled.onDropFiles(files => execute(files.foreach(rotateFile(3))))
//
//  val logArea = panel.newRow("f:100px:g").span.addTextArea()

  val frame: JFrame =
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

  def execute(action: => Unit) = {
    println("execute")
    inProgress.onNext(true); SwingPlus.runInWorker(action, inProgress.onNext(false))

  }

  def importFiles = {
//    def now = System.currentTimeMillis
//    val start = now
//    var progressBytes = 0L
//    val progressBytes$ = BehaviorSubject(0L)
//    val bytesPerSec$ = progressBytes$.map(_ / ((now - start) / 1000L + 1L))
//    val speed$ = bytesPerSec$.map(_.toSize + "/s")
//    speed$.subscribe(speed => progressBar.setFormat("%d%% " + speed))
//
//    importLabel.text("Importing")
//
//    val files = checkFilesToImport
//
//    // calculate size
//    val filesToImportSizeSum = files.map(_.length).sum
//    progressBar valueChanged 0
//
//    Log.deactivate //FIXME
//    def setProgress(bytes: Long) = {
//      val newVal = if (filesToImportSizeSum == 0) 0 else (bytes * 100 / filesToImportSizeSum).toInt
//      progressBar.valueChanged(newVal)
//    }
//
//    def fileProgress(file: File)(bytes: Long) = {
//      progressBytes += bytes
//      progressBytes$.onNext(progressBytes)
//      setProgress(progressBytes)
//      Log.trace("progressBytes " + progressBytes)
//      Log.trace("progressBytes " + (bytes * 100 / filesToImportSizeSum))
//    }
//
//    val importResults = files.par.map(file => importFile(fileProgress(file))(file)).toList.sortBy(_._2)
//    val importSuccessful = importResults.filter(_._1.isEmpty)
//    val importFailed = importResults.filter(_._1.nonEmpty)
//
//    log("Imported files:\n" + importSuccessful.mkString("\n") + ".")
//    log("NOT Imported files:\n" + importFailed.mkString("\n") + ".")
//
//    val newList = alreadyImportedFiles ++ importSuccessful.map(_._2)
//    newList.mkStringNL.writeToFile(alreadyImportedFile)
//
//    importLabel.text("Import finished")
//    progressBar.finished
    ???
  }

  def importFile(progressCallback: Long => Unit)(fileToImport: File): ( /*TODO refactor to try */ Option[String], File) = {
//    try {
//      Log.info("---------------------------------------------")
//      //val tempFile = fileToImport.copyToDir(tempPathTextField.getText.asDir)
//
//      var previousProgressBytes = 0L
//      val fileToImportLength = fileToImport.length
//
//      def convertActualBytesToProgress(actualBytes: Long): Unit = {
//        val nextActualBytes = if (actualBytes < fileToImportLength) actualBytes else fileToImportLength
//        progressCallback(nextActualBytes - previousProgressBytes)
//        previousProgressBytes = nextActualBytes
//      }
//
//      val importError: Option[String] =
//        //if (tempFile.nonEmpty)
//        {
//          //val fileName = tempFile.get.getName
//
//          lazy val fileToImportName = fileToImport.withoutExtension.getName
//          lazy val fileToImportExtension = fileToImport.extension.toLowerCase
//          lazy val dateInFileName = "\\d{8}[_-]?\\d{6}"
//          lazy val fileNameDTs = fileToImportName.findAll(dateInFileName)
//          lazy val lastModifiedTime = fileToImport.lastModifiedTime.toString("yyyyMMdd_HHmmss")
//          lazy val exifDT = exifDateTime(fileToImport)
//
//          def extendWithSpaceIfNeeded(s: String) = {
//            val ret = if (s.contains(" ")) s else s.patch(8, " ", 0)
//            Log.info("extendWithSpaceIfNeeded " + s + " " + s.contains(" ") + " ->" + ret)
//            ret
//          }
//
//          val targetDateRaw =
//            if (fileNameDTs.nonEmpty) fileNameDTs.last
//            else if (exifDT.nonEmpty) exifDT.get
//            else lastModifiedTime
//
//          val targetDate = extendWithSpaceIfNeeded(targetDateRaw.replaceAll("[_-]", " ").trim)
//
//          val fileNameWithoutDate = fileToImportName.replaceAll(dateInFileName, "").trim
//
//          val targetFileWithCorrectName = ((exportPathTextField.getText + "\\" + targetDate + " " + fileNameWithoutDate).trim + fileToImportExtension).asFile
//          val targetFileMaybeDuplicate =
//            if (isVideoToConvert(fileToImport)) (targetFileWithCorrectName.withoutExtension + ".mp4").asFile
//            else targetFileWithCorrectName
//
//          if (targetFileMaybeDuplicate.exists) Log.info("ALREADY EXISTS " + targetFileMaybeDuplicate)
//
//          val targetFile = targetFileMaybeDuplicate.generateNewNameIfExists()
//          //        log(fileToImport+" "+fileToImportName)
//          //        log(fileToImport+" "+fileToImportExtension)
//          //        log(fileToImport+" "+dateInFileName)
//          //        log(fileToImport+" "+fileNameDTs)
//          //        log(fileToImport+" "+lastModifiedTime)
//          //        log(fileToImport+" "+targetDateRaw)
//          //        log(fileToImport+" "+targetDate)
//          //        log(fileToImport+" "+fileNameWithoutDate)
//          //        log(fileToImport+" "+targetFileWithCorrectName)
//          //        log(fileToImport+" "+targetFileMaybeDuplicate)
//          log((if (targetFile.exists) "ALREADY EXISTS" else "START IMPORT") + "\t" + fileToImport.getName + "\t" + fileNameDTs + "\t" + lastModifiedTime + "\t" + exifDT + "\t" + targetFile.getName)
//
//          val importResult: Option[String] =
//            if (targetFile.exists) Option("ALREADY EXISTS")
//            else if (doImport.isSelected) {
//
//              if (isVideoToResize(fileToImport)) {
//                val resizeRes = TryCatch({
//                  val resizeTargetFileName = targetFile.addSubDir("resized")
//                  Log.info("Resize video " + fileToImport + " to " + resizeTargetFileName)
//
//                  //val res = resizeVideoToMp4(fileToImport, resizeTargetFileName)
//                  val res = resizeFile(fileToImport, resizeTargetFileName)
//                  resizeTargetFileName.setLastModified(fileToImport.lastModified)
//                  println("Importt " + fileToImport)
//                  //Files.setAttribute(resizeTargetFileName.toPath, "creationTime", FileTime.from(fileToImport.creationTime))
//                  println("Importtt")
//                  //                  println("---")
//                  //                  println("E " + res.exitValue)
//                  //                  println("Output " + res.output)
//                  //                  println("Erroe " + res.errorOutput)
//                  //                  println("---")
//                  res
//                }, (e: Throwable) => println("Resizing error: " + e.getMessage))
//              } else println("Not to resize " + fileToImport)
//
//              println("forward " + fileToImport)
//              // Import File
//              if (isVideoToConvert(fileToImport)) {
//                Log.info("Convert video " + fileToImport + " to " + targetFile)
//
//                val res = ffmpeg(fileToImport, targetFile, " -vf yadif -vcodec mpeg4 -b:v 17M -acodec libmp3lame -b:a 192k -y ", convertActualBytesToProgress)
//                targetFile.setLastModified(fileToImport.lastModified)
//                Files.setAttribute(targetFile.toPath, "creationTime", FileTime.from(fileToImport.creationTime))
//
//                Log.info("Import result " + fileToImport + " = " + res)
//                if (res == 0) None
//                else Option("Conversion not successful to " + targetFile)
//              } else {
//                Log.info("Copy " + fileToImport + " to " + targetFile)
//
//                val res = fileToImport.copyTo(targetFile)
//                targetFile.setLastModified(fileToImport.lastModified)
//                Files.setAttribute(targetFile.toPath, "creationTime", FileTime.from(fileToImport.creationTime))
//
//                Log.info("Import result " + fileToImport + " = " + res)
//                if (res) None
//                else Option("Copy not successful to " + targetFile)
//              }
//            } else Option("Do import not selected")
//
//          //val deleteTempSuccess = tempFile.get.delete
//          //Log.info(s"Delete temp: $tempFile $deleteTempSuccess")
//
//          importResult
//        }
//      //          else {
//      //            Log.error("Cannot copy " + fileToImport + " to " + tempPathTextField.getText.asDir)
//      //            false
//      //          }
//
//      convertActualBytesToProgress(fileToImportLength)
//
//      if (importError.isEmpty) {
//        (None, fileToImport)
//      } else {
//        (Option("NOT IMPORTED " + importError.get), fileToImport)
//      }
//    } catch {
//      case e: Throwable =>
//        Log.error(e)
//        (Option("ERROR " + e.getClass.getName + ": " + e.getMessage), fileToImport)
//    }
    ???
  }

  def checkFilesToImport: List[File] = {
//    logArea.setText("")
//    Log.info("Checking files to import")
//    if (importPathTextField.getValues.size < 1) { alert("At least on import dir please."); List() }
//    else if (!importPathTextField.getValues.map(_.asDir.existsAndDir).forall(d2 => d2)) { alert("One of the import dirs does not exists."); List() }
//    else if (!exportPathTextField.getText.asDir.existsAndDir) { alert("Export dir does not exists."); List() }
//    else if (!tempPathTextField.getText.asDir.existsAndDir) { alert("Temp dir does not exists."); List() }
//    else if (ffmpegPath.asFile.notExists) { alert("ffmpeg does not exists."); List() }
//    else if (ffprobePathTextField.getText.asFile.notExists) { alert("ffprobe does not exists."); List() }
//    else if (exiftoolPathTextField.getText.asFile.notExists) { alert("exiftool does not exists."); List() }
//    else {
//
//      val dirs = importPathTexts.getValues
//      val allFiles = dirs.flatMap(_.asDir.subFiles.toList)
//      log("All files: " + allFiles.size)
//      val allExtensions = allFiles.map(_.extension).distinct.map(_.toLowerCase.trim).map(_.substring(1)).toSet
//      log("All extensions: " + allExtensions.mkString(", "))
//      val extensionsToImport = extensionsToImportTextField.getText.split(",").map(_.toLowerCase.trim).toSet
//      log("ExtensionsToImport: " + extensionsToImport.mkString(", "))
//      log("NOT to import: " + allExtensions.diff(extensionsToImport).mkString(", "))
//      val allFilesToImport = allFiles.filter(_.endsWith(extensionsToImport.toList: _*))
//      log("Files to Import: " + allFilesToImport.size)
//
//      Log.trace("allFiles\r\n" + allFilesToImport.mkStringNL)
//      Log.trace("\r\nalreadyImportedFiles\r\n" + alreadyImportedFiles.mkStringNL)
//      val filesToImport = if (useAlreadyImported.isSelected) allFilesToImport.diff(alreadyImportedFiles) else allFilesToImport
//      Log.trace("filesToImport " + filesToImport.mkStringNL)
//      val filesToImportSizeSum = filesToImport.map(_.length).sum
//
//      checkToImportLabel.setText(filesToImport.size + " files to import, " + (filesToImportSizeSum / 1024 / 1024) + "MB")
//      Log.info("Checking files to import: " + filesToImport.size + " files.")
//      if (filesToImport.nonEmpty) frame.state_Normal.visible.toFront()
//
//      filesToImport.sortBy(_.length)
//    }
    ???
  }

  def log(txt: Any) = ???//logArea.append(txt + "\n")

  def writeEmail() =
    Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=Photo%20and%20video%20import&body=" + URLEncoder.encode(LogWindow.getAllLogs, "utf-8").replace("+", "%20")))

  def isVideoToConvert(file: File) = ??? //file.endsWith(extensionsToConvertTextField.getText.split(","): _*)
  def isVideoToResize(file: File) = ??? //file.endsWith(extensionsToResizeTextField.getText.split(","): _*)

  def alreadyImportedFile = ??? //(exportPathTextField.getText + "\\alreadyImported.txt").asFile
  def alreadyImportedFiles = {
//    println(alreadyImportedFile)
//    alreadyImportedFile.lift.map(_.linesList.map(_.asFile)).getOrElse(List())
  }

  def alert(msg: String) = JOptionPane.showMessageDialog(null, msg)

  def exifDateTime(file: File) = {
//    val exifCmd = exiftoolPathTextField.getText + " -T -DateTimeOriginal \"" + file + "\""
//
//    val res = exifCmd.executeAsProcessWithResult(Codec.ISO8859)
//    val exifDateTimeOption = res.output
//    Log.debug("Result: " + exifDateTimeOption)
//    if (exifDateTimeOption.nonEmpty) {
//      val exifDateTime = exifDateTimeOption.get
//      val clearedExifDateTime = exifDateTime.trim.replace(":", "").replace(" ", "_").replaceAll("\\+\\d*.DST", "")
//
//      Log.debug("clearedExifDateTime " + clearedExifDateTime)
//      val dateTime =
//        if (clearedExifDateTime.matches("\\d+_\\d+")) Option(clearedExifDateTime)
//        else None
//      Log.debug(s"find out date time of $file exifDateTime=$exifDateTime clearedExifDateTime=$clearedExifDateTime dateTime=$dateTime")
//      dateTime
//    } else {
//      Log.error("Error,        " + file + " " + exifCmd)
//      Log.error("Error, exit   " + file + " " + res.exitValue)
//      Log.error("Error, output " + file + " " + res.output)
//      Log.error("Error, error  " + file + " " + res.errorOutput)
//      None
//    }
    ???
  }

  def resizeFile(inFile: File): Unit = resizeFile(inFile, inFile.addSubDir("resized"))
  def resizeFile(inFile: File, outFile: File): Unit = {
//    Log.info(inFile);
//    val sizeCmd = ffprobePathTextField.getText + s""" -v error -select_streams v:0 -show_entries stream=width,height -of csv=s=x:p=0 "${inFile.getAbsolutePath}" """
//
//    val video_width_x_height = sizeCmd.executeAsProcess.trim
//    val matches = "(.*)x(.*)".r("width", "height").findAllIn(video_width_x_height)
//    val video_width = matches.group("width").toInt
//    val video_height = matches.group("height").toInt
//
//    val rotateCmd = ffprobePathTextField.getText + s""" -v quiet -print_format json -show_format -show_streams "${inFile.getAbsolutePath}" """
//    def rotateMatches(text: String) = """rotate": "(.*)"""".r("rotate").findAllIn(text).group("rotate")
//    val video_rotate = rotateCmd.executeAsProcess.lines.find(_.contains("rotate")).map(rotateMatches).getOrElse("0").toInt
//
//    val resolution = if (video_rotate == 90 || video_rotate == 270) (video_height, video_width)
//    else (video_width, video_height)
//
//    val widthPrimes = resolution._1.primeFactors.toList
//    val heigthPrimes = resolution._2.primeFactors.toList
//    val commonPrimes = widthPrimes.intersect(heigthPrimes)
//    val dividerCombinations = (1 to commonPrimes.size).map(commonPrimes.combinations(_)).flatten
//    val dividers = List(1) ++ (dividerCombinations.map(_.product))
//    val dividedResolutions = dividers.map(divider => (resolution._1 / divider, resolution._2 / divider))
//    val evenDividedResolutions = dividedResolutions.filter(wh => wh._1 % 2 == 0 && wh._2 % 2 == 0)
//    val resolutionClosestTo720 = evenDividedResolutions.sortBy(wh => (wh._2 - 720).abs)
//    val resizeHeight = resolutionClosestTo720.lift(0).map(_._2)
//
//    //    log("Size of file " + inFile)
//    //    log((video_rotate.toInt, resolution))
//    //    log((widthPrimes, heigthPrimes))
//    //    log(commonPrimes)
//    //    log(dividerCombinations)
//    //    log(dividers)
//    //    log(dividedResolutions)
//    //    log(evenDividedResolutions)
//    //    log(resolutionClosestTo720)
//    //    log(resizeHeight)
//
//    if (resizeHeight.isEmpty) log("not possible to resize " + resolution)
//    else {
//      val in = inFile.getAbsolutePath
//      val out = outFile.getAbsolutePath
//      val height = resizeHeight.get
//      val resizeCmd = ffmpegPath + s"""  -i "$in" -vf scale=-1:$height -c:v libx264 -crf 18 -preset veryslow -c:a copy "$out" """
//      val resizeRes = resizeCmd.executeAsProcess
//      println(inFile + resizeRes)
//    }
    log("---Done---")
  }

  def rotateFile(transpose: Int)(inFile: File): Unit = {
//    val in = inFile.getAbsolutePath
//    val out = inFile.extendFileNameWith("_r" + transpose.toString)
//    log("Rotate " + transpose + " " + inFile + " " + out)
//    val rotateCmd = ffmpegPath + s"""  -i "$in" -vf "transpose=$transpose" "$out" """
//    val rotateRes = rotateCmd.executeAsProcess
//    log(rotateRes)
  }

  def resizeVideoToMp4(in: File, out: File) = {
    val height = 960
    val outFile = out.extendFileNameWith("" + height)
    Log.info("Resizing " + in + " to " + outFile)
    val convertBat = s"""${ffmpegPath} -i "$in" -vf scale=-1:960 -c:v libx264 -crf 18 -preset veryslow -c:a copy "$outFile""""
    val processResult = convertBat.executeAsProcessWithResult
    Log.info("Resizing exitCode=" + processResult.exitValue)
    processResult
  }

  private def ffmpeg(in: File, out: File, parameters: String, bytesDoneProgress: Long => Unit): Int = {
    val cmd = s""" $ffmpegPath -i "$in" $parameters "$out" """
    Log.info("Executing " + cmd)
    val exitCode = cmd.executeAsProcessWithResultAndOutputLineCallback(s => s.findGroup("size= *(\\d*)kB".r).foreach(kB => bytesDoneProgress(kB.toLong * 1024)))
    Log.info("ExitCode=" + exitCode)
    exitCode
  }

  private def ffmpegPath = ???//ffmpegPathTextField.getText
}
