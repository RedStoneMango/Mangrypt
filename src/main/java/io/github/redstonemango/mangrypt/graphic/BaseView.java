package io.github.redstonemango.mangrypt.graphic;

import io.github.redstonemango.mangrypt.graphic.controller.AlertController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import io.github.redstonemango.mangrypt.graphic.controller.AuthController;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Custom alert, scene, overlay manager to better integrate with the style of the enclosed application
 */
public class BaseView extends StackPane {

    private final StackPane passwordOverlayLayer;
    private final FlowPane dialogLayer;
    private final AuthController passwordOverlayController;

    public BaseView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/overlay.fxml"));
            passwordOverlayLayer = loader.load();
            passwordOverlayLayer.setVisible(false);
            passwordOverlayController = loader.getController();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        dialogLayer = new FlowPane();
        dialogLayer.setAlignment(Pos.CENTER);
        dialogLayer.setVisible(false);

        widthProperty().addListener((_, _, width) -> getChildren().forEach(child -> {
            if (child instanceof Region r) r.setPrefWidth(width.doubleValue());
        }));
        heightProperty().addListener((_, _, height) -> getChildren().forEach(child -> {
            if (child instanceof Region r) r.setPrefHeight(height.doubleValue());
        }));

        Pane secondLayerRoot = new Pane();
        secondLayerRoot.setVisible(false);

        getChildren().add(new Pane()); // Scene itself, unset by default
        getChildren().add(secondLayerRoot);
        getChildren().add(passwordOverlayLayer);
        getChildren().add(dialogLayer);
    }

    public void setSceneRoot(Parent sceneRoot) {
        getChildren().removeFirst();
        getChildren().addFirst(sceneRoot);
    }
    public void setSecondLayerRoot(@Nullable Parent layerRoot) {
        if (layerRoot == null) {
            getChildren().get(1).setVisible(false);
            return;
        }
        getChildren().remove(1);
        getChildren().add(1, layerRoot);
    }

    public void showPasswordOverlay(boolean show) {
        if (show) {
            passwordOverlayController.prepare();
            dialogLayer.setVisible(false); // Hide possible dialogs
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
