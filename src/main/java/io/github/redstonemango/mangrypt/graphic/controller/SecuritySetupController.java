package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.ClosableOverlay;
import io.github.redstonemango.mangrypt.logic.ConfigIO;
import io.github.redstonemango.mangrypt.logic.Configuration;
import io.github.redstonemango.mangrypt.logic.CypherEncryption;
import io.github.redstonemango.mangrypt.logic.SecureData;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecuritySetupController {

    private boolean setupPassphrase = true;
    private boolean hasDecrypted = false;
    private boolean isSetup;
    private SecretKey passphrase;
    private byte[] passphraseSalt;

    private final CompletableFuture<Pane> vaultOverviewFuture = new CompletableFuture<>(); // For JavaFX node lazy-loaded after passphrase setup

    @FXML private StackPane root;
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
        ClosableOverlay.apply(root, (Region) headerLabel.getParent(), () -> {
            root.setVisible(false);
        });

        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onSetup();
        });
        passwordConfirmationField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onSetup();
        });
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                cancelSetup();
            }
        });
        root.setOnMouseClicked(e -> {
            Point2D scenePos = panelContainer.localToScene(0, 0);
            double width = panelContainer.getWidth();
            double height = panelContainer.getHeight();
            if (!(e.getSceneX() >= scenePos.getX()
                    && e.getSceneX() < scenePos.getX() + width
                    && e.getSceneY() >= scenePos.getY()
                    && e.getSceneY() < scenePos.getY() + height))
            {
                cancelSetup();
            }
        });

        Platform.runLater(() -> {
            passwordField.requestFocus();
        });
    }

    private void cancelSetup() {
        if (hasDecrypted) return; // If this is called during the baseView transition, cancel the cleanup to avoid empty configs while the encrypted view is shown.
        if (isSetup) ConfigIO.cleanup(); // If we are setting up, clean all the data. If we are just updating the password, maintain the old ones
        Mangrypt.getBase().setSecondLayerRoot(null);
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
        Arrays.fill(controlChars, '\0');
        passwordConfirmationField.setText("");

        if (setupPassphrase) {
            try {
                passphraseSalt = CypherEncryption.generateRandomSalt();
                passphrase = CypherEncryption.deriveKey(chars, passphraseSalt);
            }
            catch (Exception e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error updating passphrase", e);
            }
            finally {
                passwordField.setText("");
                Arrays.fill(chars, '\0');
            }
            preparePasswordSetup();
        }
        else {
            if (isSetup) {
                try {
                    ConfigIO.getConfig().updatePassword(chars);
                    ConfigIO.getConfig().updatePassphrase(passphrase, passphraseSalt);
                }
                catch (Exception e) {
                    Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                    throw new RuntimeException("Error updating password", e);
                }
                finally {
                    passwordField.setText("");
                    Arrays.fill(chars, '\0');
                }
                hasDecrypted = true;
                Mangrypt.getBase().playTransition(() -> {
                    ConfigIO.markShouldSave();
                    Mangrypt.getBase().setSecondLayerRoot(null);
                    vaultOverviewFuture.thenAccept(Mangrypt.getBase()::setSceneRoot);
                });
            }
            else {

                hasDecrypted = true;
                Mangrypt.getBase().showAlert(Alert.AlertType.INFORMATION, "Working...", "Updating references to use the new password and passphrase", false, _ -> {});
                try (ExecutorService service = Executors.newSingleThreadExecutor()) {
                    service.execute(() -> {
                        boolean error = false;
                        try {
                            byte[] passwordSalt = CypherEncryption.generateRandomSalt();
                            SecretKey key = CypherEncryption.deriveKey(chars, passwordSalt);
                            for (Configuration.Folder folder : ConfigIO.getConfig().getFolders()) {
                                for (SecureData.Encrypted data : folder.getEncryptedData()) {
                                    data.updatePassword(key, passwordSalt);
                                }
                            }

                            // Use 2 different stages to prevent en exception thrown in between, resulting in half the data being encrypted with the new password, half the data with the old one
                            Platform.runLater(() -> Mangrypt.getBase().showAlert(Alert.AlertType.INFORMATION, "Working...", "Finalizing update", false, _ -> {}));

                            for (Configuration.Folder folder : ConfigIO.getConfig().getFolders()) {
                                for (SecureData.Encrypted data : folder.getEncryptedData()) {
                                    data.finalizePasswordUpdate();
                                }
                            }
                            ConfigIO.getConfig().updatePassword(key, passwordSalt, chars);
                            ConfigIO.getConfig().updatePassphrase(passphrase, passphraseSalt);
                        }
                        catch (Exception e) {
                            error = true;
                            Platform.runLater(() -> {
                                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                                throw new RuntimeException("Error updating password", e);
                            });
                        }
                        finally {
                            passwordField.setText("");
                            Arrays.fill(chars, '\0');
                        }
                        Mangrypt.getBase().setSecondLayerRoot(null);
                        if (!error) {
                            Platform.runLater(() -> Mangrypt.getBase().showInfoAlert("Successfully updated passphrase and password"));
                        }
                    });
                }
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
            contentLabel.setText("Please setup an \"Access password\" for which you will be prompted every time this application is re-visited:");
            passwordField.setPromptText("Password");
            passwordFieldTooltip.setText("Enter the password to use");
            passwordConfirmationField.setPromptText("Confirm Password");
            passwordConfirmationFieldTooltip.setText("Confirm the password to use");
            setupButton.setText("Setup password...");
            passwordField.requestFocus();

            FadeTransition inTransition = new FadeTransition(Duration.millis(250), panelContainer);
            inTransition.setFromValue(0.1);
            inTransition.setToValue(1);
            inTransition.play();
        });
        outTransition.play();

        if (!isSetup) return; // When updating the password only, we do not need to load the folder overview layout

        try (ExecutorService service = Executors.newSingleThreadExecutor()) { // Lazy-load vault layout
            service.execute(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/folder-overview.fxml"));
                    vaultOverviewFuture.complete(loader.load());
                } catch (IOException e) {
                    vaultOverviewFuture.completeExceptionally(e);
                } finally {
                    service.shutdown();
                }
            });
        }
    }
}
