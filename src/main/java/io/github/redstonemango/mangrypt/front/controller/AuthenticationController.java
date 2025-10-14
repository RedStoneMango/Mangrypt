package io.github.redstonemango.mangrypt.front.controller;

import io.github.redstonemango.mangrypt.back.Utilities;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.front.ShakeTransition;
import io.github.redstonemango.mangrypt.back.ConfigIO;

import javax.crypto.AEADBadTagException;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class AuthenticationController {

    @FXML StackPane root;
    @FXML Pane backgroundContainer;
    @FXML PasswordField passphraseField;
    @FXML PasswordField passwordField;
    @FXML Label triesLeftLabel;
    @FXML AnchorPane panelContainer;

    private int tries = 3;

    private boolean allowCancelInternal = true;
    private boolean allowDecrypt = true;

    @FXML
    private void initialize() {
        passphraseField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onDecrypt();
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onDecrypt();
        });
        Utilities.registerClosableOverlay(root, this::cancelAuth, panelContainer);

        Platform.runLater(() -> passphraseField.requestFocus());
    }

    private void cancelAuth() {
        if (!allowCancelInternal) return; // If this is called during the baseView transition, cancel the cleanup to avoid empty configs while the encrypted view is shown.
        Mangrypt.getBase().setSecondLayerRoot(null);
        ConfigIO.cleanup();
    }

    @FXML
    private void onDecrypt() {
        if (!allowDecrypt) return;

        char[] passphrase = passphraseField.getText().toCharArray();
        char[] password = passwordField.getText().toCharArray();

        Mangrypt.getBase().decryptionWaitingScreen(true);

        CompletableFuture<Boolean> saveFuture = CompletableFuture.supplyAsync(() -> {
            boolean success;
            try {
                success = ConfigIO.decryptConfig(passphrase, password);
            }
            catch (AEADBadTagException _) {
                success = false; // GCM tag mismatch: Wrong password
            }
            catch (Exception e) {
                passwordField.setText("");
                success = false;
            }
            finally {
                Arrays.fill(passphrase, '\0');
                Arrays.fill(password, '\0');
            }
            return success;
        });

        saveFuture.whenComplete((success, ex) -> Platform.runLater(() -> {

            if (ex != null || !success) {
                if (ex != null) {
                    Mangrypt.getBase().showErrorAlert(String.valueOf(ex));
                    System.err.print("Error decrypting config: ");
                    ex.printStackTrace(System.err);
                    cancelAuth();
                }
                Mangrypt.getBase().decryptionWaitingScreen(false);
                decreaseTries();
                return;
            }

            // If we reach this, decryption was successful
            passphraseField.setText("");
            passwordField.setText("");

            allowCancelInternal = false;
            Mangrypt.getBase().playTransition(() -> {
                Mangrypt.getBase().decryptionWaitingScreen(false);
                Mangrypt.getBase().setSecondLayerRoot(null);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/file-system.fxml"));
                try {
                    Mangrypt.getBase().setSceneRoot(loader.load());
                    ConfigIO.markVaultOpen();
                } catch (IOException e) {
                    throw new RuntimeException(e); // We can throw here without an error screen; If this is ever caught, the app was compiled incorrectly, and we're cooked wither way
                }
            });
        }));
    }

    private void decreaseTries() {
        tries --;
        triesLeftLabel.setText((tries > 1 ? tries + " tries" : (tries == 1 ? "1 try" : "No tries")) + " left");
        ShakeTransition transition = new ShakeTransition(Duration.seconds(2), triesLeftLabel);
        transition.setShakeX(-2);
        transition.setCycles(10);
        transition.play();
        if (tries == 0) {
            allowDecrypt = false;
            passwordField.setText("");
            passphraseField.setText("");
            transition.setOnFinished(_ -> cancelAuth());
        }
        else {
            if (Mangrypt.getBase().getScene().getFocusOwner() instanceof PasswordField field) {
                field.selectAll();
            }
            else {
                passphraseField.requestFocus(); // Automatically selects text
            }
        }
    }

    @FXML
    private void onQuestionPassphrase() {
        Mangrypt.getBase().showAlert(
                Alert.AlertType.INFORMATION,
                "Information: Data Passphrase",
                """
                        This data passphrase is used to encrypt your vault.
                        You will be prompted for this every time a vault is opened.
                         !!! Make it as strong as possible !!!""",
                true,
                _ -> {},
                ButtonType.CLOSE);
    }

    @FXML
    private void onQuestionPassword() {
        Mangrypt.getBase().showAlert(
                Alert.AlertType.INFORMATION,
                "Information: Access Password",
                """
                        This access password is used to encrypt and obscure your vault.
                        You will be prompted for this every time a vault is opened and when the vault is obscured on \
                        focus-loss.
                        """,
                true,
                _ -> {},
                ButtonType.CLOSE);
    }
}
