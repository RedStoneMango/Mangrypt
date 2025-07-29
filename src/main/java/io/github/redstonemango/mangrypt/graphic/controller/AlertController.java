package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.BaseView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
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
        // Green is set by default

        headerLabel.setText(header);
        contentLabel.setText(content);

        if (buttons.length == 0) {
            Button button = new Button(ButtonType.OK.getText());
            button.setStyle(createButtonStyle(type));
            ButtonBar.setButtonData(button, ButtonType.OK.getButtonData());
            buttonBar.getButtons().add(button);
            button.setOnAction(_ -> {
                onAction.accept(ButtonType.OK);
                headerLabel.getParent().getParent().getParent().setVisible(false);
            });
        }
        else {
            for (ButtonType buttonType : buttons) {
                Button button = new Button(buttonType.getText());
                button.setStyle(createButtonStyle(type));
                ButtonBar.setButtonData(button, buttonType.getButtonData());
                buttonBar.getButtons().add(button);
                button.setOnAction(_ -> {
                    onAction.accept(ButtonType.OK);
                    headerLabel.getParent().getParent().getParent().setVisible(false);
                });
            }
        }

        if (cancelable) {
            Platform.runLater(() -> {
                headerLabel.getParent().getParent().getParent().setOnMouseClicked(e -> {
                    Point2D scenePos = headerLabel.getParent().localToScene(0, 0);
                    double width = ((AnchorPane) headerLabel.getParent()).getWidth();
                    double height = ((AnchorPane) headerLabel.getParent()).getHeight();
                    if (!(e.getSceneX() >= scenePos.getX()
                        && e.getSceneX() < scenePos.getX() + width
                        && e.getSceneY() >= scenePos.getY()
                        && e.getSceneY() < scenePos.getY() + height))
                    {
                        headerLabel.getParent().getParent().getParent().setVisible(false);
                    }
                });
            });
        }
    }

    private static String createButtonStyle(Alert.AlertType type) {
        return switch (type) {
            case ERROR -> "-fx-background-color: linear-gradient(red, darkred); -fx-border-color: red; -fx-border-radius: 5;";
            case WARNING -> "-fx-background-color: linear-gradient(yellow, gold); -fx-border-color: yellow; -fx-border-radius: 5;";
            default -> "-fx-background-color: linear-gradient(lightgreen, green); -fx-border-color: lightgreen; -fx-border-radius: 5;";
        };
    }

}
