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
import eu.eyan.util.rx.lang.scala.ObserverPlus.ObserverImplicit
import eu.eyan.util.swing.panelbuilder.Click
import eu.eyan.pvtools.FFMpegParam._
import eu.eyan.pvtools.FFMpegVideoParam._

/**
 * TODO: read only -> nem sikerül videót másolni vagy képet másolni
 * TODO: fájlonként mentse az exportot
 */
object PVTools extends App {
  Log.activateInfoLevel
  LogWindow.redirectSystemOutAndErrToLogWindow

  /*
   * TODO low prio ffmpeg:_
   * https://stackoverflow.com/questions/6223765/start-a-java-process-at-low-priority-using-runtime-exec-processbuilder-start
   * ProcessBuilder pb = new ProcessBuilder("cmd", "/C start /B /belownormal javaws -version");
	 * Process start = pb.start();
   * */

/***************************  UI  ***************************************************/

  case class ImportPathTexts(importPaths: List[String])
  case class ExportPath(exportPath: String)
  case class ExtensionsToImport(private val extensionsToImport: String) {
    def extensions = extensionsToImport.split(",").map(_.toLowerCase.trim).toSet
  }
  case class ExtensionsToConvert(private val extensionsToConvert: String) {
    def extensions = extensionsToConvert.split(",")
  }
  case class ExtensionsToResize(private val extensionsToResize: String) {
    def extensions = extensionsToResize.split(",")
  }
  case class FFMpegPath(ffmpegPath: String)
  case class FFProbePath(ffprobePath: String)
  case class ExifToolPath(exifToolPath: String)
  case class UseAlreadyImported(useAlreadyImported: Boolean)
  case class DoImport(doImport: Boolean)
  case class CutFrom(cutFrom: String)
  case class CutTo(cutTo: String)
  case class CropX(cropX: String) { def x = cropX.toInt }
  case class CropY(cropY: String) { def y = cropY.toInt }
  case class CropWidth(cropWidth: String) { def w = cropWidth.toInt }
  case class CropHeigth(cropHeight: String) { def h = cropHeight.toInt }

  private val TITLE = "Photo and video import"

  private val inProgress = BehaviorSubject(false)
  private val enabledState = inProgress.not

  private val importPathClicked = BehaviorSubject[Click]()
  private val importPaths = BehaviorSubject[ImportPathTexts]()

  private val exportPathClicked = BehaviorSubject[Click]()
  private val exportPath = BehaviorSubject[ExportPath]()

  private val extensionsToImport = BehaviorSubject[ExtensionsToImport]()
  private val extensionsToConvert = BehaviorSubject[ExtensionsToConvert]()
  private val extensionsToResize = BehaviorSubject[ExtensionsToResize]()
  private val ffmpegPath = BehaviorSubject[FFMpegPath]()
  private val ffprobePath = BehaviorSubject[FFProbePath]()
  private val exiftoolPath = BehaviorSubject[ExifToolPath]()
  private val useAlreadyImported = BehaviorSubject[UseAlreadyImported]()

  private val checkToImportClicked = BehaviorSubject[Click]()

  private val checkToImportLabel = BehaviorSubject[String]()

  private val doImport = BehaviorSubject[DoImport]()

  private val importClicked = BehaviorSubject[Click]()

  private val importLabel = BehaviorSubject[String]()

  private val progressBarFormat = BehaviorSubject[String]()
  private val progressBarValue = BehaviorSubject[Int]()
  private val progressBarFinished = BehaviorSubject[String]()

  private val filesToResize = BehaviorSubject[List[File]]()
  private val filesToRotate0 = BehaviorSubject[List[File]]()
  private val filesToRotate1 = BehaviorSubject[List[File]]()
  private val filesToRotate2 = BehaviorSubject[List[File]]()
  private val filesToRotate3 = BehaviorSubject[List[File]]()

  private val filesToCut = BehaviorSubject[List[File]]()
  private val cutFrom = BehaviorSubject[CutFrom]()
  private val cutTo = BehaviorSubject[CutTo]()
  private val filesToCrop = BehaviorSubject[List[File]]()
  private val cropX = BehaviorSubject[CropX]()
  private val cropY = BehaviorSubject[CropY]()
  private val cropWidth = BehaviorSubject[CropWidth]()
  private val cropHeight = BehaviorSubject[CropHeigth]()

