package io.github.redstonemango.mangrypt.front.controller;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class EncryptionWaitController {

    @FXML private StackPane root;
    @FXML private Label title;
    @FXML private Label content;
    @FXML private ProgressIndicator spinner;

    private boolean firstCall = true;

    public void init(boolean isSaving, boolean beforeExit) {
        if (firstCall) {
            root.addEventFilter(KeyEvent.ANY, Event::consume);
            firstCall = false;
        }

        if (isSaving) {
            title.setText(" Saving Vault..."); // 1 Space at the beginning for alignment
            content.setText("Mangrypt is securely saving your vault data to the .mgvault file" + (beforeExit ? " before exiting." : "."));
            root.setBackground(Background.fill(Color.BLACK));
        }
        else {
            title.setText("Opening vault...");
            content.setText("Mangrypt is trying to decrypt your vault using the given password and passphrase.");
            root.setBackground(Background.fill(Color.TRANSPARENT));
        }
        Platform.runLater(spinner::requestFocus);
    }
}
