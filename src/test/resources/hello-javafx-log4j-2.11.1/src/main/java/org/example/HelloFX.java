package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;

public class HelloFX extends Application {

    @Override
    public void start(Stage stage) {
        StackPane pane = new StackPane(new Label("HelloFX"));
        Scene scene = new Scene(pane, 300, 200);
        stage.setScene(scene);
        stage.show();

        LogManager.getLogger().info("Hello, JavaFX!");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
