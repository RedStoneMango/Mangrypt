package io.github.redstonemango.mangrypt.front;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import io.github.redstonemango.mangrypt.front.controller.AuthController;

public class MatrixBackground {

    public static final Image BACKGROUND_SPRITE = new Image(AuthController.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/matrix-rain-sprite.png"));
    public static final int BACKGROUND_SPRITE_SIZE = 400;
    public static final Duration BACKGROUND_SCROLL_DURATION = Duration.seconds(20);

    private boolean shouldPlayBackgroundScroll = false;
    private final Pane backgroundContainer;
    private int oldXCount;
    private int oldYCount;

    public MatrixBackground(Pane backgroundContainer) {
        this.backgroundContainer = backgroundContainer;
    }

    public void update() {
        int width = (int) backgroundContainer.getScene().getWidth();
        int height = (int) backgroundContainer.getScene().getHeight();
        int xCount = Math.ceilDiv(width, BACKGROUND_SPRITE_SIZE);
        int yCount = Math.ceilDiv(height, BACKGROUND_SPRITE_SIZE) + 1; // +1 for scroll buffer

        if (oldXCount != xCount || oldYCount != yCount) {
            backgroundContainer.getChildren().clear();
            for (int i = -1; i < xCount; i++) {
                for (int j = -1; j < yCount; j++) {
                    double x = BACKGROUND_SPRITE_SIZE * i;
                    double y = BACKGROUND_SPRITE_SIZE * j;
                    ImageView imageView = new ImageView(BACKGROUND_SPRITE);
                    imageView.setFitWidth(BACKGROUND_SPRITE_SIZE);
                    imageView.setFitHeight(BACKGROUND_SPRITE_SIZE);
                    imageView.setX(x);
                    imageView.setY(y);
                    backgroundContainer.getChildren().add(imageView);
                }
            }
        }
        oldXCount = xCount;
        oldYCount = yCount;
    }

     public boolean playScroll() {
        if (shouldPlayBackgroundScroll) return false; // If already playing, don't start again
        shouldPlayBackgroundScroll = true;

        TranslateTransition scroll = new TranslateTransition(BACKGROUND_SCROLL_DURATION, backgroundContainer);
        scroll.setByY(BACKGROUND_SPRITE_SIZE);
        scroll.setInterpolator(Interpolator.LINEAR);
        scroll.setOnFinished(_ -> {
            backgroundContainer.setTranslateY(0);
            if (shouldPlayBackgroundScroll) scroll.playFromStart();
        });
        scroll.play();
        return true;
    }

    public void stopScroll() {
        shouldPlayBackgroundScroll = false;
    }

    public boolean playsScroll() {
        return shouldPlayBackgroundScroll;
    }

}
