package io.github.redstonemango.mangrypt.graphic;

import io.github.redstonemango.mangrypt.graphic.controller.AlertController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import io.github.redstonemango.mangrypt.graphic.controller.AuthController;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Custom alert, scene, overlay manager to better integrate with the style of the enclosed application
 */
public class BaseView extends StackPane {

    private final StackPane passwordOverlayLayer;
    private final FlowPane dialogLayer;
    private final AuthController passwordOverlayController;

    public BaseView(Parent sceneRoot) {
        getChildren().add(sceneRoot);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/overlay.fxml"));
            passwordOverlayLayer = loader.load();
            passwordOverlayLayer.setVisible(false);
            passwordOverlayController = loader.getController();
            getChildren().add(passwordOverlayLayer);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        dialogLayer = new FlowPane();
        dialogLayer.setAlignment(Pos.CENTER);
        getChildren().add(dialogLayer);

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
            dialogLayer.setVisible(false);
        }
        passwordOverlayLayer.setVisible(show);
    }

    public void showInfoAlert(String info) {
        showAlert(Alert.AlertType.INFORMATION, "Information:", info, true, _ -> {});
    }
    public void showWarningAlert(String warning) {
        showAlert(Alert.AlertType.WARNING, "Something unexpected happened:", warning, true, _ -> {});
    }
    public void showErrorAlert(String error) {
        showAlert(Alert.AlertType.ERROR, "An error occurred:", error, true, _ -> {});
    }

    public void showAlert(Alert.AlertType type, String header, String content, boolean cancelable, Consumer<ButtonType> onAction, ButtonType... buttons) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/alert.fxml"));
        dialogLayer.getChildren().clear();
        try {
            dialogLayer.getChildren().add(loader.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ((AlertController) loader.getController()).init(type, header, content, cancelable, onAction, buttons);
        dialogLayer.setVisible(true);
    }

}
