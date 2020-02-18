package eu.eyan.pvtools;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class VideTest extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws MalformedURLException {

        StackPane root = new StackPane();

        String path = "C:\\Users\\NemAdmin\\Desktop\\Képek\\_DROPBOXBA\\2019 08\\20190824_094531 00083.mp4";
        System.out.println(path);
        System.out.println(new File(path).exists());

        Class<? extends VideTest> aClass = getClass();
        URL resource = new File(path).toURI().toURL();//aClass.getResource(path);
        String source = resource.toExternalForm();
        Media media = new Media(source);
        MediaPlayer player = new MediaPlayer(media);
        MediaView mediaView = new MediaView(player);

        root.getChildren().add( mediaView);

        Scene scene = new Scene(root, 1024, 768);

        primaryStage.setScene(scene);
        primaryStage.show();


        player.play();

    }

}