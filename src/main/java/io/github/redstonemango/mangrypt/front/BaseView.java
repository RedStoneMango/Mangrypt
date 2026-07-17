package io.github.redstonemango.mangrypt.front;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.ConfigIO;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.back.dataTypes.DataElement;
import io.github.redstonemango.mangrypt.front.controller.*;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Custom alert, scene, overlay manager to better integrate with the style of the enclosed application
 */
public class BaseView extends StackPane {

    private boolean isObscuringDialog = false;
    private boolean isObscuring2ndLayer = false;
    private boolean isObscuringDataLayer = false;
    private Node obscuredFocusOwner = null;

    private final StackPane passwordOverlayLayer;
    private final FlowPane dialogLayer;
    private final Pane baseImageBackground;
    private final Pane transitionLayer;
    private final DataView dataViewLayer;
    private final StackPane encryptionWaitLayer;

    private final EncryptionWaitController encryptionWaitController;

    private final DefaultPaneBackground defaultPaneBackground;
    private final MatrixBackground matrixBackground;

    private Pane sceneRoot;
    private Pane secondLayerRoot;

    private final Set<Node> focusForbiddenNodes = new HashSet<>();
    private final AtomicBoolean isSaving = new AtomicBoolean(false);


    public BaseView() {
        getStylesheets().add(getClass().getResource("/io/github/redstonemango/mangrypt/style/application.css").toExternalForm());

        Pane matrixContainer = new Pane();
        matrixBackground = new MatrixBackground(matrixContainer);

        baseImageBackground = new Pane();
        defaultPaneBackground = new DefaultPaneBackground(baseImageBackground);

        Platform.runLater(() -> {
            matrixBackground.update();
            defaultPaneBackground.update();
            updateDimensions();
            getScene().getWindow().focusedProperty().addListener((_, _, isFocused) -> {
                if (!isFocused && ConfigIO.isVaultOpen()) {
                    obscureData();
                }
            });
        });

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/overlay.fxml"));
            passwordOverlayLayer = loader.load();
            passwordOverlayLayer.setVisible(false);
            OverlayController oc = loader.getController();
            passwordOverlayLayer.visibleProperty().addListener((_, _, visible) -> {
               checkAllowSceneRootFocus();
               if (visible) {
                   oc.prepare();
               }
            });
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/encryption-wait.fxml"));
            encryptionWaitLayer = loader.load();
            encryptionWaitLayer.setVisible(false);
            encryptionWaitLayer.visibleProperty().addListener((_, _, _) ->
                    checkAllowSceneRootFocus());
            encryptionWaitController = loader.getController();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        dialogLayer = new FlowPane();
        dialogLayer.setAlignment(Pos.CENTER);
        dialogLayer.setVisible(false);
        dialogLayer.visibleProperty().addListener((_, _, _) ->
                checkAllowSceneRootFocus());

        widthProperty().addListener((_, _, width) -> {
            matrixBackground.update();
            defaultPaneBackground.update();
            updateDimensions();
            if (!dialogLayer.getChildren().isEmpty())
                ((Region) dialogLayer.getChildren().getFirst()).setPrefWidth(width.doubleValue());
        });
        heightProperty().addListener((_, _, height) -> {
            matrixBackground.update();
            defaultPaneBackground.update();
            updateDimensions();
            if (!dialogLayer.getChildren().isEmpty())
                ((Region) dialogLayer.getChildren().getFirst()).setPrefHeight(height.doubleValue());
        });

        transitionLayer = new Pane();
        transitionLayer.setStyle("-fx-background-color: black;");
        transitionLayer.setVisible(false);
        transitionLayer.visibleProperty().addListener((_, _, _) ->
                checkAllowSceneRootFocus());

        sceneRoot = new Pane();
        StackPane.setAlignment(sceneRoot, Pos.TOP_LEFT);

        secondLayerRoot = new Pane();
        secondLayerRoot.setVisible(false);
        secondLayerRoot.visibleProperty().addListener((_, _, _) ->
                checkAllowSceneRootFocus());

        dataViewLayer = new DataView();
        dataViewLayer.setVisible(false);
        dataViewLayer.visibleProperty().addListener((_, _, _) ->
                checkAllowSceneRootFocus());

        getChildren().add(matrixContainer);
        getChildren().add(baseImageBackground);
        getChildren().add(sceneRoot);
        getChildren().add(dataViewLayer);
        getChildren().add(secondLayerRoot);
        getChildren().add(passwordOverlayLayer);
        getChildren().add(dialogLayer);
        getChildren().add(transitionLayer);
        getChildren().add(encryptionWaitLayer);
    }

