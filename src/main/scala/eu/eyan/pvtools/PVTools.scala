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
import javax.swing.JPanel
import com.jgoodies.forms.layout.FormLayout
import com.jgoodies.forms.factories.CC
import javax.swing.JLabel
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicit
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicitT
import rx.lang.scala.Observable
import eu.eyan.util.rx.lang.scala.ObservablePlus.ObservableImplicitT
import eu.eyan.pvtools.FFMPegPlus._
import eu.eyan.util.wip.Jobs

/**
 * TODO: konfig másodperc -> change, dont react immediately
 * TODO: auto import
 * TODO: read only -> nem sikerül videót másolni vagy képet másolni
 * TODO: fájlonként mentse az exportot
 */
object PVTools extends App {
  Log.activateInfoLevel
  // TODO LogWindow.redirectSystemOutAndErrToLogWindow

  /*
   * TODO low prio ffmpeg:_
   * https://stackoverflow.com/questions/6223765/start-a-java-process-at-low-priority-using-runtime-exec-processbuilder-start
   *
   * ProcessBuilder pb = new ProcessBuilder("cmd", "/C start /B /belownormal javaws -version");
		System.out.println("Before start");
Process start = pb.start();

   * */

  //TODO Include deshake at least for resized!!
  
  // TODO: crop ffmpeg -i in.mp4 -filter:v "crop=out_w:out_h:x:y" out.mp4
  // TODO: cut ffmpeg -ss 00:00:30 -i orginalfile -t 00:00:05 -vcodec copy -acodec copy newfile

/***************************  UI  ***************************************************/

  private val TITLE = "Photo and video import"

  private val jobs = Jobs()
  private val enabledState = jobs.workInProgress.not

  private val importPathClicked = BehaviorSubject[String]()
  private val importPathTexts = BehaviorSubject[List[String]]()

  private val exportPathClicked = BehaviorSubject[String]()
  private val exportPathTextField = BehaviorSubject[String]()

  private val tempPathTextField = BehaviorSubject[String]()
  private val tempPathClicked = BehaviorSubject[String]()

  private val extensionsToImportTextField = BehaviorSubject[String]()
  private val extensionsToConvertTextField = BehaviorSubject[String]()
  private val extensionsToResizeTextField = BehaviorSubject[String]()
  private val ffmpegPathTextField = BehaviorSubject[String]()
  private val ffprobePathTextField = BehaviorSubject[String]()
  private val exiftoolPathTextField = BehaviorSubject[String]()
  private val checkIntervalTextField = BehaviorSubject[String]()
  private val useAlreadyImported = BehaviorSubject[Boolean]()

  private val checkToImportClicked = BehaviorSubject[String]()

  private val checkToImportLabel = BehaviorSubject[String]()

  private val doImport = BehaviorSubject[Boolean]()

  private val importClicked = BehaviorSubject[String]()

  private val importLabel = BehaviorSubject[String]()

  private val progressBarFormat = BehaviorSubject[String]()
  private val progressBarValue = BehaviorSubject[Int]()
  private val progressBarFinished = BehaviorSubject[String]()

  private val filesToResize = BehaviorSubject[List[File]]()
  private val filesToRotate0 = BehaviorSubject[List[File]]()
  private val filesToRotate1 = BehaviorSubject[List[File]]()
  private val filesToRotate2 = BehaviorSubject[List[File]]()
  private val filesToRotate3 = BehaviorSubject[List[File]]()

  private val logArea = BehaviorSubject[String]()
  private val logAreaAppender = BehaviorSubject[String]()

  private val panel = JPanelBuilder().withBorders.withSeparators
    .newColumn.newColumnFPG

    .newRow("f:p").addLabel.text("Import path: ").cursor_HAND_CURSOR.onMouseClicked(importPathClicked)
    .nextColumn.addTextFieldMulti("importPathTextField", 30).setValues(List()).remember("importPathTextFields").onChanged(importPathTexts)

