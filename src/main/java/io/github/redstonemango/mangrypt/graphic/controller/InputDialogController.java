package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangrypt.logic.SharedLogicManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;
import java.util.function.Function;

public class InputDialogController {

    @FXML private Label headerLabel;
    @FXML private Label contentLabel;
    @FXML private ButtonBar buttonBar;
    @FXML private TextField inputField;
    @FXML private StackPane root;
    @FXML private Label infoLabel;

    public void init(String header, String hint, String defaultText, boolean cancelable, Function<String, Boolean> allowFunction, Function<String, String> infoFunction, Consumer<String> onAction) {
        headerLabel.setText(header);
        contentLabel.setText(hint + ":");
        inputField.setText(defaultText);
        inputField.selectAll();

        final Node oldFocusOwner = root.getScene().getFocusOwner();
        Platform.runLater(() -> inputField.requestFocus());

        Button okButton = new Button(ButtonType.OK.getText());
        ButtonBar.setButtonData(okButton, ButtonType.OK.getButtonData());
        buttonBar.getButtons().add(okButton);
        okButton.setOnAction(_ -> {
            onAction.accept(inputField.getText());
            oldFocusOwner.requestFocus();
            root.getParent().setVisible(false);
        });
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) okButton.fire();
        });

        okButton.disableProperty().bind(Bindings.createBooleanBinding(() -> !allowFunction.apply(inputField.getText()), inputField.textProperty()));
        inputField.textProperty().addListener((_, _, text) -> {
            String info = infoFunction.apply(text);
            if (info == null) {
                infoLabel.setVisible(false);
            }
            else {
                infoLabel.setVisible(true);
                infoLabel.setText("> " + info);
            }
        });

        if (cancelable) {
            Button cancel = new Button(ButtonType.CANCEL.getText());
            ButtonBar.setButtonData(cancel, ButtonType.CANCEL.getButtonData());
            buttonBar.getButtons().add(cancel);
            cancel.setOnAction(_ -> {
                oldFocusOwner.requestFocus();
                root.getParent().setVisible(false);
            });

            SharedLogicManager.registerClosableOverlay(root, () -> {
                root.getParent().setVisible(false);
                oldFocusOwner.requestFocus();
            }, (Region) headerLabel.getParent());
        }
    }

}
