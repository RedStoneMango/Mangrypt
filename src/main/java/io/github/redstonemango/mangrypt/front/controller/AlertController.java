package io.github.redstonemango.mangrypt.front.controller;

import io.github.redstonemango.mangrypt.back.Utilities;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.function.Consumer;

public class AlertController {

    public static final Image BACKGROUND_RED = new Image(AlertController.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/alert_error.png"));
    public static final Image BACKGROUND_YELLOW = new Image(AlertController.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/alert_warning.png"));
    public static final Image BACKGROUND_GREEN = new Image(AlertController.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/alert_success.png"));

    @FXML private Label headerLabel;
    @FXML private Label contentLabel;
    @FXML private ImageView background;
    @FXML private ButtonBar buttonBar;
    @FXML private StackPane root;

    public void init(Alert.AlertType type, String header, String content, boolean cancelable, Consumer<ButtonType> onAction, ButtonType... buttons) {
        if (type == Alert.AlertType.ERROR) {
            headerLabel.setTextFill(Color.RED);
            contentLabel.setTextFill(Color.RED);
            background.setImage(BACKGROUND_RED);
        }
        else if (type == Alert.AlertType.WARNING) {
            headerLabel.setTextFill(Color.YELLOW);
            contentLabel.setTextFill(Color.YELLOW);
            background.setImage(BACKGROUND_YELLOW);
        }
        else {
            headerLabel.setTextFill(Color.web("#20ff00")); // Has to match the CSS stylesheet
            contentLabel.setTextFill(Color.web("#20ff00"));
            background.setImage(BACKGROUND_GREEN);
        }


        final Node oldFocusOwner = root.getScene().getFocusOwner();

        headerLabel.setText(header);
        contentLabel.setText(content);

        Button defaultButton = null;

        for (ButtonType buttonType : buttons) {
            Button button = new Button(buttonType.getText());
            if (type == Alert.AlertType.ERROR || type == Alert.AlertType.WARNING) {
                button.getStyleClass().add(type == Alert.AlertType.ERROR ? "error-button" : "warning-button");
            }
            ButtonBar.setButtonData(button, buttonType.getButtonData());
            buttonBar.getButtons().add(button);
            button.setOnAction(_ -> {
                root.getParent().setVisible(false);
                oldFocusOwner.requestFocus();
                onAction.accept(buttonType);
            });
            if (buttonType == ButtonType.OK || buttonType == ButtonType.YES) defaultButton = button;
        }

        Platform.runLater(() -> {
            Node node = buttonBar.getButtons().getFirst();
            if (node != null) {
                node.requestFocus();
            }
        });

        if (cancelable) {
            Button finalDefaultButton = defaultButton;

            root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ENTER && finalDefaultButton != null) {
                    finalDefaultButton.fire();
                }
            });
            Utilities.registerClosableOverlay(root, () -> {
                root.getParent().setVisible(false);
                oldFocusOwner.requestFocus();
            }, headerLabel.getParent());
        }
    }

}