    .newRow.addLabel.text("Export path: ").cursor_HAND_CURSOR.onMouseClicked(exportPathClicked)
    .nextColumn.addTextField.text("").onTextChanged(exportPathTextField).remember("exportPath")

    .newRow.addLabel.text("Local temp path: ").cursor_HAND_CURSOR.onMouseClicked(tempPathClicked)
    .nextColumn.addTextField.text("").onTextChanged(tempPathTextField).remember("tempPath")

    .newRow.addLabel.text("Files to import: ")
    .nextColumn.addTextField.text("JPG,MTS,m2ts,mp4,jpeg").onTextChanged(extensionsToImportTextField).remember("extensionsToImport")

    .newRow.addLabel.text("Files to convert: ")
    .nextColumn.addTextField.text("MTS,m2ts").onTextChanged(extensionsToConvertTextField).remember("extensionsToConvert")

    .newRow.addLabel.text("Files to resize: ")
    .nextColumn.addTextField.text("MTS,m2ts,mp4").onTextChanged(extensionsToResizeTextField).remember("extensionsToResize")

    .newRow.addLabel.text("ffmpeg.exe location: ")
    .nextColumn.addTextField.text("""C:\private\ffmpeg\bin\ffmpeg.exe""").onTextChanged(ffmpegPathTextField).remember("ffmpeg")

    .newRow.addLabel.text("ffprobe.exe location: ")
    .nextColumn.addTextField.text("""C:\private\ffmpeg\bin\ffprobe.exe""").onTextChanged(ffprobePathTextField).remember("ffprobe")

    .newRow.addLabel.text("exiftool.exe location: ")
    .nextColumn.addTextField.text("""C:\private\exiftool.exe""").onTextChanged(exiftoolPathTextField).remember("exiftool")

    .newRow.addLabel.text("Check interval (s): ")
    .nextColumn.addTextField.text("3600").onTextChanged(checkIntervalTextField).remember("checkInterval")

    .newRow.addCheckBox.text("Use already imported txt").onSelectionChanged(useAlreadyImported).remember("useAlreadyImported")

    .newRow.addButton.text("Check to import").onAction(checkToImportClicked).enabled(enabledState)
    .nextColumn.addLabel.text("").text(checkToImportLabel)

    .newRow.addCheckBox.text("Do Import").onSelectionChanged(doImport).remember("doImport")

    .newRow.addButton.text("Import").onAction(importClicked).enabled(enabledState)
    .nextColumn.addLabel.text(importLabel)

    .newRow.addLabel.text("Progress:")
    .nextColumn.addProgressBar().format(progressBarFormat).value(progressBarValue).finished(progressBarFinished)

    .newRow.addButton.text("D&D to resize files").disabled.onDropFiles(filesToResize)
    .nextColumn.addPanelBuilder(_
      .withSeparators
      .addLabel.text("D&D to rotate with: ")
      .newColumn.addButton.text("-90VF").disabled.onDropFiles(filesToRotate0)
      .newColumn.addButton.text("90").disabled.onDropFiles(filesToRotate1)
      .newColumn.addButton.text("-90").disabled.onDropFiles(filesToRotate2)
      .newColumn.addButton.text("90VF").disabled.onDropFiles(filesToRotate3))

    .newRow("f:100px:g").span.addTextArea.textAppender(logAreaAppender).text(logArea)
    .getPanel

  private val frame: JFrame =
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

  case class Params(private val params: (List[String], String, String, String, String, String, String, String, String, String, Boolean, Boolean)) {
    def importPathTexts = params._1
    def exportPathTextField = params._2
    def tempPathTextField = params._3
    def extensionsToImportTextField = params._4
    def extensionsToConvertTextField = params._5
    def extensionsToResizeTextField = params._6
    def ffmpegPathTextField = params._7
    def ffprobePathTextField = params._8
    def exiftoolPathTextField = params._9
    def checkIntervalTextField = params._10
    def useAlreadyImported = params._11
    def doImport = params._12
  }
  val params$ = ObservablePlus.combineLatest(
    importPathTexts, exportPathTextField, tempPathTextField, extensionsToImportTextField, extensionsToConvertTextField, extensionsToResizeTextField, ffmpegPathTextField, ffprobePathTextField, exiftoolPathTextField, checkIntervalTextField, useAlreadyImported, doImport).map(Params)

