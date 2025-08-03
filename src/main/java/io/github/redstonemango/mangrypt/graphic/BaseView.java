package io.github.redstonemango.mangrypt.graphic;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.controller.AlertController;
import io.github.redstonemango.mangrypt.graphic.controller.InputDialogController;
import io.github.redstonemango.mangrypt.graphic.controller.SecuritySetupController;
import io.github.redstonemango.mangrypt.logic.Configuration;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import io.github.redstonemango.mangrypt.graphic.controller.AuthController;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Custom alert, scene, overlay manager to better integrate with the style of the enclosed application
 */
public class BaseView extends StackPane {

    private boolean isObscuringDialog = false;

    private final StackPane passwordOverlayLayer;
    private final FlowPane dialogLayer;
    private final AuthController passwordOverlayController;
    private final Pane baseImageBackground;

    private final DefaultPaneBackground defaultPaneBackground;
    private final MatrixBackground matrixBackground;

    private Pane sceneRoot;
    private Pane secondLayerRoot;

    public BaseView() {
        getStylesheets().add(getClass().getResource("/io/github/redstonemango/mangrypt/style/application.css").toExternalForm());

        Pane matrixContainer = new Pane();
        matrixBackground = new MatrixBackground(matrixContainer);

        baseImageBackground = new Pane();
        defaultPaneBackground = new DefaultPaneBackground(baseImageBackground);

        Platform.runLater(() -> {
            matrixBackground.update();
            matrixBackground.playScroll();
            defaultPaneBackground.update();
            updateDimensions();
        });

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

        widthProperty().addListener((_, _, width) -> {
            matrixBackground.update();
            defaultPaneBackground.update();
            updateDimensions();
            if (!dialogLayer.getChildren().isEmpty()) ((Region) dialogLayer.getChildren().getFirst()).setPrefWidth(width.doubleValue());
        });
        heightProperty().addListener((_, _, height) -> {
            matrixBackground.update();
            defaultPaneBackground.update();
            updateDimensions();
            if (!dialogLayer.getChildren().isEmpty()) ((Region) dialogLayer.getChildren().getFirst()).setPrefHeight(height.doubleValue());
        });

        sceneRoot = new Pane();
        StackPane.setAlignment(sceneRoot, Pos.TOP_LEFT);

        secondLayerRoot = new Pane();
        secondLayerRoot.setVisible(false);

        getChildren().add(matrixContainer);
        getChildren().add(baseImageBackground);
        getChildren().add(sceneRoot);
        getChildren().add(secondLayerRoot);
        getChildren().add(passwordOverlayLayer);
        getChildren().add(dialogLayer);
    }

    private void updateDimensions() {
        // Matrix
        matrixBackground.update();

        // Layers
        secondLayerRoot.setPrefWidth(getWidth());
        secondLayerRoot.setPrefHeight(getHeight());
        passwordOverlayLayer.setPrefWidth(getWidth());
        passwordOverlayLayer.setPrefHeight(getHeight());
        if (!dialogLayer.getChildren().isEmpty() && dialogLayer.getChildren().getFirst() instanceof Region region) {
            region.setPrefWidth(getWidth());
            region.setPrefHeight(getHeight());
        }

        // Scene root
        defaultPaneBackground.update();
        Rectangle area = defaultPaneBackground.getContentArea();
        sceneRoot.setTranslateX(area.getX());
        sceneRoot.setTranslateY(area.getY());
        sceneRoot.setPrefWidth(area.getWidth());
        sceneRoot.setMaxWidth(area.getWidth());
        sceneRoot.setPrefHeight(area.getHeight());
        sceneRoot.setMaxHeight(area.getHeight());
    }

    public void setSceneRoot(Pane sceneRoot) {
        setSceneRoot(sceneRoot, true);
    }

    public void setSceneRoot(Pane sceneRoot, boolean background) {
        getChildren().remove(this.sceneRoot);
        StackPane.setAlignment(sceneRoot, Pos.TOP_LEFT);
        getChildren().add(2, sceneRoot);
        this.sceneRoot = sceneRoot;
        baseImageBackground.setVisible(background);
        if (background) updateDimensions();
    }
    public void setSecondLayerRoot(@Nullable Pane layerRoot) {
        if (layerRoot == null) {
            secondLayerRoot.setVisible(false);
            return;
        }
        getChildren().remove(secondLayerRoot);
        getChildren().add(3, layerRoot);
        secondLayerRoot = layerRoot;
        updateDimensions();
    }
    public boolean isObscuring() {
        return !sceneRoot.isVisible();
    }

    public void showPasswordDialog(boolean obscure) {
        isObscuringDialog = dialogLayer.isVisible();
        if (obscure) {
            dialogLayer.setVisible(false);
            sceneRoot.setVisible(false);
            secondLayerRoot.setVisible(false);
        }
        else {
            if (isObscuringDialog) dialogLayer.setVisible(true); // If the layer was not visible when obscuring, don't show it
            sceneRoot.setVisible(true);
            secondLayerRoot.setVisible(true);
        }
        passwordOverlayController.prepare();
        passwordOverlayLayer.setVisible(true);
    }

    public void hidePasswordDialog() {
        boolean trustedCaller = Configuration.WALKER.walk(frames ->
                frames.skip(1).anyMatch(frame -> {
                    Class<?> caller = frame.getDeclaringClass();
                    return (caller.equals(AuthController.class)
                            && caller.getClassLoader().equals(AuthController.class.getClassLoader()));
                })
        );
        if (!trustedCaller) {
            throw new SecurityException("Unauthorized (reflected?) access to hidePasswordDialog()");
        }
        passwordOverlayLayer.setVisible(false);
    }

    public void showConfirmationDialog(String title, String message, Runnable onSelected) {
        showAlert(Alert.AlertType.CONFIRMATION, title, message, true, btn -> {
            if (btn == ButtonType.YES) {
                onSelected.run();
            }
        }, ButtonType.NO, ButtonType.YES);
    }
    public void showInfoAlert(String info) {
        showAlert(Alert.AlertType.INFORMATION, "Information:", info, true, _ -> {}, ButtonType.OK);
    }
    public void showWarningAlert(String warning) {
        showAlert(Alert.AlertType.WARNING, "Something unexpected happened:", warning, true, _ -> {}, ButtonType.OK);
    }
    public void showErrorAlert(String error) {
        showAlert(Alert.AlertType.ERROR, "An error occurred:", error, true, _ -> {}, ButtonType.OK);
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
        updateDimensions();
    }

    public void showInputDialog(String header, String hint, String defaultText, boolean cancelable, Consumer<String> onAction) {
        showInputDialog(header, hint, defaultText, cancelable, _ -> true, onAction);
    }

    public void showInputDialog(String header, String hint, String defaultText, boolean cancelable, Function<String, Boolean> allowFunction, Consumer<String> onAction) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/input-dialog.fxml"));
        dialogLayer.getChildren().clear();
        try {
            dialogLayer.getChildren().add(loader.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ((InputDialogController) loader.getController()).init(header, hint, defaultText, cancelable, allowFunction, onAction);
        dialogLayer.setVisible(true);
        updateDimensions();
    }

}
