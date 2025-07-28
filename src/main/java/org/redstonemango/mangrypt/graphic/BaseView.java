package org.redstonemango.mangrypt.graphic;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.redstonemango.mangrypt.graphic.controller.AuthController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Custom alert, scene, overlay manager to better integrate with the style of the enclosed application
public class BaseView extends StackPane {

    private final StackPane passwordOverlayLayer;
    private final AuthController passwordOverlayController;

    private final List<Dialog<?>> visibleDialogs = new ArrayList<>();

    public BaseView(Parent sceneRoot) {
        getChildren().add(sceneRoot);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/redstonemango/mangrypt/fxml/overlay.fxml"));
            passwordOverlayLayer = loader.load();
            passwordOverlayLayer.setVisible(false);
            passwordOverlayController = loader.getController();
            getChildren().add(passwordOverlayLayer);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        widthProperty().addListener((_, _, width) -> getChildren().forEach(child -> {
                if (child instanceof Region r) r.setPrefWidth(width.doubleValue());
            }));
        heightProperty().addListener((_, _, height) -> getChildren().forEach(child -> {
            if (child instanceof Region r) r.setPrefHeight(height.doubleValue());
        }));
    }

    public void setSceneRoot(Parent sceneRoot) {
        getChildren().removeFirst();
        getChildren().addFirst(sceneRoot);
    }

    public void showPasswordOverlay(boolean show) {
        if (show) {
            passwordOverlayController.prepare();
        }
        passwordOverlayLayer.setVisible(show);
    }

    public void showInfoAlert(String process, String info) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Information about process '" + process + "':");
        alert.setContentText(info);
        showDialog(alert, false);
    }
    public void showErrorAlert(String process, String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Error during process '" + process + "':");
        alert.setContentText(error);
        showDialog(alert, false);
    }
    public <T> Optional<T> showDialog(Dialog<T> dialog, boolean wait) {
        visibleDialogs.add(dialog);
        dialog.setOnHidden(_ -> visibleDialogs.remove(dialog));
        if (wait) {
            dialog.showAndWait();
            return Optional.of(dialog.getResult());
        }
        else dialog.show();
        return Optional.empty();
    }

}
