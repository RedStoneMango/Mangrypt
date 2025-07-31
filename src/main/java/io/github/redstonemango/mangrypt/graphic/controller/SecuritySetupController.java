package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.MatrixBackground;
import io.github.redstonemango.mangrypt.logic.ConfigIO;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecuritySetupController {

    private MatrixBackground matrixBackground;
    private boolean setupPassphrase = true;
    private boolean isSetup;

    private final CompletableFuture<Parent> vaultOverviewFuture = new CompletableFuture<>(); // For JavaFX node lazy-loaded after passphrase setup

    @FXML private StackPane root;
    @FXML private Pane backgroundContainer;
    @FXML private Label headerLabel;
    @FXML private Label contentLabel;
    @FXML private PasswordField passwordField;
    @FXML private Tooltip passwordFieldTooltip;
    @FXML private PasswordField passwordConfirmationField;
    @FXML private Tooltip passwordConfirmationFieldTooltip;
    @FXML private Button setupButton;
    @FXML private StackPane panelContainer;

    @FXML
    private void initialize() {
        isSetup = !ConfigIO.shouldSave();
        if (isSetup) {
            matrixBackground = new MatrixBackground(backgroundContainer);
        }
        else {
            backgroundContainer.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    root.setVisible(false);
                }
            });
            backgroundContainer.setOnMousePressed(e -> {
                Point2D scenePos = backgroundContainer.localToScene(0, 0);
                double width = ((AnchorPane) headerLabel.getParent()).getWidth();
                double height = ((AnchorPane) headerLabel.getParent()).getHeight();
                if (!(e.getSceneX() >= scenePos.getX()
                        && e.getSceneX() < scenePos.getX() + width
                        && e.getSceneY() >= scenePos.getY()
                        && e.getSceneY() < scenePos.getY() + height))
                {
                    root.setVisible(false);
                }
            });
        }

        root.widthProperty().addListener((_, _, _) -> {
            if (isSetup) matrixBackground.update();
        });
        root.heightProperty().addListener((_, _, _) -> {
            if (isSetup) matrixBackground.update();
        });
        root.visibleProperty().addListener((_, _, isVisible) -> {
            if (isSetup) {
                if (isVisible) matrixBackground.playScroll();
                else matrixBackground.stopScroll();
            }
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onSetup();
        });
        passwordConfirmationField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onSetup();
        });
        if (isSetup) {
            Platform.runLater(() -> {
                matrixBackground.update();
                matrixBackground.playScroll();
            });
        }
    }

    @FXML
    private void onSetup() {
        char[] chars = passwordField.getText().toCharArray();
        char[] controlChars = passwordConfirmationField.getText().toCharArray();
        if (chars.length == 0) {
            Mangrypt.getBase().showWarningAlert("A " + (setupPassphrase ? "passphrase" : "password") + " has to be specified in the first field");
            return;
        }
        if (controlChars.length == 0) {
            Mangrypt.getBase().showWarningAlert("The " + (setupPassphrase ? "passphrase" : "password") + " has to be confirmed by re-typing it in the second field");
            return;
        }
        if (!Arrays.equals(chars, controlChars)) {
            Mangrypt.getBase().showWarningAlert("The entered " + (setupPassphrase ? "passphrases" : "passwords") + " do not match");
            return;
        }

        if (setupPassphrase) {
            try {
                ConfigIO.getConfig().updatePassphrase(chars);
            }
            catch (Exception e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error updating passphrase", e);
            }
            finally {
                passwordField.setText("");
                passwordConfirmationField.setText("");
                Arrays.fill(chars, '\0');
            }
            preparePasswordSetup();
        }
        else {
            try {
                ConfigIO.getConfig().updatePassword(chars);
            }
            catch (Exception e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error updating password", e);
            }
            finally {
                passwordField.setText("");
                passwordConfirmationField.setText("");
                Arrays.fill(chars, '\0');
            }

            if (isSetup) {
                ConfigIO.markShouldSave();
                vaultOverviewFuture.thenAccept(Mangrypt.getBase()::setSceneRoot);
            }
            else {
                Mangrypt.getBase().setSecondLayerRoot(null);
                Mangrypt.getBase().showInfoAlert("Successfully updated passphrase and password");
            }
        }
    }

    private void preparePasswordSetup() {
        FadeTransition outTransition = new FadeTransition(Duration.millis(250), panelContainer);
        outTransition.setFromValue(1);
        outTransition.setToValue(0.1);
        outTransition.setOnFinished(_ -> {
            setupPassphrase = false;
            headerLabel.setText("Password Setup");
            contentLabel.setText("Please setup a \"Session password\" for which you will be prompted every time this application is re-visited:");
            passwordField.setPromptText("Password");
            passwordFieldTooltip.setText("Enter the password to use");
            passwordConfirmationField.setPromptText("Confirm Password");
            passwordConfirmationFieldTooltip.setText("Confirm the password to use");
            setupButton.setText("Setup password...");

            FadeTransition inTransition = new FadeTransition(Duration.millis(250), panelContainer);
            inTransition.setFromValue(0.1);
            inTransition.setToValue(1);
            inTransition.play();
        });
        outTransition.play();

        try (ExecutorService service = Executors.newSingleThreadExecutor()) { // Lazy-load vault layout
            service.submit(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(
                            "/io/github/redstonemango/mangrypt/fxml/folder-overview.fxml"));
                    Parent layout = loader.load();
                    vaultOverviewFuture.complete(layout);
                } catch (IOException e) {
                    vaultOverviewFuture.completeExceptionally(e);
                } finally {
                    service.shutdown();
                }
            });
        }
    }
}
