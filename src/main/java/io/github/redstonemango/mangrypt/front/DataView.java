package io.github.redstonemango.mangrypt.front;

import io.github.redstonemango.mangoutils.OperatingSystem;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.*;
import javafx.application.Platform;
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
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class DataView extends BorderPane {

    private final Label nameLabel;
    private final AnchorPane centerContainer;
    private final StackPane swipeArrowLeft;
    private final StackPane swipeArrowRight;

    private List<SecureData.Encrypted> availableData;
    private SecureData.Encrypted encryptedData;
    private @Nullable SecureData decryptedData;
    private boolean changed = false;

    private @Nullable TextArea textArea;
    private @Nullable Image image;
    private @Nullable ImageView imageView;
    private @Nullable MediaPlayer mediaPlayer;
    private @Nullable SecureInMemoryMediaServer mediaServer;

    public DataView() {
        swipeArrowLeft = createSwipeArrow(40, true);
        setLeft(swipeArrowLeft);

        swipeArrowRight = createSwipeArrow(40, false);
        setRight(swipeArrowRight);

        addEventHandler(KeyEvent.KEY_PRESSED, e -> { // Do not register a filter, for we do not want to override the cursor-move event in the text-data-editor
            if (e.getCode() == KeyCode.LEFT && !swipeArrowLeft.isDisabled()) {
                onSwipe(true);
            }
            if (e.getCode() == KeyCode.RIGHT && !swipeArrowRight.isDisabled()) {
                onSwipe(false);
            }
        });

        nameLabel = new Label();
        nameLabel.getStyleClass().add("uncolored-label");
        nameLabel.setFont(Font.font("", FontWeight.BOLD, FontPosture.REGULAR, 30));
        nameLabel.setUnderline(true);
        setTop(nameLabel);
        BorderPane.setAlignment(nameLabel, Pos.CENTER);
        BorderPane.setMargin(nameLabel, new Insets(10, 0, 10, 0));


        centerContainer = new AnchorPane();
        centerContainer.getChildren().add(new Pane());
        setCenter(centerContainer);
        BorderPane.setMargin(centerContainer, new Insets(0, 0, 50, 0));

        Utilities.registerDynamicClosableOverlay(this, () -> {
            storeData();
            setVisible(false);
        }, () -> {
            List<Node> nodes = new ArrayList<>(List.of(swipeArrowLeft, swipeArrowRight, nameLabel));
            if (centerContainer.getChildren().getFirst() instanceof MediaDisplay display) {
                nodes.addAll(display.getUiElementsUnmodifiable());
            }
            else {
                nodes.add(centerContainer.getChildren().getFirst());
            }
            return nodes.toArray(Node[]::new);
        });
        centerContainer.widthProperty().addListener((_, _, width) -> sizeUpdate(width.doubleValue(), true));
        centerContainer.heightProperty().addListener((_, _, height) -> sizeUpdate(height.doubleValue(), false));
    }

    public void showData(List<SecureData.Encrypted> availableData, SecureData.Encrypted data) {
        Node node;
        try {
            node = extractContentFrom(data);
        } catch (Exception e) {
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error extracting content from a dataset", e);
        }
        nameLabel.setText(data.getName());
        nameLabel.setFont(Font.font("", FontWeight.BOLD, data.getName().startsWith(".") ? FontPosture.ITALIC : FontPosture.REGULAR, 30));
        centerContainer.getChildren().clear();
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
        centerContainer.getChildren().add(node);
        encryptedData = data;
        this.availableData = availableData;
        changed = false;
        sizeUpdate(centerContainer.getWidth(), true);
        sizeUpdate(centerContainer.getHeight(), false);

        checkSwipeable();
    }

    private void sizeUpdate(double sideLength, boolean isWidth) {
        // Most times this will be managed automatically by the layout, we just have to manually account for some exceptions
        if (centerContainer.getChildren().getFirst() instanceof ImageView) {
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

    public void storeData() {
        Utilities.ensureAuthorizedAccess(DataView.class, BaseView.class);
        if (decryptedData == null) return;

        if (decryptedData.requiresSave() && changed) {
            try {
                encryptedData.store(decryptedData);
            }
            catch (Exception e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error storing secure data", e);
            }
        }

        cleanup();
    }

    private void cleanup() {
        Utilities.ensureAuthorizedAccess(DataView.class);

        decryptedData.zeroOut();
        decryptedData = null;

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
        shutdownMediaServer();
    }
    protected void shutdownMediaServer() {
        Utilities.ensureAuthorizedAccess(DataView.class, BaseView.class);

        if (mediaServer != null) {
            mediaServer.stop();
            mediaServer = null;
        }
    }

    private Node extractContentFrom(SecureData.Encrypted data) throws Exception {
        decryptedData = data.decrypt();
        Node node = switch (data.getType()) {
            case SecureData.TYPE_TEXT ->  {
                textArea = new TextArea();
                textArea.setText(new String(decryptedData.text()));
                textArea.setWrapText(true);
                textArea.textProperty().addListener((_, _, text) -> {
                    if (decryptedData == null) return;
                    decryptedData.text(text.toCharArray());
                    changed = true;
                });
                yield textArea;
            }
            case SecureData.TYPE_IMAGE -> {
                ByteArrayInputStream stream = new ByteArrayInputStream(decryptedData.binaryBytes());
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
            case SecureData.TYPE_MEDIA -> {
                mediaServer = new SecureInMemoryMediaServer(decryptedData.binaryBytes(), decryptedData.mimeType(), 0, "media-stream");
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
        if (e.getType() == MediaException.Type.UNKNOWN && OperatingSystem.isLinux()) {
            ButtonType learnMore = new ButtonType("Learn more");
            Mangrypt.getBase().showAlert(Alert.AlertType.ERROR, "Unable to create media", "This could be due to missing codec on your Linux system", true, btn -> {
                if (btn == learnMore) {
                    OperatingSystem.loadCurrentOS().open("https://www.oracle.com/java/technologies/javase/products-doc-jdk8-jre8-certconfig.html#:~:text=decoding%20will%20fail.-,Linux,on%20Ubuntu%20Linux%2012.04%20or%20equivalent.");
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
        storeData();

        int index = availableData.indexOf(encryptedData);
        int newIndex = index + (toLeft ? -1 : 1);
        showData(availableData, availableData.get(newIndex));
    }

    private void checkSwipeable() {
        int index = availableData.indexOf(encryptedData);
        swipeArrowLeft.setDisable(index <= 0);
        swipeArrowRight.setDisable(index >= availableData.size() - 1);
    }


    private StackPane createSwipeArrow(double size, boolean toLeft) {
        double radius = size / 2;

        Circle circle = new Circle(radius);
        circle.getStyleClass().add("arrow-background");
        circle.setOnMouseClicked(_ -> onSwipe(toLeft));

        Polygon arrow = new Polygon();
        arrow.getStyleClass().add("arrow-shape");

        double scale = size / 30.0;

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
        stack.setMaxWidth(circle.getRadius() * 2);
        stack.setMaxHeight(circle.getRadius() * 2);

        BorderPane.setAlignment(stack, Pos.CENTER_RIGHT);
        BorderPane.setMargin(stack, new Insets(0, 20, 0, 20));
        Utilities.registerHoverAnimation(arrow);
        stack.setCursor(Cursor.HAND);

        return stack;
    }
}
