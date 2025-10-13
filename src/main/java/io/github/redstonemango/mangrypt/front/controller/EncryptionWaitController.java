package io.github.redstonemango.mangrypt.front.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class EncryptionWaitController {

    @FXML private Label title;
    @FXML private Label content;

    public void init(boolean isSaving) {
        if (isSaving) {
            title.setText(" Saving Vault..."); // 1 Space at the beginning for alignment
            content.setText("Mangrypt is currently in the process of securely saving your vault data to the .mgvault file.");
        }
        else {
            title.setText("Opening vault...");
            content.setText("Mangrypt is trying to decrypt your vault using the given password and passphrase.");
        }
    }
}
