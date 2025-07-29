package io.github.redstonemango.mangrypt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import io.github.redstonemango.mangrypt.graphic.BaseView;

import java.io.IOException;

public class Mangrypt extends Application {

    private static BaseView base;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/passphrase-input.fxml"));
        base = new BaseView(loader.load());
        Scene scene = new Scene(base);
        base.showAlert(Alert.AlertType.ERROR, "Test", "Hello", true, System.out::println);
        stage.setScene(scene);
        stage.setTitle("Mangrypt");
        stage.show();
        double xDecoration = stage.getWidth() - scene.getWidth();
        double yDecoration = stage.getHeight() - scene.getHeight();
        stage.setMinWidth(620 + xDecoration);
        stage.setMinHeight(380 + yDecoration);
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
