package io.github.redstonemango.mangrypt.front;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

public class DefaultPaneBackground {

    public static final Image TOP_LEFT = new Image(DefaultPaneBackground.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/default-pane/top-left.png"));
    public static final Image TOP_CENTER = new Image(DefaultPaneBackground.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/default-pane/top-center.png"));
    public static final Image TOP_RIGHT = new Image(DefaultPaneBackground.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/default-pane/top-right.png"));

    public static final Image CENTER_LEFT = new Image(DefaultPaneBackground.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/default-pane/center-left.png"));
    public static final Image CENTER = new Image(DefaultPaneBackground.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/default-pane/center.png"));
    public static final Image CENTER_RIGHT = new Image(DefaultPaneBackground.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/default-pane/center-right.png"));

    public static final Image BOTTOM_LEFT = new Image(DefaultPaneBackground.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/default-pane/bottom-left.png"));
    public static final Image BOTTOM_CENTER = new Image(DefaultPaneBackground.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/default-pane/bottom-center.png"));
    public static final Image BOTTOM_RIGHT = new Image(DefaultPaneBackground.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/default-pane/bottom-right.png"));

    private final ImageView topCenter = new ImageView(TOP_CENTER);
    private final ImageView topRight = new ImageView(TOP_RIGHT);

    private final ImageView centerLeft = new ImageView(CENTER_LEFT);
    private final ImageView center = new ImageView(CENTER);
    private final ImageView centerRight = new ImageView(CENTER_RIGHT);

    private final ImageView bottomLeft = new ImageView(BOTTOM_LEFT);
    private final ImageView bottomCenter = new ImageView(BOTTOM_CENTER);
    private final ImageView bottomRight = new ImageView(BOTTOM_RIGHT);

    private final Pane container;

    public DefaultPaneBackground(Pane container) {
        this.container = container;
        ImageView topLeft = new ImageView(TOP_LEFT); // Does not have to be populated, for the layout is top-left anchored
        container.getChildren().add(topLeft);
        container.getChildren().add(topCenter);
        container.getChildren().add(topRight);
        container.getChildren().add(centerLeft);
        container.getChildren().add(center);
        container.getChildren().add(centerRight);
        container.getChildren().add(bottomLeft);
        container.getChildren().add(bottomCenter);
        container.getChildren().add(bottomRight);
    }

    public Rectangle getContentArea() {
        return new Rectangle(center.getTranslateX(), center.getTranslateY(), center.getFitWidth(), center.getFitHeight());
    }

    public void update() {
        double contentWidth = container.getScene().getWidth();
        double contentHeight = container.getScene().getHeight();

        // Edges
        bottomLeft.setTranslateY(Math.max(0, contentHeight - BOTTOM_LEFT.getHeight()));
        topRight.setTranslateX(Math.max(0, contentWidth - TOP_RIGHT.getWidth()));
        bottomRight.setTranslateX(Math.max(0, contentWidth - BOTTOM_RIGHT.getWidth()));
        bottomRight.setTranslateY(Math.max(0, contentHeight - BOTTOM_RIGHT.getHeight()));

        // Spaces in border - Position
        centerLeft.setTranslateY(Math.max(0, TOP_LEFT.getHeight()));
        topCenter.setTranslateX(Math.max(0, TOP_LEFT.getWidth()));
        centerRight.setTranslateX(Math.max(0, contentWidth - CENTER_RIGHT.getWidth()));
        centerRight.setTranslateY(Math.max(0, TOP_RIGHT.getHeight()));
        bottomCenter.setTranslateX(Math.max(0, BOTTOM_LEFT.getWidth()));
        bottomCenter.setTranslateY(Math.max(0, contentHeight - BOTTOM_CENTER.getHeight()));

        // Spaces in border - Stretch
        centerLeft.setFitHeight(Math.max(1, contentHeight - TOP_LEFT.getHeight() - BOTTOM_LEFT.getHeight()));
        topCenter.setFitWidth(Math.max(1, contentWidth - TOP_LEFT.getWidth() - TOP_RIGHT.getWidth()));
        centerRight.setFitHeight(Math.max(1, contentHeight - TOP_RIGHT.getHeight() - BOTTOM_RIGHT.getHeight()));
        bottomCenter.setFitWidth(Math.max(1, contentWidth - BOTTOM_LEFT.getWidth() - BOTTOM_RIGHT.getWidth()));

        // Center
        center.setTranslateX(Math.max(0, CENTER_LEFT.getWidth()));
        center.setTranslateY(Math.max(0, TOP_CENTER.getHeight()));
        center.setFitWidth(Math.max(1, contentWidth - CENTER_LEFT.getWidth() - CENTER_RIGHT.getWidth()));
        center.setFitHeight(Math.max(1, contentHeight - TOP_CENTER.getHeight() - BOTTOM_CENTER.getHeight()));
    }
}