  importPathTexts.subscribe(v => frame.resizeAndBack) // this resizeAndBack should be handled better! in the multi class

  importPathClicked.takeLatestOf(importPathTexts).subscribe(_.foreach(_.openAsFile))
  exportPathClicked.takeLatestOf(exportPathTextField).subscribe(_.openAsFile)
  tempPathClicked.takeLatestOf(tempPathTextField).subscribe(_.openAsFile)

  checkToImportClicked.takeLatestOf(params$).subscribe(params => execute(checkFilesToImport(params)))
  importClicked.takeLatestOf(params$).subscribe(params => execute(importFiles(params)))

  filesToResize.combineLatest(params$).subscribe(filesParams => execute(filesParams._1.foreach(resizeFile(720, filesParams._2.ffmpegPathTextField, filesParams._2.ffprobePathTextField, long => {/*TODO*/}))))

  def rotateFiles(rotate: Int)(filesFfmgep: (List[File], String)) = execute(filesFfmgep._1.foreach(rotateFile(filesFfmgep._2, rotate, l=>{})))
  filesToRotate0.combineLatest(ffmpegPathTextField).subscribe(rotateFiles(0)(_))
  filesToRotate1.combineLatest(ffmpegPathTextField).subscribe(rotateFiles(1)(_))
  filesToRotate2.combineLatest(ffmpegPathTextField).subscribe(rotateFiles(2)(_))
  filesToRotate3.combineLatest(ffmpegPathTextField).subscribe(rotateFiles(3)(_))

  private def execute(action: => Unit) = SwingPlus.runInWorker(jobs.run{action}) 

  private def importFiles(params: Params) = {
    def now = System.currentTimeMillis
    val start = now
    var progressBytes = 0L
    val progressBytes$ = BehaviorSubject(0L)
    val bytesPerSec$ = progressBytes$.map(_ / ((now - start) / 1000L + 1L))
    val speed$ = bytesPerSec$.map(_.toSize + "/s")
    speed$.subscribe(speed => progressBarFormat.onNext("%d%% " + speed))

    importLabel onNext "Importing"

    val files = checkFilesToImport(params)

    // calculate size
    val filesToImportSizeSum = files.map(_.length).sum
    progressBarValue onNext 0

    def setProgress(bytes: Long) = {
      val newVal = if (filesToImportSizeSum == 0) 0 else (bytes * 100 / filesToImportSizeSum).toInt
      progressBarValue onNext newVal
    }

    def fileProgress(file: File)(bytes: Long) = {
      progressBytes += bytes
      progressBytes$.onNext(progressBytes)
      setProgress(progressBytes)
      Log.trace("progressBytes " + progressBytes)
      Log.trace("progressBytes " + (bytes * 100 / filesToImportSizeSum))
    }

    val importResults = files /* .par FIXME */ .map(file => importFile(params, fileProgress(file))(file)).toList.sortBy(_._2)
    val importSuccessful = importResults.filter(_._1.isEmpty)
    val importFailed = importResults.filter(_._1.nonEmpty)

    log("Imported files:\n" + importSuccessful.mkString("\n") + ".")
    log("NOT Imported files:\n" + importFailed.mkString("\n") + ".")

    val newList = alreadyImportedFiles(params.exportPathTextField) ++ importSuccessful.map(_._2)
    newList.mkStringNL.writeToFileUtf8(alreadyImportedFile(params.exportPathTextField).toString)

    importLabel.onNext("Import finished")
    progressBarFinished onNext "finished"
  }

