package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class HelloFXJep493 extends Application {

    @Override
    public void start(Stage stage) {
        StackPane pane = new StackPane(new Label("HelloFXJpe493"));
        Scene scene = new Scene(pane, 300, 200);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
