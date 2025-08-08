package io.github.redstonemango.mangrypt.graphic;

import io.github.redstonemango.mangrypt.logic.Configuration;
import io.github.redstonemango.mangrypt.logic.SecureData;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

public class DataView extends BorderPane {

    private final AnchorPane centerContainer;
    private final StackPane swipeArrowLeft;
    private final StackPane swipeArrowRight;

    private Configuration.Folder folder;
    private SecureData.Encrypted encryptedData;
    private SecureData decryptedData;

    public DataView() {
        swipeArrowLeft = createSwipeArrow(40, true);
        setLeft(swipeArrowLeft);
        BorderPane.setAlignment(swipeArrowLeft, Pos.CENTER_RIGHT);
        BorderPane.setMargin(swipeArrowLeft, new Insets(0, 20, 0, 20));

        swipeArrowRight = createSwipeArrow(40, false);
        setRight(swipeArrowRight);
        BorderPane.setAlignment(swipeArrowRight, Pos.CENTER_RIGHT);
        BorderPane.setMargin(swipeArrowRight, new Insets(0, 20, 0, 20));

        centerContainer = new AnchorPane();
        centerContainer.widthProperty().addListener((_, _, width) -> {
            if (!centerContainer.getChildren().isEmpty()) ((Region) centerContainer.getChildren().getFirst()).setPrefWidth(width.doubleValue());
        });
        centerContainer.heightProperty().addListener((_, _, height) -> {
            if (!centerContainer.getChildren().isEmpty()) ((Region) centerContainer.getChildren().getFirst()).setPrefHeight(height.doubleValue());
        });
        setCenter(centerContainer);
        BorderPane.setMargin(centerContainer, new Insets(50, 0, 50, 0));

        ClosableOverlay.apply(this, centerContainer, () -> {
            if (!swipeArrowLeft.isHover() && !swipeArrowRight.isHover()) {
                setVisible(false);
            }
        });
    }

    public void showData(Configuration.Folder folder, SecureData.Encrypted data) throws Exception {
        centerContainer.getChildren().clear();
        Region region = extractContentFrom(data);
        region.setPrefWidth(centerContainer.getWidth());
        region.setPrefHeight(centerContainer.getHeight());
        centerContainer.getChildren().add(region);
        encryptedData = data;
        this.folder = folder;
    }

    private Region extractContentFrom(SecureData.Encrypted data) throws Exception {
        decryptedData = data.decrypt();
        return switch (data.getType()) {
            case SecureData.TYPE_TEXT ->  {
                TextArea area = new TextArea();
                area.setText(new String(decryptedData.text()));
                area.setWrapText(true);
                Platform.runLater(area::requestFocus);
                yield area;
            }
            default -> new Pane();
        };
    }


    public static StackPane createSwipeArrow(double size, boolean toLeft) {
        double radius = size / 2;

        Circle circle = new Circle(radius);
        circle.getStyleClass().add("arrow-background");

        Polygon arrow = new Polygon();
        arrow.getStyleClass().add("arrow-shape");

        double scale = size / 30.0;

        arrow.getPoints().addAll(
                0.0 * scale, 0.0 * scale,      // Bottom-left
                20.0 * scale, 10.0 * scale,         // Tip
                0.0 * scale, 20.0 * scale,          // Top-left
                7.5 * scale, 10.0 * scale           // Notch
        );

        // Position arrow centered on circle
        arrow.setLayoutX(radius - (15.0 * scale / 2));
        arrow.setLayoutY(radius - (20.0 * scale / 2));

        if (toLeft) arrow.setRotate(180);

        StackPane stack = new StackPane(circle, arrow);
        stack.getStyleClass().add("swipe-arrow");
        stack.pseudoClassStateChanged(PseudoClass.getPseudoClass(toLeft ? "left" : "right"), true);
        stack.setMaxWidth(circle.getRadius() * 2);
        stack.setMaxHeight(circle.getRadius() * 2);
        return stack;
    }
}