  private def importFile(params: Params, progressCallback: Long => Unit)(fileToImport: File): ( /*TODO refactor to try */ Option[String], File) = {
    try {
      Log.info("---------------------------------------------")
      //val tempFile = fileToImport.copyToDir(tempPathTextField.getText.asDir)

      var previousProgressBytes = 0L
      val fileToImportLength = fileToImport.length

      def convertActualBytesToProgress(actualBytes: Long): Unit = {
        val nextActualBytes = if (actualBytes < fileToImportLength) actualBytes else fileToImportLength
        progressCallback(nextActualBytes - previousProgressBytes)
        previousProgressBytes = nextActualBytes
      }

      val importError: Option[String] =
        //if (tempFile.nonEmpty)
        {
          //val fileName = tempFile.get.getName

          lazy val fileToImportName = fileToImport.withoutExtension.getName
          lazy val fileToImportExtension = fileToImport.extension.toLowerCase
          lazy val dateInFileName = "\\d{8}[_-]?\\d{6}"
          lazy val fileNameDTs = fileToImportName.findAll(dateInFileName)
          lazy val lastModifiedTime = fileToImport.lastModifiedTime.toString("yyyyMMdd_HHmmss")
          lazy val exifDT = exifDateTime(fileToImport, params.exiftoolPathTextField)

          def extendWithSpaceIfNeeded(s: String) = {
            val ret = if (s.contains(" ")) s else s.patch(8, " ", 0)
            Log.info("extendWithSpaceIfNeeded " + s + " " + s.contains(" ") + " ->" + ret)
            ret
          }

          val targetDateRaw =
            if (fileNameDTs.nonEmpty) fileNameDTs.last
            else if (exifDT.nonEmpty) exifDT.get
            else lastModifiedTime

          val targetDate = extendWithSpaceIfNeeded(targetDateRaw.replaceAll("[_-]", " ").trim)

          val fileNameWithoutDate = fileToImportName.replaceAll(dateInFileName, "").trim

          val targetFileWithCorrectName = ((params.exportPathTextField + "\\" + targetDate + " " + fileNameWithoutDate).trim + fileToImportExtension).asFile
          val targetFileMaybeDuplicate =
            if (isVideoToConvert(params.extensionsToConvertTextField)(fileToImport)) (targetFileWithCorrectName.withoutExtension + ".mp4").asFile
            else targetFileWithCorrectName

          if (targetFileMaybeDuplicate.exists) Log.info("ALREADY EXISTS " + targetFileMaybeDuplicate)

          val targetFile = targetFileMaybeDuplicate.generateNewNameIfExists()
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
          log((if (targetFile.exists) "ALREADY EXISTS" else "START IMPORT") + "\t" + fileToImport.getName + "\t" + fileNameDTs + "\t" + lastModifiedTime + "\t" + exifDT + "\t" + targetFile.getName)

          val importResult: Option[String] =
            if (targetFile.exists) Option("ALREADY EXISTS")
            else if (params.doImport) {

              if (isVideoToResize(params.extensionsToResizeTextField)(fileToImport)) {
                val resizeRes = TryCatch({
                  val resizeTargetFileName = targetFile.addSubDir("resized")
                  log("Resize video " + fileToImport + " to " + resizeTargetFileName)

                  val res = resizeFile(720, params.ffmpegPathTextField, params.ffprobePathTextField, fileToImport, resizeTargetFileName, convertActualBytesToProgress)
                  resizeTargetFileName.setLastModified(fileToImport.lastModified)
                  println("Importt " + fileToImport)
                  //Files.setAttribute(resizeTargetFileName.toPath, "creationTime", FileTime.from(fileToImport.creationTime))
                  println("Importtt")
                  //                  println("---")
                  //                  println("E " + res.exitValue)
                  //                  println("Output " + res.output)
                  //                  println("Erroe " + res.errorOutput)
                  //                  println("---")
                  res
                }, (e: Throwable) => println("Resizing error: " + e.getMessage))
              } else if (isImageToResize("jpg,JPG,jpeg,JPEG")(fileToImport)) {
                val resizeRes = TryCatch({
                  val resizeTargetFileName = targetFile.addSubDir("resized")
                  log("Resize image " + fileToImport + " to " + resizeTargetFileName)
                  val res = runFFMpeg(params.ffmpegPathTextField, fileToImport, resizeTargetFileName, convertActualBytesToProgress, FFMpegParam.SCALE_HEIGHT_NOOVERSIZE(1080))
                  resizeTargetFileName.setLastModified(fileToImport.lastModified)
                  //Files.setAttribute(resizeTargetFileName.toPath, "creationTime", FileTime.from(fileToImport.creationTime))
                  log("Resize image result " + res)
                  res
                }, (e: Throwable) => println("Resizing error: " + e.getMessage))
              } else log("Not to resize " + fileToImport)

              // Import File
              if (isVideoToConvert(params.extensionsToConvertTextField)(fileToImport)) {
                log("Convert video " + fileToImport + " to " + targetFile)

                
                val res = runFFMpeg(
                  params.ffmpegPathTextField,
                  fileToImport,
                  targetFile,
                  convertActualBytesToProgress,
                  FFMpegParam.VF_YADIF,
                  FFMpegParam.VCODEC_MPEG4,
                  FFMpegParam.BITRATE_VIDEO_17M,
                  FFMpegParam.AUDIO_CODEC_MP3,
                  FFMpegParam.BITRATE_AUDIO_192K)
                  
                targetFile.setLastModified(fileToImport.lastModified)
                Files.setAttribute(targetFile.toPath, "creationTime", FileTime.from(fileToImport.creationTime))

                log("Import result " + fileToImport + " = " + res)
                if (res == 0) None
                else Option("Conversion not successful to " + targetFile)
              } else {
                log("Copy " + fileToImport + " to " + targetFile)

                val res = fileToImport.copyTo(targetFile)
                targetFile.setLastModified(fileToImport.lastModified)
                Files.setAttribute(targetFile.toPath, "creationTime", FileTime.from(fileToImport.creationTime))

                log("Import result " + fileToImport + " = " + res)
                if (res) None
                else Option("Copy not successful to " + targetFile)
              }
            } else Option("Do import not selected")

          //val deleteTempSuccess = tempFile.get.delete
          //Log.info(s"Delete temp: $tempFile $deleteTempSuccess")

          importResult
        }
      //          else {
      //            Log.error("Cannot copy " + fileToImport + " to " + tempPathTextField.getText.asDir)
      //            false
      //          }

      convertActualBytesToProgress(fileToImportLength)

      if (importError.isEmpty) {
        (None, fileToImport)
      } else {
        (Option("NOT IMPORTED " + importError.get), fileToImport)
      }
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        Log.error(e)
        (Option("ERROR " + e.getClass.getName + ": " + e.getMessage), fileToImport)
    }
  }

