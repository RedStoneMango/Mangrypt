package io.github.redstonemango.mangrypt;

import io.github.redstonemango.mangrypt.back.ConfigIO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import io.github.redstonemango.mangrypt.front.BaseView;

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
        stage.setMinWidth(780 + xDecoration);
        stage.setMinHeight(480 + yDecoration);

        stage.setOnCloseRequest(e -> {
            e.consume();
            base.savingRoutine(false, () -> {
                try {
                    ConfigIO.cleanup();
                }
                catch (Exception ex) {
                    // Fail silently
                    System.err.print("Error running cleanup: ");
                    ex.printStackTrace(System.err);
                }
                try {
                    base.shutdownMediaServer();
                }
                catch (Exception ex) {
                    // Fail silently
                    System.err.print("Error shutting down media server: ");
                    ex.printStackTrace(System.err);
                }
                Platform.exit();
            }, true);
        });

        if (!ConfigIO.getVaultDirectory().exists()) ConfigIO.getVaultDirectory().mkdirs();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/vault-selection.fxml"));
        base.setSceneRoot(loader.load());
    }

    @Override
    public void stop() {
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
