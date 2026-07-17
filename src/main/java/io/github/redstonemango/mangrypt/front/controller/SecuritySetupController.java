package io.github.redstonemango.mangrypt.front.controller;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.*;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Arrays;

public class SecuritySetupController {

    private boolean setupPassphrase = true;
    private boolean disallowCancel = false;
    private boolean isSetup;
    private char[] passphrase;

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
        isSetup = !ConfigIO.isVaultOpen();
        Utilities.registerClosableOverlay(root, () -> {
            root.setVisible(false);
        }, headerLabel.getParent());

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

        Platform.runLater(() -> passwordField.requestFocus());
    }

    private void cancelSetup() {
        if (disallowCancel) return;
        if (isSetup) ConfigIO.cleanup(); // If we are setting up, clean all the data. If we are just updating the password, maintain the old ones
        Mangrypt.getBase().setSecondLayerRoot(null);
    }

    @FXML
    private void onSetup() {
        char[] chars = passwordField.getText().toCharArray();
        char[] confirmChars = passwordConfirmationField.getText().toCharArray();
        if (chars.length == 0) {
            passwordField.requestFocus();
            Mangrypt.getBase().showWarningAlert("A " + (setupPassphrase ? "passphrase" : "password") + " has to be specified in the first field");
            return;
        }
        if (confirmChars.length == 0) {
            passwordConfirmationField.requestFocus();
            Mangrypt.getBase().showWarningAlert("The " + (setupPassphrase ? "passphrase" : "password") + " has to be confirmed by re-typing it in the second field");
            return;
        }
        if (!Arrays.equals(chars, confirmChars)) {
            passwordConfirmationField.requestFocus();
            passwordConfirmationField.selectAll();
            Mangrypt.getBase().showWarningAlert("The entered " + (setupPassphrase ? "passphrases" : "passwords") + " do not match");
            return;
        }
        passwordField.setText("");
        passwordConfirmationField.setText("");
        Arrays.fill(confirmChars, '\0');

        if (setupPassphrase) {
            passphrase = chars;
            preparePasswordSetup();
        }
        else {
            // At this point, 'chars' is our password and 'passphrase' our passphrase
            try {
                ConfigIO.setPasswords(passphrase, chars);
            }
            catch (Exception e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error updating password", e);
            }
            finally {
                Arrays.fill(chars, '\0');
                Arrays.fill(passphrase, '\0');
            }

            if (isSetup) {
                disallowCancel = true; // Do not allow cancel while transition is playing
                Mangrypt.getBase().playTransition(() -> {
                    Mangrypt.getBase().setSecondLayerRoot(null);
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/file-system.fxml"));
                    try {
                        Mangrypt.getBase().setSceneRoot(loader.load());
                        Mangrypt.getBase().setMatrixScroll(false);
                        ConfigIO.markVaultOpen();
                        ConfigIO.markShouldSave(); // Save is required. Otherwise, vault file will be empty after exit
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            else {
                Mangrypt.getBase().setSecondLayerRoot(null);
                Mangrypt.getBase().showInfoAlert("Successfully updated password and passphrase");
                ConfigIO.markShouldSave();
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
            contentLabel.setText("Please setup an \"Access password\" for which you will be prompted when decrypting the vault and when the vault was obscured because it lost focus:");
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
    }
}
