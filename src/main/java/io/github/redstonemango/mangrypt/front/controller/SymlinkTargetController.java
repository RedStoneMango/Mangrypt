package io.github.redstonemango.mangrypt.front.controller;

import io.github.redstonemango.mangrypt.back.Utilities;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;

public class SymlinkTargetController {

    @FXML private StackPane root;
    @FXML private Button doneButton;
    @FXML private Button cancelButton;
    @FXML private TextField pathField;

    private Node oldFocusOwner;

    public void init(String initialPath, Consumer<String> doneCallback) {
        pathField.setText(initialPath);
        pathField.selectAll();

        oldFocusOwner = root.getScene().getFocusOwner();
        Platform.runLater(() -> pathField.requestFocus());

        doneButton.setOnAction(_ -> {
            doneCallback.accept(pathField.getText());
            oldFocusOwner.requestFocus();
            root.getParent().setVisible(false);
        });
        cancelButton.setOnAction(_ -> {
            oldFocusOwner.requestFocus();
            root.getParent().setVisible(false);
        });
        pathField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) doneButton.fire();
        });
        Utilities.registerClosableOverlay(root, () -> {
            root.getParent().setVisible(false);
            oldFocusOwner.requestFocus();
        }, pathField.getParent());
    }
}
