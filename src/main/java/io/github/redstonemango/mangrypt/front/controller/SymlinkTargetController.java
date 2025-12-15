package io.github.redstonemango.mangrypt.front.controller;

import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.back.dataTypes.FolderElement;
import io.github.redstonemango.mangrypt.front.PathCompletion;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
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

    public void init(String initialPath, FolderElement currentFolder, Consumer<String> doneCallback, Runnable closeCallback) {
        pathField.setText(initialPath);
        pathField.selectAll();

        oldFocusOwner = root.getScene().getFocusOwner();
        Platform.runLater(() -> pathField.requestFocus());

        new PathCompletion(pathField, currentFolder, new SimpleBooleanProperty(true), _ -> {});

        doneButton.setOnAction(_ -> {
            doneCallback.accept(pathField.getText());
            oldFocusOwner.requestFocus();
            closeCallback.run();
        });
        cancelButton.setOnAction(_ -> {
            oldFocusOwner.requestFocus();
            closeCallback.run();
        });
        pathField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) doneButton.fire();
        });
        Utilities.registerClosableOverlay(root, () -> {
            oldFocusOwner.requestFocus();
            closeCallback.run();
        }, pathField.getParent());
    }
}
