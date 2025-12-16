package io.github.redstonemango.mangrypt.front.elementBase;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public abstract class ListEntryBase extends BorderPane {

    protected final AnchorPane anchorPane;
    protected final Button selectButton;
    protected final ImageView imageView;
    protected final Button deleteButton;
    protected final ImageView imageView0;
    protected final VBox vBox;
    protected final Label nameLabel;
    protected final Label descriptionLabel;
    protected final StackPane icon;
    protected final ImageView iconView;
    protected final ImageView iconOverlayView;

    public ListEntryBase() {

        anchorPane = new AnchorPane();
        selectButton = new Button();
        imageView = new ImageView();
        deleteButton = new Button();
        imageView0 = new ImageView();
        vBox = new VBox();
        nameLabel = new Label();
        descriptionLabel = new Label();
        icon = new StackPane();
        iconView = new ImageView();
        iconOverlayView = new ImageView();

        setMaxHeight(USE_PREF_SIZE);
        setMaxWidth(USE_PREF_SIZE);
        setMinHeight(USE_PREF_SIZE);
        setMinWidth(USE_PREF_SIZE);
        setPrefHeight(60.0);

        BorderPane.setAlignment(anchorPane, javafx.geometry.Pos.CENTER);
        anchorPane.setPrefHeight(60.0);
        anchorPane.setPrefWidth(118.0);

        selectButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        selectButton.setFocusTraversable(false);
        selectButton.setLayoutX(61.0);
        selectButton.setLayoutY(8.0);
        selectButton.setMnemonicParsing(false);
        selectButton.setOnAction(this::onSelect);
        selectButton.setPrefHeight(40.0);
        selectButton.setPrefWidth(40.0);

        imageView.setFitHeight(35.0);
        imageView.setFitWidth(39.0);
        imageView.setPickOnBounds(true);
        imageView.setPreserveRatio(true);
        imageView.setImage(new Image(getClass().getResource("/io/github/redstonemango/mangrypt/image/select.png").toExternalForm()));
        selectButton.setGraphic(imageView);

        deleteButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        deleteButton.setFocusTraversable(false);
        deleteButton.setLayoutX(3.0);
        deleteButton.setLayoutY(8.0);
        deleteButton.setMnemonicParsing(false);
        deleteButton.setOnAction(this::onDelete);
        deleteButton.setPrefHeight(40.0);
        deleteButton.setPrefWidth(40.0);

        imageView0.setFitHeight(35.0);
        imageView0.setFitWidth(39.0);
        imageView0.setPickOnBounds(true);
        imageView0.setPreserveRatio(true);
        imageView0.setImage(new Image(getClass().getResource("/io/github/redstonemango/mangrypt/image/delete.png").toExternalForm()));
        deleteButton.setGraphic(imageView0);
        setRight(anchorPane);

        BorderPane.setAlignment(vBox, javafx.geometry.Pos.CENTER);
        vBox.setPrefHeight(60.0);
        vBox.setPrefWidth(285.0);

        nameLabel.getStyleClass().add("uncolored-label");
        nameLabel.setText("NAME");
        nameLabel.setFont(new Font(17.0));
        VBox.setMargin(nameLabel, new Insets(0.0));

        descriptionLabel.getStyleClass().add("uncolored-label");
        descriptionLabel.setText("Description");
        VBox.setMargin(descriptionLabel, new Insets(0.0, 0.0, 0.0, 10.0));
        vBox.setPadding(new Insets(10.0, 0.0, 0.0, 10.0));
        setCenter(vBox);

        BorderPane.setAlignment(icon, javafx.geometry.Pos.CENTER);
        BorderPane.setMargin(icon, new Insets(0.0, 0.0, 0.0, 5.0));

        iconView.setFitHeight(32.0);
        iconView.setFitWidth(32.0);
        iconView.setPickOnBounds(true);
        iconView.setPreserveRatio(true);

        iconOverlayView.setFitHeight(32.0);
        iconOverlayView.setFitWidth(32.0);
        iconOverlayView.setPickOnBounds(true);
        iconOverlayView.setPreserveRatio(true);
        setLeft(icon);

        anchorPane.getChildren().add(selectButton);
        anchorPane.getChildren().add(deleteButton);
        vBox.getChildren().add(nameLabel);
        vBox.getChildren().add(descriptionLabel);
        icon.getChildren().add(iconView);
        icon.getChildren().add(iconOverlayView);

    }

    protected abstract void onSelect(javafx.event.ActionEvent actionEvent);

    protected abstract void onDelete(javafx.event.ActionEvent actionEvent);

}