  private def checkFilesToImport(params: Params): List[File] = {
    logArea.onNext("")
    Log.info("Checking files to import")
    if (params.importPathTexts.size < 1) { alert("Set at least one import dir please."); List() }
    else if (!params.importPathTexts.map(_.asDir.existsAndDir).forall(d2 => d2)) { alert("One of the import dirs does not exists."); List() }
    else if (!params.exportPathTextField.asDir.existsAndDir) { alert("Export dir does not exists."); List() }
    else if (!params.tempPathTextField.asDir.existsAndDir) { alert("Temp dir does not exists."); List() }
    else if (params.ffmpegPathTextField.asFile.notExists) { alert("ffmpeg does not exists."); List() }
    else if (params.ffprobePathTextField.asFile.notExists) { alert("ffprobe does not exists."); List() }
    else if (params.exiftoolPathTextField.asFile.notExists) { alert("exiftool does not exists."); List() }
    else {

      val dirs = params.importPathTexts
      val allFiles = dirs.flatMap(_.asDir.subFiles.toList)
      log("All files: " + allFiles.size)
      val allExtensions = allFiles.map(_.extension).distinct.map(_.toLowerCase.trim).map(_.substring(1)).toSet
      log("All extensions: " + allExtensions.mkString(", "))
      val extensionsToImport = params.extensionsToImportTextField.split(",").map(_.toLowerCase.trim).toSet
      log("ExtensionsToImport: " + extensionsToImport.mkString(", "))
      log("NOT to import: " + allExtensions.diff(extensionsToImport).mkString(", "))
      val allFilesToImport = allFiles.filter(_.endsWith(extensionsToImport.toList: _*))
      log("Files to Import: " + allFilesToImport.size)

      Log.trace("allFiles\r\n" + allFilesToImport.mkStringNL)
      Log.trace("\r\nalreadyImportedFiles\r\n" + alreadyImportedFiles(params.exportPathTextField).mkStringNL)
      val filesToImport = if (params.useAlreadyImported) {
//        println("allFilesToImport", allFilesToImport)
//        println("alreadyImportedFiles", alreadyImportedFiles(params.exportPathTextField))
//        println("diff", allFilesToImport.diff(alreadyImportedFiles(params.exportPathTextField)))
        allFilesToImport.diff(alreadyImportedFiles(params.exportPathTextField)) 
      }
      else allFilesToImport
      Log.trace("filesToImport " + filesToImport.mkStringNL)
      val filesToImportSizeSum = filesToImport.map(_.length).sum

      checkToImportLabel.onNext(filesToImport.size + " files to import, " + (filesToImportSizeSum / 1024 / 1024) + "MB")
      Log.info("Checking files to import: " + filesToImport.size + " files.")
      if (filesToImport.nonEmpty) frame.state_Normal.visible.toFront

      filesToImport.sortBy(_.length)
    }
  }

