package io.github.redstonemango.mangrypt.graphic;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.logic.*;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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


        centerContainer = new AnchorPane();

        setCenter(centerContainer);
        BorderPane.setMargin(centerContainer, new Insets(50, 0, 50, 0));

        SharedLogicManager.registerClosableOverlay(this, () -> {
            storeData();
            setVisible(false);
        }, centerContainer, swipeArrowLeft, swipeArrowRight);
    }

    public void showData(Configuration.Folder folder, SecureData.Encrypted data) {
        Region region;
        try {
            region = extractContentFrom(data);
        } catch (Exception e) {
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error showing an encrypted dataset", e);
        }
        centerContainer.getChildren().clear();
        AnchorPane.setTopAnchor(region, 0.0);
        AnchorPane.setBottomAnchor(region, 0.0);
        AnchorPane.setLeftAnchor(region, 0.0);
        AnchorPane.setRightAnchor(region, 0.0);
        centerContainer.getChildren().add(region);
        encryptedData = data;
        this.folder = folder;
        changed = false;

        checkSwipeable();
    }

    public void storeData() {
        Utilities.ensureAuthorizedAccess(DataView.class, BaseView.class);

        if (changed) {
            try {
                encryptedData.store(decryptedData);
            }
            catch (Exception e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error storing secure data", e);
            }
        }
        decryptedData.zeroOut();
    }

    private Region extractContentFrom(SecureData.Encrypted data) throws Exception {
        decryptedData = data.decrypt();
        Region region = switch (data.getType()) {
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
            default -> new Pane();
        };
        Platform.runLater(region::requestFocus);
        return region;
    }

    private void onSwipe(boolean toLeft) {
        storeData();

        int index = folder.getEncryptedData().indexOf(encryptedData);
        int newIndex = index + (toLeft ? -1 : 1);
        showData(folder, folder.getEncryptedData().get(newIndex));
    }

    private void checkSwipeable() {
        int index = folder.getEncryptedData().indexOf(encryptedData);
        swipeArrowLeft.setDisable(index <= 0);
        swipeArrowRight.setDisable(index >= folder.getEncryptedData().size() - 1);
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
        SharedLogicManager.registerHoverAnimation(arrow);

        return stack;
    }
}
