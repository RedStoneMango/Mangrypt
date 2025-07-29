package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.BaseView;
import io.github.redstonemango.mangrypt.graphic.MatrixBackground;
import io.github.redstonemango.mangrypt.logic.ConfigIO;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecuritySetupController {

    private MatrixBackground matrixBackground;
    private boolean setupPassphrase = true;

    private final CompletableFuture<Parent> folderOverviewFuture = new CompletableFuture<>(); // For JavaFX node lazy-loaded after passphrase setup

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
        matrixBackground = new MatrixBackground(backgroundContainer);

        root.widthProperty().addListener((_, _, _) -> matrixBackground.update());
        root.heightProperty().addListener((_, _, _) -> matrixBackground.update());
        root.visibleProperty().addListener((_, _, isVisible) -> {
            if (isVisible) matrixBackground.playScroll();
            else matrixBackground.stopScroll();
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onSetup();
        });
        passwordConfirmationField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onSetup();
        });
        Platform.runLater(() -> {
            matrixBackground.update();
            matrixBackground.playScroll();
        });
    }

    @FXML
    private void onSetup() {
        if (passwordField.getText().isEmpty()) {
            Mangrypt.getBase().showWarningAlert("A " + (setupPassphrase ? "passphrase" : "password") + " has to be specified in the first field");
            return;
        }
        if (passwordConfirmationField.getText().isEmpty()) {
            Mangrypt.getBase().showWarningAlert("The " + (setupPassphrase ? "passphrase" : "password") + " has to be confirmed by re-typing it in the second field");
            return;
        }

        if (!passwordField.getText().equals(passwordConfirmationField.getText())) {
            Mangrypt.getBase().showWarningAlert("The entered " + (setupPassphrase ? "passphrases" : "passwords") + " do not match");
            return;
        }

        if (setupPassphrase) {
            ConfigIO.getLayer1().updatePassphrase(passwordField.getText().toCharArray());
            preparePasswordSetup();
        }
        else {
            try {
                ConfigIO.getLayer1().getLayer2().updatePassword(passwordField.getText().toCharArray());
            }
            catch (Exception e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error hashing password", e);
            }

            folderOverviewFuture.thenAccept(Mangrypt.getBase()::setSceneRoot);
        }
    }

    private void preparePasswordSetup() {
        FadeTransition outTransition = new FadeTransition(Duration.millis(250), panelContainer);
        outTransition.setFromValue(1);
        outTransition.setToValue(0.1);
        outTransition.setOnFinished(_ -> {
            setupPassphrase = false;
            headerLabel.setText("Security Setup (2 / 2)");
            contentLabel.setText("Please setup a \"Session password\" for which you will be prompted every time this application is re-visited:");
            passwordField.setPromptText("Password");
            passwordField.setText("");
            passwordFieldTooltip.setText("Enter the password to use");
            passwordConfirmationField.setPromptText("Confirm Password");
            passwordConfirmationField.setText("");
            passwordConfirmationFieldTooltip.setText("Confirm the password to use");
            setupButton.setText("Setup password...");

            FadeTransition inTransition = new FadeTransition(Duration.millis(250), panelContainer);
            inTransition.setFromValue(0.1);
            inTransition.setToValue(1);
            inTransition.play();
        });
        outTransition.play();

        try (ExecutorService service = Executors.newSingleThreadExecutor()) { // Lazy-load folder layout
            service.submit(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(
                            "/io/github/redstonemango/mangrypt/fxml/folder-overview.fxml"));
                    Parent layout = loader.load();
                    folderOverviewFuture.complete(layout);
                } catch (IOException e) {
                    folderOverviewFuture.completeExceptionally(e);
                } finally {
                    service.shutdown();
                }
            });
        }
    }
}
