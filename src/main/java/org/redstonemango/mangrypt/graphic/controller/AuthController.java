package org.redstonemango.mangrypt.graphic.controller;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.redstonemango.mangrypt.Mangrypt;
import org.redstonemango.mangrypt.ShakeTransition;
import org.redstonemango.mangrypt.logic.ConfigIO;

import java.io.IOException;

/**
 * This class controls the authentication passphrase as well as the password check screen, for they have almost the same underlying logic.
 */
public class AuthController {

    private static final Image BACKGROUND_SPRITE = new Image(AuthController.class.getResourceAsStream("/org/redstonemango/mangrypt/image/matrix-rain-sprite.png"));
    private static final int BACKGROUND_SPRITE_SIZE = 400;
    private static final Duration BACKGROUND_SCROLL_DURATION = Duration.seconds(20);
    private boolean shouldPlayBackgroundScroll = false;

    @FXML StackPane root;
    @FXML Pane backgroundContainer;
    @FXML PasswordField passwordField;
    @FXML Label triesLeftLabel;
    /**
     * If this is not <code>null</code>, we are to apply the passphrase logic; otherwise the password logic should be applied.
     */
    @FXML Label passphraseIndicator;

    private int oldXCount = -1;
    private int oldYCount = -1;

    private int tries = 3;

    @FXML
    private void initialize() {
        root.widthProperty().addListener((_, _, _) -> updateBackground());
        root.heightProperty().addListener((_, _, _) -> updateBackground());
        root.visibleProperty().addListener((_, _, isVisible) -> {
            if (isVisible) startBackgroundScroll();
            else endBackgroundScroll();
        });
        startBackgroundScroll();
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onDecrypt();
        });
    }

    @FXML
    private void onDecrypt() {
        if (passphraseIndicator == null) {
            boolean success = ConfigIO.getLayer1().isLayer2Decrypted() ?
                    verifyPassword() :
                    decryptPassword();

            if (success) {
                Mangrypt.getBase().showPasswordOverlay(false);
            }
        }
        else {
            if (decryptPassphrase()) {
                Mangrypt.getBase().showPasswordOverlay(true);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/redstonemango/mangrypt/fxml/folder-overview.fxml"));
                try {
                    Mangrypt.getBase().setSceneRoot(loader.load());
                } catch (IOException e) {
                    throw new RuntimeException(e); // We can throw here without an error screen; If this is ever caught, we're cooked wither way
                }
            }
        }
    }

    private boolean decryptPassphrase() {
        boolean decryptionSuccess = ConfigIO.decryptLayerOne(passwordField.getText());
        if (!decryptionSuccess) {
            decreaseTries();
            return false;
        }
        passwordField.setText("");
        return true;
    }

    private boolean decryptPassword() {
        boolean decryptionSuccess = ConfigIO.getLayer1().decryptLayer2(passwordField.getText());
        if (decryptionSuccess) {
             boolean valid = verifyPassword(); // Backwards-check the password to prevent a false-positive decryption (tough it is very unlikely, it's better to be safe than sorry).
             if (!valid) {
                 decreaseTries();
             }
             return valid;
        }
        else {
            decreaseTries();
            return false;
        }
    }

    private boolean verifyPassword() {
        try {
            boolean valid = ConfigIO.getLayer1().getLayer2().verifyPassword(passwordField.getText());
            if (!valid) {
                decreaseTries();
                return false;
            }
            passwordField.setText("");
            return true;
        } catch (Exception e) {
            Mangrypt.getBase().showErrorAlert("Verify Password", String.valueOf(e));
            throw new RuntimeException("Error verifying the password", e);
        }
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

    private void updateBackground() {
        int width = (int) root.getWidth();
        int height = (int) root.getHeight();
        int xCount = Math.ceilDiv(width, BACKGROUND_SPRITE_SIZE);
        int yCount = Math.ceilDiv(height, BACKGROUND_SPRITE_SIZE) + 1; // +1 for scroll buffer

        if (oldXCount != xCount || oldYCount != yCount) {
            backgroundContainer.getChildren().clear();
            for (int i = -1; i < xCount; i++) {
                for (int j = -1; j < yCount; j++) {
                    double x = BACKGROUND_SPRITE_SIZE * i;
                    double y = BACKGROUND_SPRITE_SIZE * j;
                    ImageView imageView = new ImageView(BACKGROUND_SPRITE);
                    imageView.setFitWidth(BACKGROUND_SPRITE_SIZE);
                    imageView.setFitHeight(BACKGROUND_SPRITE_SIZE);
                    imageView.setX(x);
                    imageView.setY(y);
                    backgroundContainer.getChildren().add(imageView);
                }
            }
        }
        oldXCount = xCount;
        oldYCount = yCount;
    }

    private void startBackgroundScroll() {
        if (shouldPlayBackgroundScroll) return; // If already playing, don't start again
        shouldPlayBackgroundScroll = true;

        TranslateTransition scroll = new TranslateTransition(BACKGROUND_SCROLL_DURATION, backgroundContainer);
        scroll.setByY(BACKGROUND_SPRITE_SIZE);
        scroll.setInterpolator(Interpolator.LINEAR);
        scroll.setOnFinished(_ -> {
            backgroundContainer.setTranslateY(0);
            if (shouldPlayBackgroundScroll) scroll.playFromStart();
        });
        scroll.play();
    }

    private void endBackgroundScroll() {
        shouldPlayBackgroundScroll = false;
    }

    public void prepare() {
        tries = 3;
    }
}
