package io.github.redstonemango.mangrypt.front.controller;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.ConfigIO;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.BaseView;
import io.github.redstonemango.mangrypt.front.ShakeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import java.util.Arrays;

public class OverlayController {

    @FXML private PasswordField passwordField;
    @FXML private Label triesLeftLabel;

    private int tries = 3;

    private boolean allowAccess = true;

    @FXML
    private void initialize() {
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onAccess();
        });

        Platform.runLater(() -> passwordField.requestFocus());
    }

    @FXML
    private void onAccess() {
        if (!allowAccess) return;

        char[] password = passwordField.getText().toCharArray();
        boolean authSuccess;
        try {
            authSuccess = ConfigIO.getConfig().verifyPassword(password);
        }
        catch (Exception e) {
            passwordField.setText("");
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error authenticating", e);
        }
        finally {
            Arrays.fill(password, '\0');
        }
        if (!authSuccess) {
            decreaseTries();
            return;
        }

        passwordField.setText("");
        Mangrypt.getBase().stopObscuring();
    }

    private void decreaseTries() {
        tries --;
        triesLeftLabel.setText((tries > 1 ? tries + " tries" : (tries == 1 ? "1 try" : "No tries")) + " left");
        ShakeTransition transition = new ShakeTransition(Duration.seconds(1), triesLeftLabel);
        transition.setShakeX(-2);
        transition.setCycles(5);
        transition.play();
        if (tries == 0) {
            allowAccess = false;
            passwordField.setText("");
            transition.setOnFinished(_ -> Mangrypt.getBase().savingRoutine());
        }
        else {
            passwordField.requestFocus();
            passwordField.selectAll();
        }
    }

    public void prepare() {
        Utilities.ensureAuthorizedAccess(BaseView.class);

        tries = 3;
        allowAccess = true;
        passwordField.setText("");
        triesLeftLabel.setText("3 tries left");
    }

}