  private val logArea = BehaviorSubject[String]()
  private val logAreaAppender = BehaviorSubject[String]()

  private val panel = JPanelBuilder().withBorders.withSeparators
    .newColumn.newColumnFPG

    .newRow("f:p").addLabel.text("Import path: ").cursor_HAND_CURSOR.onMouseClicked(importPathClicked)
    .nextColumn.addTextFieldMulti("importPathTextField", 30).setValues(List()).remember("importPathTextFields").onChanged(importPaths.pam(ImportPathTexts(_)))

    .newRow.addLabel.text("Export path: ").cursor_HAND_CURSOR.onMouseClicked(exportPathClicked)
    .nextColumn.addTextField.text("").onTextChanged(exportPath.pam(ExportPath(_))).remember("exportPath")

    .newRow.addLabel.text("Files to import: ")
    .nextColumn.addTextField.text("JPG,MTS,m2ts,mp4,jpeg").onTextChanged(extensionsToImport.pam(ExtensionsToImport(_))).remember("extensionsToImport")

    .newRow.addLabel.text("Files to convert: ")
    .nextColumn.addTextField.text("MTS,m2ts").onTextChanged(extensionsToConvert.pam(ExtensionsToConvert(_))).remember("extensionsToConvert")

    .newRow.addLabel.text("Files to resize: ")
    .nextColumn.addTextField.text("MTS,m2ts,mp4").onTextChanged(extensionsToResize.pam(ExtensionsToResize(_))).remember("extensionsToResize")

    .newRow.addLabel.text("ffmpeg.exe location: ")
    .nextColumn.addTextField.text("""C:\private\ffmpeg\bin\ffmpeg.exe""").onTextChanged(ffmpegPath.pam(FFMpegPath(_))).remember("ffmpeg")

    .newRow.addLabel.text("ffprobe.exe location: ")
    .nextColumn.addTextField.text("""C:\private\ffmpeg\bin\ffprobe.exe""").onTextChanged(ffprobePath.pam(FFProbePath(_))).remember("ffprobe")

    .newRow.addLabel.text("exiftool.exe location: ")
    .nextColumn.addTextField.text("""C:\private\exiftool.exe""").onTextChanged(exiftoolPath.pam(ExifToolPath(_))).remember("exiftool")

    .newRow.addCheckBox.text("Use already imported txt").onSelectionChanged(useAlreadyImported.pam(UseAlreadyImported(_))).remember("useAlreadyImported")

    .nextColumn.addButton.text("Check to import").onAction(checkToImportClicked).enabled(enabledState)
    .newRow.span.addLabel.text("").text(checkToImportLabel)

    .newRow.addCheckBox.text("Do Import").onSelectionChanged(doImport.pam(DoImport(_))).remember("doImport")

    .nextColumn.addButton.text("Import").onAction(importClicked).enabled(enabledState)
    .newRow.span.addLabel.text(importLabel)

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

    .newRow.addButton.text("Cut file").disabled.onDropFiles(filesToCut)
    .nextColumn.addPanelBuilder(_
      .withSeparators
      .addLabel.text("from: ")
      .newColumn.addTextField.text("00:00:00").size(10).onTextChanged(cutFrom.pam(CutFrom(_))).remember("cutFrom")
      .newColumn.addLabel.text("length: ")
      .newColumn.addTextField.text("00:00:00").size(10).onTextChanged(cutTo.pam(CutTo(_))).remember("cutTo"))

    .newRow.addButton.text("Crop file").disabled.onDropFiles(filesToCrop)
    .nextColumn.addPanelBuilder(_
      .withSeparators
      .addLabel.text("x: ")
      .newColumn.addTextField.text("").size(5).onTextChanged(cropX.pam(CropX(_))).remember("cropX")
      .newColumn.addLabel.text("y: ")
      .newColumn.addTextField.text("").size(5).onTextChanged(cropY.pam(CropY(_))).remember("cropY")
      .newColumn.addLabel.text("width: ")
      .newColumn.addTextField.text("").size(5).onTextChanged(cropWidth.pam(CropWidth(_))).remember("cropW")
      .newColumn.addLabel.text("height: ")
      .newColumn.addTextField.text("").size(5).onTextChanged(cropHeight.pam(CropHeigth(_))).remember("cropH"))

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

