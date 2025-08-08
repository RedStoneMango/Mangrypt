package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangrypt.graphic.ClosableOverlay;
import io.github.redstonemango.mangrypt.logic.Configuration;
import io.github.redstonemango.mangrypt.logic.CypherEncryption;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.ShakeTransition;
import io.github.redstonemango.mangrypt.logic.ConfigIO;

import javax.crypto.SecretKey;
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
    @FXML AnchorPane panelContainer;
    /**
     * If this is not <code>null</code>, we are to apply the passphrase logic; otherwise the password logic should be applied.
     */
    @FXML Label passphraseIndicator;

    private int tries = 3;
    /**
     * This is a flag for temporarily disallowing canceling.<br>
     * There also are other aspects to whether canceling is possible, specified in {@link #cancelAuth()}
     */
    private boolean allowCancelInternal = true;

    @FXML
    private void initialize() {
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onDecrypt();
        });
        ClosableOverlay.apply(root, panelContainer, this::cancelAuth);

        Platform.runLater(() -> passwordField.requestFocus());
    }

    private void cancelAuth() {
        if (Mangrypt.getBase().isObscuring()) return; // This is only allowed to work when decrypting a vault, not when obscuring data
        if (!allowCancelInternal) return; // If this is called during the baseView transition, cancel the cleanup to avoid empty configs while the encrypted view is shown.
        Mangrypt.getBase().hidePasswordDialog();
        Mangrypt.getBase().setSecondLayerRoot(null);
        ConfigIO.cleanup();
    }

    private byte[] extractPasswordSalt() {
        for (Configuration.Folder folder : ConfigIO.getConfig().getFolders()) {
            if (!folder.getEncryptedData().isEmpty()) {
                byte[] salt = folder.getEncryptedData().getFirst().extractSalt();
                return salt;
            }
        }
        return CypherEncryption.generateRandomSalt();
    }

    @FXML
    private void onDecrypt() {
        if (passphraseIndicator == null) {
            boolean success = verifyPassword();
            if (success) {
                allowCancelInternal = false;
                if (Mangrypt.getBase().isObscuring()) {
                    passwordField.setText("");
                    Mangrypt.getBase().hidePasswordDialog();
                    return;
                }

                char[] c = passwordField.getText().toCharArray();
                try {
                    byte[] passwordSalt = extractPasswordSalt();
                    SecretKey key = CypherEncryption.deriveKey(c, passwordSalt);
                    ConfigIO.getConfig().updatePassword(key, passwordSalt, c);
                } catch (Exception e) {
                    Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                    throw new RuntimeException("Error updating password", e);
                }
                finally {
                    passwordField.setText("");
                    Arrays.fill(c, '\0');
                }

                Mangrypt.getBase().playTransition(() -> {
                    ConfigIO.markShouldSave();
                    Mangrypt.getBase().hidePasswordDialog();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/folder-overview.fxml"));
                    try {
                        Mangrypt.getBase().setSceneRoot(loader.load());
                    } catch (IOException e) {
                        throw new RuntimeException(e); // We can throw here without an error screen; If this is ever caught, the app was compiled incorrectly, and we're cooked wither way
                    }
                });
            }
        }
        else {
            if (decryptConfig()) {
                allowCancelInternal = false;
                FadeTransition transition = new FadeTransition(Duration.millis(250), root);
                transition.setFromValue(1);
                transition.setToValue(0);
                transition.play();
                transition.setOnFinished(_ -> {
                    Mangrypt.getBase().setSecondLayerRoot(null);
                    Mangrypt.getBase().showPasswordDialog(false);
                    allowCancelInternal = true;
                });
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
        return true;
    }

    private void decreaseTries() {
        passwordField.selectAll();
        tries --;
        triesLeftLabel.setText((tries > 1 ? tries + " tries" : (tries == 1 ? "1 try" : "No tries")) + " left");
        ShakeTransition transition = new ShakeTransition(Duration.seconds(1), triesLeftLabel);
        transition.setShakeX(-2);
        transition.setCycles(5);
        transition.play();
        if (tries == 0) {
            passwordField.setText("");
            transition.setOnFinished(_ -> Mangrypt.safetyShutdown());
        }
        else {
            passwordField.selectAll();
        }
    }

    public void prepare() {
        tries = 3;
        allowCancelInternal = false;
    }
}
