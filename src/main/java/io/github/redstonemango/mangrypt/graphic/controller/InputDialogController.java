package io.github.redstonemango.mangrypt.graphic.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.function.Consumer;
import java.util.function.Function;

public class InputDialogController {

    @FXML private Label headerLabel;
    @FXML private Label contentLabel;
    @FXML private ButtonBar buttonBar;
    @FXML private TextField inputField;
    @FXML private StackPane root;

    public void init(String header, String hint, String defaultText, boolean cancelable, Function<String, Boolean> allowFunction, Consumer<String> onAction) {
        headerLabel.setText(header);
        contentLabel.setText(hint + ":");
        inputField.setText(defaultText);
        inputField.selectAll();

        final Node oldFocusOwner = root.getScene().getFocusOwner();
        Platform.runLater(() -> {
            inputField.requestFocus();
        });

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
        if (cancelable) {
            Button cancel = new Button(ButtonType.CANCEL.getText());
            ButtonBar.setButtonData(cancel, ButtonType.CANCEL.getButtonData());
            buttonBar.getButtons().add(cancel);
            cancel.setOnAction(_ -> {
                oldFocusOwner.requestFocus();
                root.getParent().setVisible(false);
            });

            root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    oldFocusOwner.requestFocus();
                    root.getParent().setVisible(false);
                }
            });

            Platform.runLater(() -> {
                root.setOnMouseClicked(e -> {
                    Point2D scenePos = headerLabel.getParent().localToScene(0, 0);
                    double width = ((AnchorPane) headerLabel.getParent()).getWidth();
                    double height = ((AnchorPane) headerLabel.getParent()).getHeight();
                    if (!(e.getSceneX() >= scenePos.getX()
                        && e.getSceneX() < scenePos.getX() + width
                        && e.getSceneY() >= scenePos.getY()
                        && e.getSceneY() < scenePos.getY() + height))
                    {
                        oldFocusOwner.requestFocus();
                        root.getParent().setVisible(false);
                    }
                });
            });
        }
    }

}