  case class CutParams(private val params: (CutFrom, CutTo)) {
    def from = params._1
    def to = params._2
  }
  case class CropParams(private val params: (CropX, CropY, CropWidth, CropHeigth)) {
    def x = params._1
    def y = params._2
    def width = params._3
    def heigth = params._4
  }
  case class Params(private val params: (ImportPathTexts, ExportPath, ExtensionsToImport, ExtensionsToConvert, ExtensionsToResize, FFMpegPath, FFProbePath, ExifToolPath, UseAlreadyImported, DoImport, CutParams, CropParams)) {
    def importPaths = params._1
    def exportPath = params._2
    def extensionsToImport = params._3
    def extensionsToConvert = params._4
    def extensionsToResize = params._5
    def ffmpegPath = params._6
    def ffprobePath = params._7
    def exiftoolPath = params._8
    def useAlreadyImported = params._9
    def doImport = params._10
    def cut = params._11
    def crop = params._12
  }

  val cutParams$ = ObservablePlus.combineLatest(cutFrom, cutTo).map(CutParams)
  val cropParams$ = ObservablePlus.combineLatest(cropX, cropY, cropWidth, cropHeight).map(CropParams)

  val params$ = ObservablePlus.combineLatest(
    importPaths, exportPath, extensionsToImport, extensionsToConvert, extensionsToResize, ffmpegPath, ffprobePath, exiftoolPath, useAlreadyImported, doImport, cutParams$, cropParams$).map(Params)

  importPaths.subscribe(v => frame.resizeAndBack) // this resizeAndBack should be handled better! in the multi class

  importPathClicked.takeLatestOf(importPaths).subscribe(_.importPaths.foreach(_.openAsFile))
  exportPathClicked.takeLatestOf(exportPath).subscribe(_.exportPath.openAsFile)

  checkToImportClicked.takeLatestOf(params$).subscribe(params => execute(checkFilesToImport(params)))
  importClicked.takeLatestOf(params$).subscribe(params => execute(importFiles(params)))

  filesToResize.withLatestOf(params$).subscribe(filesParams => execute(filesParams._1.foreach(resizeFile(filesParams._2))))

  def rotateFiles(rotate: Int)(filesFfmgep: (List[File], FFMpegPath)) = execute(filesFfmgep._1.foreach(rotateFile(filesFfmgep._2, rotate, l => {})))
  filesToRotate0.withLatestOf(ffmpegPath).subscribe(rotateFiles(0)(_))
  filesToRotate1.withLatestOf(ffmpegPath).subscribe(rotateFiles(1)(_))
  filesToRotate2.withLatestOf(ffmpegPath).subscribe(rotateFiles(2)(_))
  filesToRotate3.withLatestOf(ffmpegPath).subscribe(rotateFiles(3)(_))

  filesToCut.withLatestOf(params$).subscribe(filesParams => execute(filesParams._1.foreach(cutFile(filesParams._2))))
  filesToCrop.withLatestOf(params$).subscribe(filesParams => execute(filesParams._1.foreach(cropFile(filesParams._2))))

  //TODO refactor this is bad design. instead create an array and as long array is not empty then in progress....
  private def execute(action: => Unit) = { inProgress.onNext(true); SwingPlus.runInWorker(action, inProgress.onNext(false)) }

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

    val importResults = files.par.map(file => importFile(params, fileProgress(file))(file)).toList.sortBy(_._2)
    val importSuccessful = importResults.filter(_._1.isEmpty)
    val importFailed = importResults.filter(_._1.nonEmpty)

    if (importSuccessful.nonEmpty)
      log("Imported files:\n" + importSuccessful.mkString("\n") + ".")
    else
      log("Imported files: NOTHING!!!")

    if (importFailed.nonEmpty)
      log("NOT Imported files:\n" + importFailed.mkString("\n") + ".")
    else
      log("NOT Imported files: OK, no errors.")

    val newList = alreadyImportedFiles(params.exportPath.exportPath) ++ importSuccessful.map(_._2)
    newList.mkStringNL.writeToFileUtf8(alreadyImportedFile(params.exportPath.exportPath).toString)

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
          lazy val exifDT = exifDateTime(fileToImport, params.exiftoolPath.exifToolPath)

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

