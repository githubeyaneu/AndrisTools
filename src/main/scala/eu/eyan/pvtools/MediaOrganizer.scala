package eu.eyan.pvtools

import java.awt.RenderingHints.Key
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.awt.{Color, Desktop, Graphics}
import java.io.File
import java.net.{URI, URLEncoder}
import java.nio.file.Files
import java.nio.file.attribute.FileTime

import eu.eyan.log.{Log, LogWindow}
import eu.eyan.util.awt.clipboard.ClipboardPlus
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import eu.eyan.util.java.time.InstantPlus.InstantImplicit
import eu.eyan.util.registry.RegistryPlus
import eu.eyan.util.rx.lang.scala.ObservablePlus
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus
import eu.eyan.util.scala.collection.TraversableOncePlus.TraversableOnceImplicit
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.swing.JButtonPlus.JButtonImplicit
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JLabelPlus.JLabelImplicit
import eu.eyan.util.swing.JPanelWithFrameLayout
import eu.eyan.util.swing.JTextFieldPlus.JTextFieldPlusImplicit
import eu.eyan.util.swing.JToggleButtonPlus.JToggleButtonImplicit
import javafx.scene.Scene
import javafx.scene.media.{Media, MediaPlayer, MediaView}
import javax.imageio.ImageIO
import javax.swing.{JFrame, JOptionPane, JPanel, JScrollPane}
import org.fest.assertions.Delta
import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

import scala.io.Codec

object MediaOrganizer extends App {
  var image:BufferedImage = _

  Log.activateInfoLevel
  LogWindow.redirectSystemOutAndErrToLogWindow

  /*                    UI                    */

  val TITLE = "Photo organizer"

  val panel = new JPanelWithFrameLayout().withBorders.withSeparators.newColumn.newColumn("f:1px:g")

  panel.newRow("f:p").addLabel("Path: ")
  val importPathTextField = panel.nextColumn.addLabel("""C:\Users\NemAdmin\Desktop\Képek\_DROPBOXBA\2019 08""")//.rememberValueInRegistry("importPathTextField")
  val path = BehaviorSubject("""C:\Users\NemAdmin\Desktop\Képek\_DROPBOXBA\2019 08""")
  //importPathTextField.onTextChanged(path)

  panel.newRow.addLabel("Selected file: ")
  val selectedFileLabel = panel.nextColumn.addLabel("")

  val imagePanel = new JPanel(){
    override def paintComponent(g: Graphics): Unit = {
      g.clearRect(0,0,getWidth, getHeight)
      if (image != null) {
        val imageWidth = image.getWidth(this).toFloat
        val imageHeight = image.getHeight(this).toFloat
        val stand = imageHeight>imageWidth
       // val ratio = imageHeight.toFloat/imageWidth.toFloat
        val resize = if(stand) getHeight/imageHeight else getWidth/imageWidth

        val left = if(stand) (getWidth-imageWidth*resize)/2f else 0f
        val top = if(!stand) (getHeight-imageHeight*resize)/2f else 0f
        val width = if(stand) (imageWidth*resize).toInt else getWidth
        val height = if(!stand) (imageHeight*resize).toInt else getHeight

        println(s"$imageWidth x $imageHeight, $stand, $resize, panel: $getWidth x $getHeight, left: $left, top: $top, w: $width, h:$height")

          g.drawImage(image, left.toInt, top.toInt, width.toInt, height.toInt, this)
        }
      }
    }


  panel.newRow("f:1px:g").span.addFluent(imagePanel)

  val videoPanel = new JPanel()
  panel.newRow("f:1px:g").span.addFluent(videoPanel)

  val frame =
    new JFrame()
      .title(TITLE)
      .onCloseHide
      .iconFromChar('O', Color.CYAN)
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
      .maximize

  frame.onKeyPressedEvent(e=> keyPressed.onNext(e.getKeyCode))

  /*                    LOGIC                    */

  case class FileListAndState(files:List[File], selectedIndex: Int){
    def selectDelta(delta: Int) = {
      val newIndexCandidate = selectedIndex+delta
      val newIndex = if (newIndexCandidate<0) 0
      else if(newIndexCandidate>=files.size) files.size-1
      else newIndexCandidate
      FileListAndState(files, newIndex)
    }

    def selectedNr = selectedIndex+1
    def selectedFile= files(selectedIndex)
  }

