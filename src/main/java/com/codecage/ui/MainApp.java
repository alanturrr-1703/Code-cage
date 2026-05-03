package com.codecage.ui;

import com.codecage.ui.controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    private MainController controller;

    @Override
    public void start(Stage stage) {
        controller = new MainController();
        var root = controller.buildRoot();

        Scene scene = new Scene(root, 1280, 820);

        // Load CSS
        try {
            String css = Objects.requireNonNull(
                    getClass().getResource("/com/codecage/css/dark-theme.css")
            ).toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("CSS not found: " + e.getMessage());
        }

        stage.setTitle("Code-cage  ⚙  Multiprocess Execution Platform");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        stage.setOnCloseRequest(e -> controller.shutdown());
        stage.show();
    }

    public static void launchApp(String[] args) {
        launch(args);
    }
}