  private def log(txt: Any) = {
    Log.info("LOG "+txt)
    logAreaAppender.onNext(txt + "\n")
  }

  private def writeEmail() =
    Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=Photo%20and%20video%20import&body=" + URLEncoder.encode(LogWindow.getAllLogs, "utf-8").replace("+", "%20")))

  private def isVideoToConvert(extensionsToConvert: String)(file: File) = this.synchronized { file.endsWith(extensionsToConvert.split(","): _*) }

  private def isVideoToResize(extensionsToResize: String)(file: File) = file.endsWith(extensionsToResize.split(","): _*)
  private def isImageToResize(extensionsToResize: String)(file: File) = file.endsWith(extensionsToResize.split(","): _*)

  private def alreadyImportedFile(exportPath: String) = (exportPath + "\\alreadyImported.txt").asFile
  private def alreadyImportedFiles(exportPath: String) = alreadyImportedFile(exportPath).lift.map(_.linesListUtf8.map(_.asFile)).getOrElse(List())

  private def alert(msg: String) = JOptionPane.showMessageDialog(null, msg)

  private def exifDateTime(file: File, exiftoolPath: String) = {
    val exifCmd = exiftoolPath + " -T -DateTimeOriginal \"" + file + "\""

    val res = exifCmd.executeAsProcessWithResult(Codec.ISO8859)
    val exifDateTimeOption = res.output
    Log.debug("Result: " + exifDateTimeOption)
    if (exifDateTimeOption.nonEmpty) {
      val exifDateTime = exifDateTimeOption.get
      val clearedExifDateTime = exifDateTime.trim.replace(":", "").replace(" ", "_").replaceAll("\\+\\d*.DST", "")

      Log.debug("clearedExifDateTime " + clearedExifDateTime)
      val dateTime =
        if (clearedExifDateTime.matches("\\d+_\\d+")) Option(clearedExifDateTime)
        else None
      Log.debug(s"find out date time of $file exifDateTime=$exifDateTime clearedExifDateTime=$clearedExifDateTime dateTime=$dateTime")
      dateTime
    } else {
      Log.error("Error,        " + file + " " + exifCmd)
      Log.error("Error, exit   " + file + " " + res.exitValue)
      Log.error("Error, output " + file + " " + res.output)
      Log.error("Error, error  " + file + " " + res.errorOutput)
      None
    }
  }