    private void updateDimensions() {
        // Matrix
        matrixBackground.update();

        // Layers
        secondLayerRoot.setPrefWidth(getWidth());
        secondLayerRoot.setPrefHeight(getHeight());
        passwordOverlayLayer.setPrefWidth(getWidth());
        passwordOverlayLayer.setPrefHeight(getHeight());
        dataViewLayer.setPrefWidth(getWidth());
        dataViewLayer.setPrefHeight(getHeight());
        encryptionWaitLayer.setPrefWidth(getWidth());
        encryptionWaitLayer.setPrefHeight(getHeight());
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

    public void checkAllowSceneRootFocus() {
        if (activeOverlayCount() == 0) {
            focusForbiddenNodes.forEach(n -> n.setFocusTraversable(true));
            focusForbiddenNodes.clear();
        }
        else {
            focusForbiddenNodes.addAll(
                    sceneRoot.lookupAll("*").stream()
                            .filter(Node::isFocusTraversable)
                            .collect(Collectors.toSet())
            );

            focusForbiddenNodes.forEach(n -> n.setFocusTraversable(false));
        }
    }

    public int activeOverlayCount() {
        int count = 0;
        if (dataViewLayer.isVisible()) count++;
        if (transitionLayer.isVisible()) count++;
        if (dialogLayer.isVisible()) count++;
        if (passwordOverlayLayer.isVisible()) count++;
        if (secondLayerRoot.isVisible()) count++;
        if (encryptionWaitLayer.isVisible()) count++;
        return count;
    }

    public void showData(List<DataElement> availableData, DataElement data) {
        dataViewLayer.showData(availableData, data);
        if (!dataViewLayer.isVisible()) dataViewLayer.setVisible(true);
    }

    public void shutdownMediaServer() {
        Utilities.ensureAuthorizedAccess(Mangrypt.class);
        dataViewLayer.shutdownMediaServer();
    }

    public void storeShowingData() {
        Utilities.ensureAuthorizedAccess(ConfigIO.class);
        dataViewLayer.cleanup();
    }

    public Pane getSceneRoot() {
        return sceneRoot;
    }

    public <T extends Pane> T getSceneRootCasted(Class<T> clazz) {
        return clazz.cast(sceneRoot);
    }

    public Pane getSecondLayerRoot() {
        return secondLayerRoot;
    }

    public <T extends Pane> T getSecondLayerRootCasted(Class<T> clazz) {
        return clazz.cast(secondLayerRoot);
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
            checkAllowSceneRootFocus();
            return;
        }
        getChildren().remove(secondLayerRoot);
        getChildren().add(3, layerRoot);
        secondLayerRoot = layerRoot;
        checkAllowSceneRootFocus();
        updateDimensions();
    }
    public boolean isObscuring() {
        return !sceneRoot.isVisible(); // SceneRoot will always be visible, except if it is obscured
    }

    public boolean setMatrixScroll(boolean matrixScroll) {
        if (matrixScroll) {
            return matrixBackground.playScroll();
        }
        else {
            return matrixBackground.stopScroll();
        }
    }

    private void obscureData() {
        Utilities.ensureAuthorizedAccess(BaseView.class);

        if (passwordOverlayLayer.isVisible()) return;
        setMatrixScroll(true);

        isObscuringDialog = dialogLayer.isVisible();
        isObscuring2ndLayer = secondLayerRoot.isVisible();
        isObscuringDataLayer = dataViewLayer.isVisible();
        obscuredFocusOwner = getScene().getFocusOwner();
        dialogLayer.setVisible(false);
        sceneRoot.setVisible(false);
        secondLayerRoot.setVisible(false);
        dataViewLayer.setVisible(false);
        baseImageBackground.setVisible(false);

        passwordOverlayLayer.setVisible(true);
    }