          val targetFileWithCorrectName = ((params.exportPath + "\\" + targetDate + " " + fileNameWithoutDate).trim + fileToImportExtension).asFile
          val targetFileMaybeDuplicate =
            if (isVideoToConvert(params.extensionsToConvert)(fileToImport)) (targetFileWithCorrectName.withoutExtension + ".mp4").asFile
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
            else if (params.doImport.doImport) {

              // Import File
              if (isVideoToConvert(params.extensionsToConvert)(fileToImport)) {
                log("Convert video " + fileToImport + " to " + targetFile)

                val res = runFFMpeg(
                  params.ffmpegPath.ffmpegPath,
                  fileToImport,
                  targetFile,
                  convertActualBytesToProgress,
                  VF(YADIF),
                  VCODEC_MPEG4,
                  BITRATE_VIDEO_17M,
                  AUDIO_CODEC_MP3,
                  BITRATE_AUDIO_192K)
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
    if (params.importPaths.importPaths.size < 1) { alert("Set at least one import dir please."); List() }
    else if (!params.importPaths.importPaths.map(_.asDir.existsAndDir).forall(d2 => d2)) { alert("One of the import dirs does not exists."); List() }
    else if (!params.exportPath.exportPath.asDir.existsAndDir) { alert("Export dir does not exists."); List() }
    else if (params.ffmpegPath.ffmpegPath.asFile.notExists) { alert("ffmpeg does not exists."); List() }
    else if (params.ffprobePath.ffprobePath.asFile.notExists) { alert("ffprobe does not exists."); List() }
    else if (params.exiftoolPath.exifToolPath.asFile.notExists) { alert("exiftool does not exists."); List() }
    else {

      val dirs = params.importPaths.importPaths
      val allFiles = dirs.flatMap(_.asDir.subFiles.toList)
      log("All files: " + allFiles.size)
      val allExtensions = allFiles.map(_.extension).distinct.map(_.toLowerCase.trim).map(_.substring(1)).toSet
      log("All extensions: " + allExtensions.mkString(", "))
      val extensionsToImport = params.extensionsToImport.extensions
      log("ExtensionsToImport: " + extensionsToImport.mkString(", "))
      log("NOT to import: " + allExtensions.diff(extensionsToImport).mkString(", "))
      val allFilesToImport = allFiles.filter(_.endsWith(extensionsToImport.toList: _*))
      log("Files to Import: " + allFilesToImport.size)

      Log.trace("allFiles\r\n" + allFilesToImport.mkStringNL)
      Log.trace("\r\nalreadyImportedFiles\r\n" + alreadyImportedFiles(params.exportPath.exportPath).mkStringNL)
      val filesToImport = if (params.useAlreadyImported.useAlreadyImported) {
        //        println("allFilesToImport", allFilesToImport)
        //        println("alreadyImportedFiles", alreadyImportedFiles(params.exportPathTextField))
        //        println("diff", allFilesToImport.diff(alreadyImportedFiles(params.exportPathTextField)))
        allFilesToImport.diff(alreadyImportedFiles(params.exportPath.exportPath))
      } else allFilesToImport
      Log.trace("filesToImport " + filesToImport.mkStringNL)
      val filesToImportSizeSum = filesToImport.map(_.length).sum

      checkToImportLabel.onNext(filesToImport.size + " files to import, " + (filesToImportSizeSum / 1024 / 1024) + "MB")
      Log.info("Checking files to import: " + filesToImport.size + " files.")
      if (filesToImport.nonEmpty) frame.state_Normal.visible.toFront

      filesToImport.sortBy(_.length).reverse
    }
  }

  private def log(txt: Any) = logAreaAppender.onNext(txt + "\n")

  private def writeEmail() =
    Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=Photo%20and%20video%20import&body=" + URLEncoder.encode(LogWindow.getAllLogs, "utf-8").replace("+", "%20")))

  private def isVideoToConvert(extensionsToConvert: ExtensionsToConvert)(file: File) = this.synchronized { file.endsWith(extensionsToConvert.extensions: _*) }

  private def isVideoToResize(extensionsToResize: ExtensionsToResize)(file: File) = file.endsWith(extensionsToResize.extensions: _*)
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

  private def cutFile(params: Params)(fileToCut: File): Unit = {
    val targetFileName = fileToCut.extendFileNameWith("_cut")
    log("Cut video " + fileToCut + " to " + targetFileName)
    val res = runFFMpeg(params.ffmpegPath.ffmpegPath, fileToCut, targetFileName, dontcare => {},
      CUT(params.cut.from.cutFrom, params.cut.to.cutTo),
      CODEC_AUDIO_COPY, CODEC_VIDEO_COPY)

    targetFileName.setLastModified(fileToCut.lastModified)
    log("Cut video result " + res)
  }
  private def cropFile(params: Params)(fileToCrop: File): Unit = {
    val targetFileName = fileToCrop.extendFileNameWith("_crop")
    log("Crop video " + fileToCrop + " to " + targetFileName)
    val res = runFFMpeg(params.ffmpegPath.ffmpegPath, fileToCrop, targetFileName, dontcare => {},
      CROP(params.crop.x.x, params.crop.y.y, params.crop.width.w, params.crop.heigth.h))
    targetFileName.setLastModified(fileToCrop.lastModified)
    log("Crop video result " + res)

  }
  private def resizeFile(params: Params)(fileToResize: File): Unit = {
    if (isVideoToResize(params.extensionsToResize)(fileToResize)) {
      val resizeRes = TryCatch({
        val resizeTargetFileName = fileToResize.addSubDir("resized")
        log("Resize video " + fileToResize + " to " + resizeTargetFileName)

        val res = resizeFile(720, params.ffmpegPath.ffmpegPath, params.ffprobePath.ffprobePath, fileToResize, resizeTargetFileName)
        resizeTargetFileName.setLastModified(fileToResize.lastModified)
        //        println("Importt " + fileToResize)
        //Files.setAttribute(resizeTargetFileName.toPath, "creationTime", FileTime.from(fileToImport.creationTime))
        //        println("Importtt")
        //                  println("---")
        //                  println("E " + res.exitValue)
        //                  println("Output " + res.output)
        //                  println("Erroe " + res.errorOutput)
        //                  println("---")
        log("Resize video result " + res)
        res
      }, (e: Throwable) => println("Resizing error: " + e.getMessage))

    } else if (isImageToResize("jpg,JPG,jpeg,JPEG")(fileToResize)) {
      val resizeRes = TryCatch({
        val resizeTargetFileName = fileToResize.addSubDir("resized")
        log("Resize image " + fileToResize + " to " + resizeTargetFileName)
        val res = runFFMpeg(params.ffmpegPath.ffmpegPath, fileToResize, resizeTargetFileName, /*FIXME*/ dontcare => {}, VF(SCALE_HEIGHT_NOOVERSIZE(1080)))
        resizeTargetFileName.setLastModified(fileToResize.lastModified)
        //Files.setAttribute(resizeTargetFileName.toPath, "creationTime", FileTime.from(fileToImport.creationTime))
        log("Resize image result " + res)
        res
      }, (e: Throwable) => println("Resizing error: " + e.getMessage))
    } else log("Not to resize " + fileToResize)
  }

  //private def resizeFile_(height: Int, ffmpegPath: String, ffprobePath: String)(inFile: File): Unit = resizeFile(height, ffmpegPath, ffprobePath, inFile, inFile.addSubDir("resized"))
  private def resizeFile(height: Int, ffmpegPath: String, ffprobePath: String, inFile: File, outFile: File): Unit = {
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
      log("Resize exit code: " + runFFMpeg(ffmpegPath, inFile, outFile, dontcare => {},
        VF(SCALE_HEIGHT(height), DESHAKE),
        CODEC_VIDEO_LIBX264,
        CONSTANT_RATE_FACTOR_VISUALLY_LOSSLESS,
        PRESET_MEDIUM,
        CODEC_AUDIO_COPY))
    }
    log("---Done---")
  }

  private def rotateFile(ffmpegPath: FFMpegPath, transpose: Int, bytesDoneProgress: Long => Unit)(inFile: File): Unit = {
    val out = inFile.extendFileNameWith("_r" + transpose.toString)
    log("Rotate " + transpose + " " + inFile + " " + out)
    log("Rotate exit code: " + runFFMpeg(ffmpegPath.ffmpegPath, inFile, out, bytesDoneProgress, VF(TRANSPOSE(transpose))))
  }

}