  private def resizeFile(height: Int, ffmpegPath: String, ffprobePath: String, progress: Long=>Unit)(inFile: File): Unit = resizeFile(height, ffmpegPath, ffprobePath, inFile, inFile.addSubDir("resized"), progress)
  private def resizeFile(height: Int, ffmpegPath: String, ffprobePath: String, inFile: File, outFile: File, progress: Long=>Unit): Unit = {
    Log.info(inFile);
    val sizeCmd = ffprobePath + s""" -v error -select_streams v:0 -show_entries stream=width,height -of csv=s=x:p=0 "${inFile.getAbsolutePath}" """

    val video_width_x_height = sizeCmd.executeAsProcess.trim
    val matches = "(.*)x(.*)".r("width", "height").findAllIn(video_width_x_height)
    val video_width = matches.group("width").toInt
    val video_height = matches.group("height").toInt

    val rotateCmd = ffprobePath + s""" -v quiet -print_format json -show_format -show_streams "${inFile.getAbsolutePath}" """
    def rotateMatches(text: String) = """rotate": "(.*)"""".r("rotate").findAllIn(text).group("rotate")
    val video_rotate = rotateCmd.executeAsProcess.lines.find(_.contains("rotate")).map(rotateMatches).getOrElse("0").toInt

    val resolution = if (video_rotate == 90 || video_rotate == 270) (video_height, video_width)
    else (video_width, video_height)

    val widthPrimes = resolution._1.primeFactors.toList
    val heigthPrimes = resolution._2.primeFactors.toList
    val commonPrimes = widthPrimes.intersect(heigthPrimes)
    val dividerCombinations = (1 to commonPrimes.size).map(commonPrimes.combinations(_)).flatten
    val dividers = List(1) ++ (dividerCombinations.map(_.product))
    val dividedResolutions = dividers.map(divider => (resolution._1 / divider, resolution._2 / divider))
    val evenDividedResolutions = dividedResolutions.filter(wh => wh._1 % 2 == 0 && wh._2 % 2 == 0)
    val resolutionClosestToHeight = evenDividedResolutions.sortBy(wh => (wh._2 - height).abs)
    val resizeHeight = resolutionClosestToHeight.lift(0).map(_._2)

    //    log("Size of file " + inFile)
    //    log((video_rotate.toInt, resolution))
    //    log((widthPrimes, heigthPrimes))
    //    log(commonPrimes)
    //    log(dividerCombinations)
    //    log(dividers)
    //    log(dividedResolutions)
    //    log(evenDividedResolutions)
    //    log(resolutionClosestTo720)
    //    log(resizeHeight)

    if (resizeHeight.isEmpty) log("not possible to resize " + resolution)
    else {
      val height = resizeHeight.get
      log("Resize height=" + height + " " + inFile + " to " + outFile)
      log("Resize exit code: " + runFFMpeg(ffmpegPath, inFile, outFile, progress, FFMpegParam.SCALE_HEIGHT(height),
        FFMpegParam.CODEC_VIDEO_LIBX264,
        FFMpegParam.CONSTANT_RATE_FACTOR_VISUALLY_LOSSLESS,
        FFMpegParam.PRESET_MEDIUM,
        FFMpegParam.CODEC_AUDIO_COPY))
    }
    log("---Done---")
  }

  private def rotateFile(ffmpegPath: String, transpose: Int, bytesDoneProgress: Long => Unit)(inFile: File): Unit = {
    val out = inFile.extendFileNameWith("_r" + transpose.toString)
    log("Rotate " + transpose + " " + inFile + " " + out)
    log("Rotate exit code: " + runFFMpeg(ffmpegPath, inFile, out, bytesDoneProgress, FFMpegParam.TRANSPOSE(transpose)))
  }
  
}
