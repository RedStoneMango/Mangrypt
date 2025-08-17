package io.github.redstonemango.mangrypt.graphic;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.VideoTrack;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper class for a {@link javafx.scene.media.MediaPlayer}. Displays basic flw controls for the media and shows video graphics if a video track exists in the player's underlying {@link javafx.scene.media.Media} object.
 */
public class MediaDisplay extends VBox {

    private String maxTime = "--:--";

    public MediaDisplay(MediaPlayer player) {
        Label timeLabel = new Label("--:-- / --:--");
        VBox.setMargin(timeLabel, new Insets(5, 0, 0, 0));
        timeLabel.getStyleClass().add("uncolored-label");
        Slider durationSlider = new Slider(0, 0, 0);
        durationSlider.valueProperty().addListener((_, _, value) -> {
            if (durationSlider.isValueChanging()) player.seek(Duration.seconds(value.doubleValue()));
        });
        durationSlider.setDisable(true);
        VBox.setMargin(durationSlider, new Insets(0, 5, 0, 5));
        Button pauseButton = new Button(">");
        pauseButton.setPrefWidth(33);
        pauseButton.setPrefHeight(33);
        pauseButton.setFocusTraversable(false);
        pauseButton.setOnAction(_ -> {
            if (player.getStatus() != MediaPlayer.Status.PLAYING) {
                player.play();
            }
            else {
                player.pause();
            }
        });
        Button forwardButton = new Button("=>");
        forwardButton.setPrefWidth(33);
        forwardButton.setPrefHeight(33);
        forwardButton.setFocusTraversable(false);
        forwardButton.setDisable(true);
        forwardButton.setOnAction(_ -> {
            double secs = player.getCurrentTime().toSeconds();
            double max = player.getMedia().getDuration().toSeconds();
            player.seek(Duration.seconds(Math.min(secs + 10, max)));
        });
        Button backardButton = new Button("<=");
        backardButton.setPrefWidth(33);
        backardButton.setPrefHeight(33);
        backardButton.setFocusTraversable(false);
        backardButton.setDisable(true);
        backardButton.setOnAction(_ -> {
            double secs = player.getCurrentTime().toSeconds();
            player.seek(Duration.seconds(Math.max(secs - 10, 0)));
        });
        Button stopButton = new Button("O");
        stopButton.setPrefWidth(33);
        stopButton.setPrefHeight(33);
        stopButton.setFocusTraversable(false);
        stopButton.setDisable(true);
        stopButton.setOnAction(_ -> {
            player.stop();
        });
        Region spacingRegion = new Region();
        HBox.setHgrow(spacingRegion, Priority.ALWAYS);
        Region spacingRegion_ = new Region();
        HBox.setHgrow(spacingRegion_, Priority.ALWAYS);
        Region _spacingRegion = new Region();
        HBox.setHgrow(_spacingRegion, Priority.ALWAYS);
        Slider volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.valueProperty().addListener((_, _, volume) -> {
            player.setVolume(volume.doubleValue());
        });
        Label l1 = new Label("0 %");
        l1.getStyleClass().add("uncolored-label");
        Label l2 = new Label("100 %");
        l2.getStyleClass().add("uncolored-label");

        HBox buttonBox = new HBox(spacingRegion_, backardButton, pauseButton, stopButton, forwardButton, spacingRegion, l1, volumeSlider,  l2, _spacingRegion);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setSpacing(10);
        VBox.setMargin(buttonBox, new Insets(0, 0, 5, 0));

        player.setOnReady(() -> {
            if (player.getMedia().getTracks().stream().anyMatch(track -> track instanceof VideoTrack)) {
                MediaView mv = new MediaView(player);
                mv.setPreserveRatio(true);
                mv.setManaged(false);
                AnchorPane pane = new AnchorPane(mv);
                VBox.setVgrow(pane, Priority.ALWAYS);
                getChildren().addFirst(pane);
                sizeUpdate(true, pane.getWidth(), mv, pane);
                sizeUpdate(false, pane.getHeight(), mv, pane);

                pane.widthProperty().addListener((_, _, val) -> sizeUpdate(true, val.doubleValue(), mv, pane));
                pane.heightProperty().addListener((_, _, val) -> sizeUpdate(false, val.doubleValue(), mv, pane));
            }

            maxTime = formatDuration(player.getMedia().getDuration());
            timeLabel.setText("00:00 / " + maxTime);
            durationSlider.setMax(player.getMedia().getDuration().toSeconds());
        });

        player.setOnStopped(() -> {
            stopButton.setDisable(true);
            forwardButton.setDisable(true);
            backardButton.setDisable(true);
            timeLabel.setText("--:-- / --:--");
            pauseButton.setText(">");
            durationSlider.setDisable(true);
        });
        player.setOnPlaying(() -> {
            stopButton.setDisable(false);
            forwardButton.setDisable(false);
            backardButton.setDisable(false);
            pauseButton.setText("||");
            durationSlider.setDisable(false);
        });
        player.setOnPaused(() -> pauseButton.setText(">"));
        player.setOnEndOfMedia(() -> {
            player.stop();
            player.seek(Duration.ZERO); // Needs to be done manually
        });
        player.currentTimeProperty().addListener((_, _, currentTime) -> {
            if (!durationSlider.isValueChanging()) durationSlider.setValue(currentTime.toSeconds());
            timeLabel.setText(formatDuration(currentTime) + " / " + maxTime);
        });

        VBox controlsBox = new VBox(timeLabel, durationSlider, buttonBox);
        controlsBox.setBackground(
                new Background(new BackgroundFill(Color.DARKGREEN, new CornerRadii(20), null))
        );
        controlsBox.setAlignment(Pos.CENTER);
        controlsBox.setSpacing(20);

        getChildren().add(controlsBox);
        setAlignment(Pos.CENTER);
        setSpacing(20);
    }

    public List<Node> getUiElementsUnmodifiable() {
        List<Node> es = new ArrayList<>();
        getChildren().forEach(child -> {
            if (child instanceof AnchorPane pane) es.add(pane.getChildren().getFirst());
            else es.add(child);
        });
        return Collections.unmodifiableList(es);
    }
    public static String formatDuration(Duration duration) {
        StringBuilder builder = new StringBuilder();

        int hours = Math.max((int) duration.toHours(), 0);
        int minutes = (int) duration.toMinutes() - (hours * 60);
        int seconds = (int) duration.toSeconds() - (hours * 3600) - (minutes * 60);

        if (hours > 0) builder.append(hours).append(":");

        if (minutes >= 10) builder.append(minutes).append(":");
        else builder.append("0").append(minutes).append(":");

        if (seconds >= 10) builder.append(seconds);
        else builder.append("0").append(seconds);

        return builder.toString();
    }

    private void sizeUpdate(boolean isWidth, double sideLength, MediaView mediaView, AnchorPane pane) {
        if (isWidth) {
            mediaView.setFitWidth(sideLength);
        }
        else {
            mediaView.setFitHeight(sideLength);
        }

        Platform.runLater(() -> {
            Bounds newBounds = mediaView.getLayoutBounds();

            double containerWidth = pane.getWidth();
            double containerHeight = pane.getHeight();

            double offsetX = (containerWidth - newBounds.getWidth()) / 2;
            double offsetY = (containerHeight - newBounds.getHeight()) / 2;

            mediaView.setLayoutX(offsetX);
            mediaView.setLayoutY(offsetY);

            mediaView.setVisible(true);
        });
    }
}
