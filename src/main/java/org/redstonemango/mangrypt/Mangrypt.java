package org.redstonemango.mangrypt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.redstonemango.mangrypt.graphic.BaseView;
import org.redstonemango.mangrypt.logic.Configuration;
import org.redstonemango.mangrypt.logic.CypherEncryption;

import java.io.IOException;

public class Mangrypt extends Application {

    private static BaseView base;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/redstonemango/mangrypt/fxml/passphrase-input.fxml"));
        base = new BaseView(loader.load());
        Scene scene = new Scene(base);
        stage.setScene(scene);
        stage.setTitle("Mangrypt");
        stage.show();
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
