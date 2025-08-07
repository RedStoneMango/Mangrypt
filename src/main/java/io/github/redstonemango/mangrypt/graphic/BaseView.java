package io.github.redstonemango.mangrypt.graphic;

import io.github.redstonemango.mangrypt.graphic.controller.*;
import io.github.redstonemango.mangrypt.logic.ConfigIO;
import io.github.redstonemango.mangrypt.logic.Utilities;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Custom alert, scene, overlay manager to better integrate with the style of the enclosed application
 */
public class BaseView extends StackPane {

    private boolean isObscuringDialog = false;
    private boolean isObscuring2ndLayer = false;
    private Node obscuredFocusOwner = null;

    private final StackPane passwordOverlayLayer;
    private final FlowPane dialogLayer;
    private final AuthController passwordOverlayController;
    private final Pane baseImageBackground;
    private final Pane transitionLayer;

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
            getScene().getWindow().focusedProperty().addListener((_, _, isFocused) -> {
                if (!isFocused && ConfigIO.shouldSave() /* Is a vault opened? */) {
                    showPasswordDialog(true);
                }
            });
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

        transitionLayer = new Pane();
        transitionLayer.setStyle("-fx-background-color: black;");
        transitionLayer.setVisible(false);

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
        getChildren().add(transitionLayer);
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

    public Pane getSceneRoot() {
        return sceneRoot;
    }

    public <T extends Pane> T getSceneRootCasted(Class<T> clazz) {
        return clazz.cast(sceneRoot);
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
        return !sceneRoot.isVisible(); // SceneRoot will always be visible, except if it is obscured
    }

    public void showPasswordDialog(boolean obscure) {
        isObscuringDialog = dialogLayer.isVisible();
        isObscuring2ndLayer = secondLayerRoot.isVisible();
        if (obscure) {
            obscuredFocusOwner = getScene().getFocusOwner();
            dialogLayer.setVisible(false);
            sceneRoot.setVisible(false);
            secondLayerRoot.setVisible(false);
            baseImageBackground.setVisible(false);
        }
        else {
            passwordOverlayLayer.setOpacity(0);
            FadeTransition transition = new FadeTransition(Duration.millis(150), passwordOverlayLayer);
            transition.setFromValue(0);
            transition.setToValue(1);
            transition.play();
        }
        passwordOverlayController.prepare();
        passwordOverlayLayer.setVisible(true);
    }

    public void hidePasswordDialog() {
        Utilities.ensureAuthorizedAccess(AuthController.class);

        if (isObscuringDialog) dialogLayer.setVisible(true); // If the layer was not visible when obscuring, don't show it
        if (isObscuring2ndLayer) secondLayerRoot.setVisible(true);
        sceneRoot.setVisible(true);
        baseImageBackground.setVisible(true);
        passwordOverlayLayer.setVisible(false);
        if (obscuredFocusOwner != null) obscuredFocusOwner.requestFocus();
        obscuredFocusOwner = null;
    }

    public void playTransition(Runnable transitionAction) {
        FadeTransition outTransition = new FadeTransition(Duration.millis(150), transitionLayer);
        outTransition.setFromValue(0);
        outTransition.setToValue(1);
        outTransition.play();
        transitionLayer.setVisible(true);
        outTransition.setOnFinished(_ -> {
            transitionAction.run();

            FadeTransition inTransition = new FadeTransition(Duration.millis(150), transitionLayer);
            inTransition.setFromValue(1);
            inTransition.setToValue(0);
            inTransition.play();
            inTransition.setOnFinished(_ -> transitionLayer.setVisible(false));
        });
    }

    public void showConfirmationDialog(String title, String message, Runnable onConfirmed) {
        showAlert(Alert.AlertType.CONFIRMATION, title, message, true, btn -> {
            if (btn == ButtonType.YES) {
                onConfirmed.run();
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
        showInputDialog(header, hint, defaultText, cancelable, _ -> true, _ -> null, onAction);
    }
    public void showInputDialog(String header, String hint, String defaultText, boolean cancelable, Function<String, String> infoFunction, Consumer<String> onAction) {
        showInputDialog(header, hint, defaultText, cancelable, _ -> true, infoFunction, onAction);
    }

    public void showInputDialog(String header, String hint, String defaultText, boolean cancelable, Function<String, Boolean> allowFunction, Function<String, String> infoFunction, Consumer<String> onAction) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/input-dialog.fxml"));
        dialogLayer.getChildren().clear();
        try {
            dialogLayer.getChildren().add(loader.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ((InputDialogController) loader.getController()).init(header, hint, defaultText, cancelable, allowFunction, infoFunction, onAction);
        dialogLayer.setVisible(true);
        updateDimensions();
    }
}
