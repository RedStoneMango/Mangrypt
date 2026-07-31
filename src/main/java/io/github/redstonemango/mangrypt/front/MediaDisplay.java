package io.github.redstonemango.mangrypt.front;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Nullable;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaEventAdapter;
import uk.co.caprica.vlcj.media.MediaParsedStatus;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wrapper class for a {@link javafx.scene.media.MediaPlayer}. Displays basic flw controls for the media and shows video graphics if a video track exists in the player's underlying {@link javafx.scene.media.Media} object.
 */
public class MediaDisplay extends VBox {

    private final EmbeddedMediaPlayer player;
    private final MediaPlayerFactory factory;
    private String maxTime = "--:--";
    private @Nullable Consumer<@Nullable ImageView> mediaViewAction;

    public MediaDisplay(String url) {
        factory = new MediaPlayerFactory();
        player = factory.mediaPlayers().newEmbeddedMediaPlayer();

        Label timeLabel = new Label("--:-- / --:--");
        VBox.setMargin(timeLabel, new Insets(5, 0, 0, 0));
        timeLabel.getStyleClass().add("uncolored-label");
        Slider durationSlider = new Slider(0, 0, 0);
        durationSlider.valueProperty().addListener((_, _, value) -> {
            if (durationSlider.isValueChanging()) {
                player.controls().setTime(value.longValue());
            }
        });
        durationSlider.setDisable(true);
        VBox.setMargin(durationSlider, new Insets(0, 5, 0, 5));
        Button pauseButton = new Button(">");
        pauseButton.setPrefWidth(33);
        pauseButton.setPrefHeight(33);
        pauseButton.setFocusTraversable(false);
        pauseButton.setOnAction(_ -> {
            if (!player.status().isPlaying()) {
                player.controls().play();
            }
            else {
                player.controls().pause();
            }
        });
        Button forwardButton = new Button("=>");
        forwardButton.setPrefWidth(33);
        forwardButton.setPrefHeight(33);
        forwardButton.setFocusTraversable(false);
        forwardButton.setDisable(true);
        forwardButton.setOnAction(_ ->
            player.controls().skipTime(10 * 1000L)
        );
        Button backardButton = new Button("<=");
        backardButton.setPrefWidth(33);
        backardButton.setPrefHeight(33);
        backardButton.setFocusTraversable(false);
        backardButton.setDisable(true);
        backardButton.setOnAction(_ ->
            player.controls().skipTime(-(10 * 1000L))
        );
        Button stopButton = new Button("O");
        stopButton.setPrefWidth(33);
        stopButton.setPrefHeight(33);
        stopButton.setFocusTraversable(false);
        stopButton.setDisable(true);
        stopButton.setOnAction(_ ->
            player.controls().stop()
        );
        Region spacingRegion = new Region();
        HBox.setHgrow(spacingRegion, Priority.ALWAYS);
        Region spacingRegion_ = new Region();
        HBox.setHgrow(spacingRegion_, Priority.ALWAYS);
        Region _spacingRegion = new Region();
        HBox.setHgrow(_spacingRegion, Priority.ALWAYS);
        Slider volumeSlider = new Slider(0, 125, 100);
        volumeSlider.valueProperty().addListener((_, _, volume) ->
            player.audio().setVolume(volume.intValue())
        );
        Label l1 = new Label("0 %");
        l1.getStyleClass().add("uncolored-label");
        Label l2 = new Label("125 %");
        l2.getStyleClass().add("uncolored-label");

        HBox buttonBox = new HBox(spacingRegion_, backardButton, pauseButton, stopButton, forwardButton, spacingRegion, l1, volumeSlider,  l2, _spacingRegion);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setSpacing(10);
        VBox.setMargin(buttonBox, new Insets(0, 0, 5, 0));

        final ImageView[] videoView = {new ImageView()};
        videoView[0].setPreserveRatio(true);
        StackPane pane = new StackPane(videoView[0]);
        pane.setAlignment(Pos.CENTER);
        pane.setMinSize(0, 0);
        videoView[0].fitHeightProperty().bind(pane.heightProperty());
        videoView[0].fitWidthProperty().bind(pane.widthProperty());
        VBox.setVgrow(pane, Priority.ALWAYS);
        player.videoSurface().set(new ImageViewVideoSurface(videoView[0]));

        player.events().addMediaEventListener(new MediaEventAdapter() {
            @Override
            public void mediaParsedChanged(Media media, MediaParsedStatus newStatus) {
                Platform.runLater(() -> {
                    if (newStatus == MediaParsedStatus.DONE) {
                        player.audio().setVolume(100);
                        if (!player.media().info().videoTracks().isEmpty()) {
                            getChildren().addFirst(pane);

//                            pane.widthProperty().addListener((_, _, val) ->
//                                    sizeUpdate(true, val.doubleValue(), videoView[0], pane));
//                            pane.heightProperty().addListener((_, _, val) ->
//                                    sizeUpdate(false, val.doubleValue(), videoView[0], pane));

                        } else {
                            videoView[0] = null;
                        }
                        if (mediaViewAction != null) {
                            mediaViewAction.accept(videoView[0]);
                        }
                    }
                });
            }

            @Override
            public void mediaDurationChanged(Media media, long newDuration) {
                Platform.runLater(() -> {
                    maxTime = formatDuration(newDuration);
                    timeLabel.setText("00:00 / " + maxTime);
                    durationSlider.setMax(newDuration);
                });
            }
        });

        player.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> {
                    stopButton.setDisable(true);
                    forwardButton.setDisable(true);
                    backardButton.setDisable(true);
                    timeLabel.setText("00:00 / " + maxTime);
                    pauseButton.setText(">");
                    durationSlider.setDisable(true);
                });
            }

            @Override
            public void playing(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> {
                    stopButton.setDisable(false);
                    forwardButton.setDisable(false);
                    backardButton.setDisable(false);
                    pauseButton.setText("||");
                    durationSlider.setDisable(false);
                });
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> pauseButton.setText(">"));
            }

            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                Platform.runLater(() -> {
                    if (!durationSlider.isValueChanging()) durationSlider.setValue(newTime);
                    timeLabel.setText(formatDuration(newTime) + " / " + maxTime);
                });
            }
        });
        player.media().startPaused(url);

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
    void tryUseMediaView(Consumer<@Nullable ImageView> mediaViewAction) {
        this.mediaViewAction = mediaViewAction;
    }
    public static String formatDuration(long millis) {
        long totalSeconds = millis / 1000;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public void dispose() {
        player.release();
        factory.release();
    }
}
