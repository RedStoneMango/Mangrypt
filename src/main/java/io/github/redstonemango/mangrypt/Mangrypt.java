package io.github.redstonemango.mangrypt;

import io.github.redstonemango.mangrypt.logic.ConfigIO;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import io.github.redstonemango.mangrypt.graphic.BaseView;

import java.io.IOException;

public class Mangrypt extends Application {

    private static BaseView base;

    @Override
    public void start(Stage stage) throws IOException {
        base = new BaseView();
        Scene scene = new Scene(base);
        stage.setScene(scene);
        stage.setTitle("Mangrypt");
        stage.show();
        double xDecoration = stage.getWidth() - scene.getWidth();
        double yDecoration = stage.getHeight() - scene.getHeight();
        stage.setMinWidth(720 + xDecoration);
        stage.setMinHeight(480 + yDecoration);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (ConfigIO.shouldSave()) ConfigIO.save();
            ConfigIO.cleanup(); // Try to unset all mutable objects to minimize the risk of memory leaks
        }));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/vault-selection.fxml"));
        base.setSceneRoot(loader.load());
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