  val state = BehaviorSubject[Option[FileListAndState]](None)
  val keyPressed = BehaviorSubject[Int]
  val numberOfFiles = state.map(_.map(_.files.size))

  state.subscribe(println(_))
  numberOfFiles.subscribe(println(_))
  keyPressed.subscribe(println(_))

  val next = keyPressed.filter(_ == KeyEvent.VK_RIGHT).map(x => 1)
  val prev = keyPressed.filter(_ == KeyEvent.VK_LEFT).map(x => -1)
  val pageDwn = keyPressed.filter(_ == KeyEvent.VK_PAGE_DOWN).map(x => 10)
  val pageUp = keyPressed.filter(_ == KeyEvent.VK_PAGE_UP).map(x => -10)
  next.merge(prev).merge(pageDwn).merge(pageUp).subscribe( delta =>{
    println(delta)
    state.take(1).subscribe(stateOptOld => state.onNext(stateOptOld.map(stateOld => stateOld.selectDelta(delta))))
  }
  )



  path.subscribe(path => if(path.asFile.existsAndDir){
    state.onNext(Some(FileListAndState(path.asFile.listFiles.filter(_.isFile).toList,0)))
  } else state.onNext(None))

  state.map(_.map(state => state.selectedNr +"/" +state.files.size+" "+state.selectedFile.getName ))
  .subscribe( t => selectedFileLabel.setText(t.getOrElse("")))

  def showImage(image: BufferedImage) = {this.image = image; imagePanel.repaint()}

  def showMedia(stateOpt: Option[FileListAndState]) = {
    if(stateOpt.isEmpty) clearMedia
    else {
      val state = stateOpt.get
      if(state.selectedFile.endsWith("jpg")){
        showImage(ImageIO.read(state.selectedFile))
      }
      else clearMedia
    }
  }

  def clearMedia = image = null


  state.subscribe(showMedia(_))



  def writeEmail() =
    Desktop.getDesktop.mail(new URI("mailto:PVTools@eyan.eu?subject=Photo%20organizer&body=" + URLEncoder.encode(LogWindow.getAllLogs, "utf-8").replace("+", "%20")))

  def alert(msg: String) = JOptionPane.showMessageDialog(null, msg)

  val ppath = """C:\Users\NemAdmin\Desktop\Képek\_DROPBOXBA\2019 08\20190824_094531 00083.mp4"""

  import javafx.beans.binding.Bindings
  import javafx.beans.property.DoubleProperty
  import javafx.embed.swing.JFXPanel
  import javafx.scene.Scene
  import javafx.scene.layout.StackPane
  import javafx.scene.media.MediaView
  import javafx.stage.Screen
  import java.awt.BorderLayout

  getVideo()

  def getVideo()={
    val VFXPanel = new JFXPanel()
    val video_source = new File(ppath)
    val m = new Media(video_source.toURI.toString)
    val  player = new MediaPlayer(m)
    val  viewer = new MediaView(player)

    val  root = new StackPane()
    val  scene = new Scene(root)

    // center video position
    val  screen = Screen.getPrimary.getVisualBounds
    println(videoPanel.getWidth, videoPanel.getHeight)
    viewer.setX((screen.getWidth - videoPanel.getWidth) / 2)
    viewer.setY((screen.getHeight - videoPanel.getHeight) / 2)

    // resize video based on screen size
    val  width = viewer.fitWidthProperty()
    val  height = viewer.fitHeightProperty()
    width.bind(Bindings.selectDouble(viewer.sceneProperty(), "width"))
    height.bind(Bindings.selectDouble(viewer.sceneProperty(), "height"))
    viewer.setPreserveRatio(true)

    // add video to stackpane
    root.getChildren.add(viewer)

    VFXPanel.setScene(scene)

    videoPanel.setLayout(new BorderLayout())
    videoPanel.add(VFXPanel, BorderLayout.CENTER)

    player.play()
  }

}