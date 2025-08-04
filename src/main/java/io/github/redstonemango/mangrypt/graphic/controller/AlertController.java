package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangrypt.graphic.ClosableOverlay;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
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
            button.setStyle(createButtonStyle(type));
            ButtonBar.setButtonData(button, buttonType.getButtonData());
            buttonBar.getButtons().add(button);
            button.setOnAction(_ -> {
                onAction.accept(buttonType);
                root.getParent().setVisible(false);
                oldFocusOwner.requestFocus();
            });
            if (buttonType == ButtonType.OK || buttonType == ButtonType.YES) defaultButton = button;
        }

        if (cancelable) {
            Button finalDefaultButton = defaultButton;

            root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ENTER && finalDefaultButton != null) {
                    finalDefaultButton.fire();
                }
            });
            ClosableOverlay.apply(root, (Region) headerLabel.getParent(), () -> {
                root.getParent().setVisible(false);
                oldFocusOwner.requestFocus();
            });
        }
    }

    private static String createButtonStyle(Alert.AlertType type) {
        return switch (type) {
            case ERROR -> "-fx-background-color: linear-gradient(red, darkred); -fx-border-color: red; -fx-border-radius: 5;";
            case WARNING -> "-fx-background-color: linear-gradient(yellow, orange); -fx-border-color: yellow; -fx-border-radius: 5;";
            default -> "-fx-background-color: linear-gradient(lightgreen, green); -fx-border-color: lightgreen; -fx-border-radius: 5;";
        };
    }

}
