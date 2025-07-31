package io.github.redstonemango.mangrypt.graphic.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.MatrixBackground;
import io.github.redstonemango.mangrypt.graphic.ShakeTransition;
import io.github.redstonemango.mangrypt.logic.ConfigIO;

import java.io.IOException;
import java.util.Arrays;

/**
 * This class controls the authentication passphrase as well as the password check screen, for they have almost the same underlying logic.
 */
public class AuthController {

    @FXML StackPane root;
    @FXML Pane backgroundContainer;
    @FXML PasswordField passwordField;
    @FXML Label triesLeftLabel;
    @FXML ImageView image;
    /**
     * If this is not <code>null</code>, we are to apply the passphrase logic; otherwise the password logic should be applied.
     */
    @FXML Label passphraseIndicator;

    private int tries = 3;
    private MatrixBackground matrixBackground;

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
            if (e.getCode() == KeyCode.ENTER) onDecrypt();
        });
        Platform.runLater(() -> {
            matrixBackground.update();
            matrixBackground.playScroll();
        });
    }

    @FXML
    private void onDecrypt() {
        if (passphraseIndicator == null) {
            boolean success = verifyPassword();
            if (success) {
                ConfigIO.markShouldSave();
                Mangrypt.getBase().showPasswordOverlay(false);
            }
        }
        else {
            if (decryptConfig()) {
                Mangrypt.getBase().showPasswordOverlay(true);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/folder-overview.fxml"));
                try {
                    Mangrypt.getBase().setSceneRoot(loader.load());
                } catch (IOException e) {
                    throw new RuntimeException(e); // We can throw here without an error screen; If this is ever caught, the app was compiled incorrectly, and we're cooked wither way
                }
            }
        }
    }

    private boolean decryptConfig() {
        char[] c = passwordField.getText().toCharArray();
        boolean decryptionSuccess;
        try {
            decryptionSuccess = ConfigIO.decryptConfig(c);
        }
        catch (Exception e) {
            passwordField.setText("");
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error decrypting config", e);
        }
        finally {
            Arrays.fill(c, '\0');
        }
        if (!decryptionSuccess) {
            decreaseTries();
            return false;
        }
        passwordField.setText("");
        return true;
    }

    private boolean verifyPassword() {
        char[] c = passwordField.getText().toCharArray();
        boolean valid;
        try {
            valid = ConfigIO.getConfig().verifyPassword(passwordField.getText().toCharArray());
        } catch (Exception e) {
            passwordField.setText("");
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error verifying the password", e);
        }
        finally {
            Arrays.fill(c, '\0');
        }
        if (!valid) {
            decreaseTries();
            return false;
        }
        passwordField.setText("");
        return true;
    }

    private void decreaseTries() {
        passwordField.selectAll();
        tries --;
        ShakeTransition transition = new ShakeTransition(Duration.seconds(1), triesLeftLabel);
        transition.setShakeX(-2);
        transition.setCycles(5);
        transition.play();
        if (tries == 0) {
            passwordField.setText("");
            transition.setOnFinished(_ -> Mangrypt.safetyShutdown());
            return;
        }
        else {
            passwordField.selectAll();
        }
        triesLeftLabel.setText((tries > 1 ? tries + " tries" : (tries == 1 ? "1 try" : "No tries")) + " left");
    }

    public void prepare() {
        tries = 3;
    }
}
