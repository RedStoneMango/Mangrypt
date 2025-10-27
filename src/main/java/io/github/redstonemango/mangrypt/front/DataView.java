package io.github.redstonemango.mangrypt.front;

import io.github.redstonemango.mangoutils.OperatingSystem;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.*;
import io.github.redstonemango.mangrypt.back.dataTypes.*;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DataView extends BorderPane {

    private final Label nameLabel;
    private final AnchorPane centerContainer;
    private final StackPane swipeArrowLeft;
    private final StackPane swipeArrowRight;
    private final BorderPane topPane;

    private List<DataElement> availableData;
    private DataElement currentData;

    private @Nullable TextArea textArea;
    private @Nullable Image image;
    private @Nullable ImageView imageView;
    private @Nullable MediaPlayer mediaPlayer;
    private @Nullable SecureInMemoryMediaServer mediaServer;

    private boolean isShowingData = false;
    private long lastMouseMoved = -1;
    private @Nullable ScheduledService<Void> checkMouseHideService = null;

    public DataView() {
        swipeArrowLeft = createSwipeArrow(true);
        setLeft(swipeArrowLeft);

        swipeArrowRight = createSwipeArrow(false);
        setRight(swipeArrowRight);

        addEventHandler(KeyEvent.KEY_PRESSED, e -> { // Do not register a filter, for we do not want to override the caret-move event in the text-data-editor
            if (e.getCode() == KeyCode.LEFT && !swipeArrowLeft.isDisabled()) {
                onSwipe(true);
            }
            if (e.getCode() == KeyCode.RIGHT && !swipeArrowRight.isDisabled()) {
                onSwipe(false);
            }
        });

        topPane = new BorderPane();
        setTop(topPane);

        nameLabel = new Label();
        nameLabel.getStyleClass().add("uncolored-label");
        nameLabel.setFont(Font.font("", FontWeight.BOLD, FontPosture.REGULAR, 30));
        nameLabel.setUnderline(true);
        topPane.setCenter(nameLabel);
        BorderPane.setAlignment(nameLabel, Pos.CENTER);
        BorderPane.setMargin(nameLabel, new Insets(10, 0, 10, 0));


        centerContainer = new AnchorPane();
        centerContainer.getChildren().add(new Pane());
        setCenter(centerContainer);
        BorderPane.setMargin(centerContainer, new Insets(0, 0, 50, 0));

        Utilities.registerDynamicClosableOverlay(this, () -> {
            cleanup();
            setVisible(false);
        }, () -> {
            List<Node> nodes = new ArrayList<>(List.of(swipeArrowLeft, swipeArrowRight, nameLabel));
            nodes.add(topPane.getRight()); // BG toggle node
            if (centerContainer.getChildren().getFirst() instanceof MediaDisplay display) {
                nodes.addAll(display.getUiElementsUnmodifiable());
            }
            else {
                nodes.add(centerContainer.getChildren().getFirst());
            }
            if (ConfigIO.getConfig().renderDataBG()) nodes.add(centerContainer);
            return nodes.toArray(Node[]::new);
        });
        centerContainer.widthProperty().addListener((_, _, width) -> sizeUpdate(width.doubleValue(), true));
        centerContainer.heightProperty().addListener((_, _, height) -> sizeUpdate(height.doubleValue(), false));
    }

    private void updateBg(boolean active) {
        StackPane node = createBgToggleNode(active);
        topPane.setRight(node);
        BorderPane.setMargin(topPane.getCenter(), new Insets(0, 0, 0, 50)); // Align label (because of bg toggle node)

        centerContainer.setBackground(
                active ?
                new Background(new BackgroundFill(Color.BLACK, new CornerRadii(20), Insets.EMPTY))
                :
                Background.EMPTY
        );
        ConfigIO.markShouldSave();
    }

    public void showData(List<DataElement> availableData, DataElement data) {
        updateBg(ConfigIO.getConfig().renderDataBG());

        Node node;
        try {
            node = extractContentFrom(data);
        } catch (Exception e) {
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error extracting content from a dataset", e);
        }
        isShowingData = true;
        nameLabel.setText(data.getName());
        nameLabel.setFont(Font.font("", FontWeight.BOLD, data.getName().startsWith(".") ? FontPosture.ITALIC : FontPosture.REGULAR, 30));
        centerContainer.getChildren().clear();
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
        centerContainer.getChildren().add(node);
        currentData = data;
        this.availableData = availableData;
        sizeUpdate(centerContainer.getWidth(), true);
        sizeUpdate(centerContainer.getHeight(), false);

        tryUseHiddenCursorNode(node, hiddenCursorNode -> {
            if (checkMouseHideService != null) {
                checkMouseHideService.cancel();
                checkMouseHideService = null;
            }

            if (hiddenCursorNode != null) {
                hiddenCursorNode.setOnMouseMoved(_ -> {
                    lastMouseMoved = System.currentTimeMillis();
                    if (!checkHideCursor() && hiddenCursorNode.getCursor() != null) {
                        hiddenCursorNode.setCursor(null);
                    }
                });

                checkMouseHideService = new ScheduledService<>() {
                    @Override
                    protected Task<Void> createTask() {
                        return new Task<>() {
                            @Override
                            protected Void call() {
                                if (checkHideCursor() && hiddenCursorNode.getCursor() == null) {
                                    hiddenCursorNode.setCursor(Cursor.NONE);
                                }
                                return null;
                            }
                        };
                    }
                };
                checkMouseHideService.setPeriod(Duration.seconds(3));
                checkMouseHideService.start();
            }
            else {
                lastMouseMoved = System.currentTimeMillis();
            }
        });

        checkSwipeable();
    }

    private boolean checkHideCursor() {
        return System.currentTimeMillis() - lastMouseMoved >= 5000;
    }

    private void tryUseHiddenCursorNode(Node node, Consumer<@Nullable Node> action) {
        if (node instanceof ImageView) action.accept(node);
        else if (node instanceof MediaDisplay mediaDisplay) mediaDisplay.tryUseMediaView(action::accept);
        else action.accept(null);
    }

    private void sizeUpdate(double sideLength, boolean isWidth) {
        if (centerContainer.getChildren().isEmpty()) return;
        // Most times this will be managed automatically by the layout, we just have to manually account for some exceptions
        if (centerContainer.getChildren().getFirst() instanceof ImageView && imageView != null) {
            if (isWidth) {
                imageView.setFitWidth(sideLength);
            }
            else {
                imageView.setFitHeight(sideLength);
            }

            Platform.runLater(() -> {
                Bounds newBounds = imageView.getLayoutBounds();

                double containerWidth = centerContainer.getWidth();
                double containerHeight = centerContainer.getHeight();

                double offsetX = (containerWidth - newBounds.getWidth()) / 2;
                double offsetY = (containerHeight - newBounds.getHeight()) / 2;

                imageView.setLayoutX(offsetX);
                imageView.setLayoutY(offsetY);

                imageView.setVisible(true); // Prevent image pos jump on first load
            });
        }
    }

    public void cleanup() {
        Utilities.ensureAuthorizedAccess(DataView.class, BaseView.class);
        isShowingData = false;

        if (textArea != null) {
            textArea.setText("");
            textArea = null;
        }

        if (image != null) {
            image = null;
        }
        if (imageView != null) {
            imageView = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.dispose();
            mediaPlayer = null;
        }

        if (Platform.isFxApplicationThread()) {
            centerContainer.getChildren().clear();
        }
        shutdownMediaServer();
    }
    protected void shutdownMediaServer() {
        Utilities.ensureAuthorizedAccess(DataView.class, BaseView.class);

        if (mediaServer != null) {
            mediaServer.stop();
            mediaServer = null;
        }
    }

    private Node extractContentFrom(DataElement data) throws Exception {
        Node node = switch (data) {
            case TextDataElement textData ->  {
                textArea = new TextArea();
                textArea.setText(new String(textData.bytes(), StandardCharsets.UTF_8));
                textArea.setWrapText(true);
                textArea.textProperty().addListener((_, _, text) -> {
                    if (isShowingData) {
                        textData.bytes(text.getBytes(StandardCharsets.UTF_8));
                        ConfigIO.markShouldSave();
                    }
                });
                yield textArea;
            }
            case ImageDataElement imageData -> {
                ByteArrayInputStream stream = new ByteArrayInputStream(imageData.bytes());
                image = new Image(stream);
                if (image.isError()) {
                    yield createErrorDisplay();
                }
                imageView = new ImageView(image);
                imageView.setPreserveRatio(true);
                imageView.setManaged(false);
                imageView.setSmooth(true);
                imageView.setVisible(false); // Prevent image pos jump on first load
                yield imageView;
            }
            case MediaDataElement mediaData -> {
                mediaServer = new SecureInMemoryMediaServer(mediaData.bytes(), mediaData.mimeType(), 0, "media-stream");
                mediaServer.start();
                Media media;
                try {
                    media = new Media(mediaServer.getTokenizedUrl());
                }
                catch (MediaException e) {
                    mediaServer.stop();
                    tryShowCodecMessage(e);
                    yield createErrorDisplay();
                }
                try {
                    mediaPlayer = new MediaPlayer(media);
                }
                catch (MediaException e) {
                    mediaServer.stop();
                    tryShowCodecMessage(e);
                    yield createErrorDisplay();
                }
                yield new MediaDisplay(mediaPlayer);
            }
            default -> createErrorDisplay();
        };
        Platform.runLater(centerContainer::requestFocus);
        return node;
    }

    private static void tryShowCodecMessage(MediaException e) {
        if (e.getType() == MediaException.Type.UNKNOWN) {
            ButtonType learnMore = new ButtonType("Learn more");
            Mangrypt.getBase().showAlert(Alert.AlertType.ERROR, "Unable to create media", "This could be due to missing system libraries on your device", true, btn -> {
                if (btn == learnMore) {
                    OperatingSystem.loadCurrentOS().open("https://www.oracle.com/java/technologies/javase/products-doc-jdk8-jre8-certconfig.html#:~:text=JavaFX%20Media,12.04%20or%20equivalent.");
                }
            }, learnMore, ButtonType.CLOSE);
        }
    }

    private FlowPane createErrorDisplay() {
        Label label = new Label("Invalid or corrupted data");
        label.setFont(Font.font("", FontWeight.NORMAL, FontPosture.REGULAR, 20));
        label.setTextFill(Color.RED);
        FlowPane flowPane = new FlowPane();
        flowPane.setAlignment(Pos.CENTER);
        return flowPane;
    }

    private void onSwipe(boolean toLeft) {
        cleanup();

        int index = availableData.indexOf(currentData);
        int newIndex = index + (toLeft ? -1 : 1);
        showData(availableData, availableData.get(newIndex));
    }

    private void checkSwipeable() {
        int index = availableData.indexOf(currentData);
        swipeArrowLeft.setDisable(index <= 0);
        swipeArrowRight.setDisable(index >= availableData.size() - 1);
    }


    private StackPane createSwipeArrow(boolean toLeft) {
        final double SIZE = 40;

        double radius = SIZE / 2;

        Circle circle = new Circle(radius);
        circle.getStyleClass().add("circle-background");
        circle.setOnMouseClicked(_ -> onSwipe(toLeft));

        Polygon arrow = new Polygon();
        arrow.getStyleClass().add("arrow-shape");

        double scale = SIZE / 30.0;

        arrow.getPoints().addAll(
                0.0 * scale, 0.0 * scale,      // Bottom-left
                20.0 * scale, 10.0 * scale,         // Tip
                0.0 * scale, 20.0 * scale,          // Top-left
                7.5 * scale, 10.0 * scale           // Notch
        );
        arrow.setLayoutX(radius - (15.0 * scale / 2));
        arrow.setLayoutY(radius - (20.0 * scale / 2));
        arrow.setOnMouseClicked(_ -> onSwipe(toLeft));

        if (toLeft) arrow.setRotate(180);

        StackPane stack = new StackPane(circle, arrow);
        stack.getStyleClass().add("swipe-arrow");
        stack.pseudoClassStateChanged(PseudoClass.getPseudoClass(toLeft ? "left" : "right"), true);
        stack.setMaxWidth(SIZE);
        stack.setMaxHeight(SIZE);

        BorderPane.setAlignment(stack, Pos.CENTER_RIGHT);
        BorderPane.setMargin(stack, new Insets(0, 20, 0, 20));
        Utilities.registerHoverAnimation(arrow);
        stack.setCursor(Cursor.HAND);

        return stack;
    }

    private StackPane createBgToggleNode(boolean isActive) {
        final double SIZE = 40;

        double radius = SIZE / 2;

        Circle circle = new Circle(radius);
        circle.getStyleClass().add("circle-background");

        Image image = new Image(getClass().getResource("/io/github/redstonemango/mangrypt/image/background-" +
                (isActive ? "on" : "off") + ".png").toExternalForm());
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(SIZE);
        imageView.setMouseTransparent(true);

        StackPane stack = new StackPane(circle, imageView);
        stack.setMaxWidth(SIZE);
        stack.setMaxHeight(SIZE);
        stack.setOnMouseClicked(_ -> {
            Configuration config = ConfigIO.getConfig();

            config.renderDataBG(!config.renderDataBG());
            updateBg(config.renderDataBG());
        });

        BorderPane.setMargin(stack, new Insets(10, 10, 0, 0));
        Utilities.registerHoverAnimation(imageView);
        stack.setCursor(Cursor.HAND);

        return stack;
    }
}
