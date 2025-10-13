package io.github.redstonemango.mangrypt.front.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class EncryptionWaitController {

    @FXML private Label title;
    @FXML private Label content;

    private void init(boolean isSaving) {
        if (isSaving) {
            title.setText(" Saving Vault..."); // 1 Space at the beginning for alignment
            content.setText("Mangrypt is currently in the process of securely saving your vault data to the .mgvault file.");
        }
        // Loading text is written by default: No need to set it manually
    }
}
