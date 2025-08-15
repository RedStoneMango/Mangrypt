package io.github.redstonemango.mangrypt.graphic;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.logic.*;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.List;

public class DataView extends BorderPane {

    private final Label nameLabel;
    private final AnchorPane centerContainer;
    private final StackPane swipeArrowLeft;
    private final StackPane swipeArrowRight;
    private final Runnable onClose = () -> {
        storeData();
        setVisible(false);
    };

    private List<SecureData.Encrypted> availableData;
    private SecureData.Encrypted encryptedData;
    private @Nullable SecureData decryptedData;
    private boolean changed = false;

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

        Utilities.registerDynamicClosableOverlay(this, onClose, () -> new Node[]{swipeArrowLeft, swipeArrowRight, centerContainer.getChildren().getFirst()});
        centerContainer.widthProperty().addListener((_, _, width) -> sizeUpdate(width.doubleValue(), true, false));
        centerContainer.heightProperty().addListener((_, _, height) -> sizeUpdate(height.doubleValue(), false, false));
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
        sizeUpdate(centerContainer.getWidth(), true, false);
        sizeUpdate(centerContainer.getHeight(), false, true);

        checkSwipeable();
    }

    // TODO: The centering logic doesn't work reliably yet
    private void sizeUpdate(double sideLength, boolean isWidth, boolean updateVisibility) {
        // Most times this will be managed automatically by the layout, we just have to manually account for some exceptions
        if (centerContainer.getChildren().getFirst() instanceof ImageView imageView) {
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
        decryptedData.zeroOut();
        decryptedData = null;
    }

    private Node extractContentFrom(SecureData.Encrypted data) throws Exception {
        decryptedData = data.decrypt();
        Node node = switch (data.getType()) {
            case SecureData.TYPE_TEXT ->  {
                TextArea area = new TextArea();
                area.setText(new String(decryptedData.text()));
                area.setWrapText(true);
                area.textProperty().addListener((_, _, text) -> {
                    changed = true;
                    decryptedData.text(text.toCharArray());
                });
                yield area;
            }
            case SecureData.TYPE_IMAGE -> {
                ByteArrayInputStream stream = new ByteArrayInputStream(decryptedData.imageBytes());
                Image image = new Image(stream);
                if (image.isError()) {
                    yield createErrorDisplay();
                }
                ImageView imageView = new ImageView(image);
                imageView.setPreserveRatio(true);
                imageView.setManaged(false);
                imageView.setSmooth(true);
                imageView.setVisible(false); // Prevent image pos jump on first load
                yield imageView;
            }
            default -> createErrorDisplay();
        };
        Platform.runLater(centerContainer::requestFocus);
        return node;
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