    public void stopObscuring() {
        Utilities.ensureAuthorizedAccess(OverlayController.class);

        setMatrixScroll(false);
        if (isObscuringDialog) dialogLayer.setVisible(true); // If the layer was not visible before obscuring, don't show it
        if (isObscuring2ndLayer) secondLayerRoot.setVisible(true);
        if (isObscuringDataLayer) dataViewLayer.setVisible(true);
        sceneRoot.setVisible(true);
        baseImageBackground.setVisible(true);
        passwordOverlayLayer.setVisible(false);
        if (obscuredFocusOwner != null) obscuredFocusOwner.requestFocus();
        obscuredFocusOwner = null;
    }

    public void savingRoutine() {
        savingRoutine(this::closeVaultUi, false, false);
    }

    public synchronized void savingRoutine(Runnable postSaveAction, boolean beforeExit, boolean runAlways) {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, OverlayController.class, Mangrypt.class);

        if (!ConfigIO.shouldSave() && !runAlways) {
            postSaveAction.run();
            return;
        }

        if (isSaving.get()) return; // If we are already saving, do not run again
        isSaving.set(true);

        encryptionWaitController.init(true, beforeExit);
        encryptionWaitLayer.setVisible(true);

        CompletableFuture<Boolean> saveFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return ConfigIO.save();
            } catch (Exception e) {
                System.err.print("Error saving: ");
                e.printStackTrace(System.err);
                return false;
            }
        });

        saveFuture.whenComplete((success, ex) -> {
            Platform.runLater(() -> {
                encryptionWaitLayer.setVisible(false);
                isSaving.set(false);

                if (ex != null || !success) {
                    Mangrypt.getBase().showAlert(
                            Alert.AlertType.ERROR,
                            "Save Error",
                            "Mangrypt was unable to save your vault. Do you still want to close the it (data may be lost)",
                            true,
                            btn -> {
                                if (btn == ButtonType.YES) {
                                    postSaveAction.run();
                                }
                            },
                            ButtonType.YES, ButtonType.NO
                    );
                }
                else {
                    postSaveAction.run();
                }
            });
        });
    }

    public void decryptionWaitingScreen(boolean show) {
        Utilities.ensureAuthorizedAccess(AuthenticationController.class);
        encryptionWaitController.init(false, false);
        encryptionWaitLayer.setVisible(show);
    }

    private void closeVaultUi() {
        Utilities.ensureAuthorizedAccess(BaseView.class);

        ConfigIO.cleanup();

        try {
            FXMLLoader loader = new FXMLLoader(Utilities.class.getResource("/io/github/redstonemango/mangrypt/fxml/vault-selection.fxml"));
            Mangrypt.getBase().setSceneRoot(loader.load());
            Mangrypt.getBase().setMatrixScroll(true);
            passwordOverlayLayer.setVisible(false); // After loader.load() to ensure it does not run if loading fails
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        showAlert(Alert.AlertType.ERROR, "An unexpected error occurred:", error, true, _ -> {}, ButtonType.OK);
    }

    public void showAlert(Alert.AlertType type, String header, String content, boolean cancelable, Consumer<ButtonType> onAction, ButtonType... buttons) {
        Runnable action = () -> {
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
        };
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }

    public void showInputDialog(String header, String hint, String defaultText, boolean cancelable, Consumer<String> onAction) {
        showInputDialog(header, hint, defaultText, cancelable, _ -> true, _ -> null, onAction);
    }
    public void showInputDialog(String header, String hint, String defaultText, boolean cancelable, Function<String, String> infoFunction, Consumer<String> onAction) {
        showInputDialog(header, hint, defaultText, cancelable, _ -> true, infoFunction, onAction);
    }

    public void showInputDialog(String header, String hint, String defaultText, boolean cancelable, Function<String, Boolean> allowFunction, Function<String, String> infoFunction, Consumer<String> onAction) {
        Runnable action = () -> {
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
        };
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }
}
