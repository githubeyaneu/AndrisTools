package eu.eyan.pvtools

import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.stage.Stage


class VideoTest extends Application {

  private val MEDIA_URL = "http://download.oracle.com/otndocs/products/javafx/oow2010-2.flv" // """C:\Users\NemAdmin\Desktop\Képek\_DROPBOXBA\2019 08\20190824_094531 00083.mp4"""//

  import javafx.scene.Scene
  import javafx.stage.Stage

  import javafx.application.Application
  import javafx.scene.Scene
  import javafx.scene.layout.StackPane
  import javafx.scene.media.MediaView
  import javafx.stage.Stage

  def main(args: Array[String]): Unit = {
    Application.launch(args:_*)
  }

  def start(primaryStage: Stage): Unit = {
    val root = new StackPane
    val player = new MediaPlayer( new Media(getClass().getResource("""C:\Users\NemAdmin\Desktop\Képek\_DROPBOXBA\2019 08\20190824_094531 00083.mp4""").toExternalForm()))
    val mediaView = new MediaView(player)
    root.getChildren.add(mediaView)
    val scene = new Scene(root, 1024, 768)
    primaryStage.setScene(scene)
    primaryStage.show()
    player.play
  }


}
