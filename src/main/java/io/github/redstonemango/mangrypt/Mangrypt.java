package io.github.redstonemango.mangrypt;

import io.github.redstonemango.mangrypt.logic.ConfigIO;
import io.github.redstonemango.mangrypt.logic.Configuration;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import io.github.redstonemango.mangrypt.graphic.BaseView;

public class Mangrypt extends Application {

    private static BaseView base;

    @Override
    public void start(Stage stage) {
        base = new BaseView();
        Scene scene = new Scene(base);
        stage.setScene(scene);
        stage.setTitle("Mangrypt");
        stage.show();
        double xDecoration = stage.getWidth() - scene.getWidth();
        double yDecoration = stage.getHeight() - scene.getHeight();
        stage.setMinWidth(620 + xDecoration);
        stage.setMinHeight(380 + yDecoration);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (ConfigIO.shouldSave()) ConfigIO.save();
            ConfigIO.cleanup(); // Try to unset all mutable configuration objects to minimize the risk of memory leaks
        }));

        ConfigIO.authenticateUserAndLoadConfig();
    }

    public static BaseView getBase() {
        return base;
    }

    public static void safetyShutdown() {
        base.getScene().getWindow().hide();
    }

    public static void main(String[] args) {
        launch();
    }
}
