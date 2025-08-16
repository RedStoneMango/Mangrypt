package io.github.redstonemango.mangrypt.logic;

import io.github.redstonemango.mangrypt.Mangrypt;
import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Utilities {

    public static final StackWalker WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static void ensureAuthorizedAccess(Class<?>... authorizedClasses) throws SecurityException {
        String methodName = WALKER.walk(frames ->
                frames.skip(1).findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse("unknownMethod"));

        List<Class<?>> classes = Arrays.asList(authorizedClasses);

        boolean trustedCaller = WALKER.walk(frames ->
                frames.skip(1).anyMatch(frame -> {
                    Class<?> caller = frame.getDeclaringClass();
                    if (!classes.contains(caller)) {
                        return false;
                    }
                    int index = classes.indexOf(caller);
                    return classes.get(index).getClassLoader().equals(caller.getClassLoader());
                })
        );


        if (!trustedCaller) {
            throw new SecurityException("Unauthorized access to method '" + methodName + "'");
        }
    }

    public static <T> void applyCustomNodeCellFactory(ListView<T> listView, Function<T, Node> nodeFunction) {
        applyCustomNodeCellFactory(listView, nodeFunction, _ -> {});
    }

    public static <T> void applyCustomNodeCellFactory(ListView<T> listView, Function<T, Node> nodeFunction, Consumer<T> onDoubleClick) {
        listView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<T> call(ListView<T> lv) {
                return new ListCell<>() {

                    private long lastClick = -1;

                    @Override
                    protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setGraphic(nodeFunction.apply(item));
                            setPadding(new Insets(0));
                            setOnMouseClicked(_ -> {
                                if (System.currentTimeMillis() - lastClick <= 250) onDoubleClick.accept(getItem());
                                lastClick = System.currentTimeMillis();
                            });
                        }
                    }
                };
            }
        });
    }

    public static void registerHoverAnimation(Node node) {
        node.getParent().setOnMouseEntered(_ -> {
            FadeTransition transition = new FadeTransition(Duration.millis(250), node);
            transition.setFromValue(1);
            transition.setToValue(0.45);
            transition.play();
        });
        node.getParent().setOnMouseExited(_ -> {
            FadeTransition transition = new FadeTransition(Duration.millis(250), node);
            transition.setFromValue(0.45);
            transition.setToValue(1);
            transition.play();
        });
    }

    public static void registerClosableOverlay(Pane root, Runnable onCancel, Node... allowedNodes) {
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                onCancel.run();
            }
        });
        root.setOnMousePressed(e -> {
            boolean inRegion = false;

            for (Node allowedNode : allowedNodes) {
                Point2D scenePos = allowedNode.localToScene(0, 0);
                double width = allowedNode.getLayoutBounds().getWidth();
                double height = allowedNode.getLayoutBounds().getHeight();
                if (e.getSceneX() >= scenePos.getX()
                        && e.getSceneX() < scenePos.getX() + width
                        && e.getSceneY() >= scenePos.getY()
                        && e.getSceneY() < scenePos.getY() + height)
                {
                    inRegion = true;
                }
            }

            if (!inRegion) onCancel.run();
        });
    }
    public static void registerDynamicClosableOverlay(Pane root, Runnable onCancel, Supplier<Node[]> allowedNodesSupplier) {
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                onCancel.run();
            }
        });
        root.setOnMousePressed(e -> {
            boolean inRegion = false;

            for (Node allowedNode : allowedNodesSupplier.get()) {
                Point2D scenePos = allowedNode.localToScene(0, 0);
                double width = allowedNode.getLayoutBounds().getWidth();
                double height = allowedNode.getLayoutBounds().getHeight();
                if (e.getSceneX() >= scenePos.getX()
                        && e.getSceneX() < scenePos.getX() + width
                        && e.getSceneY() >= scenePos.getY()
                        && e.getSceneY() < scenePos.getY() + height)
                {
                    inRegion = true;
                }
            }

            if (!inRegion) onCancel.run();
        });
    }

    public static void showConfigureDialog(ImageView configureImage, BooleanProperty showHiddenContentProperty, boolean folderOverview) {
        CheckMenuItem showHiddenFoldersItem = new CheckMenuItem("Show hidden " + (folderOverview ? "folders" : "datasets"));
        MenuItem passwordChangeItem = new MenuItem("Change password & passphrase");
        MenuItem backMenuItem = new MenuItem("Back to vault overview");
        ContextMenu menu = new ContextMenu(showHiddenFoldersItem, passwordChangeItem, new SeparatorMenuItem(), backMenuItem);
        Point2D imagePos = configureImage.localToScreen(0, 0);
        menu.show(configureImage.getParent(), imagePos.getX(), imagePos.getY());
        if (folderOverview) menu.setX(imagePos.getX() - menu.getWidth());
        else menu.setX(imagePos.getX() + configureImage.getFitWidth());

        showHiddenFoldersItem.setSelected(showHiddenContentProperty.get());
        showHiddenFoldersItem.selectedProperty().addListener((_, _, b) -> showHiddenContentProperty.set(b));

        passwordChangeItem.setOnAction(_ -> {
            try {
                FXMLLoader loader = new FXMLLoader(Utilities.class.getResource("/io/github/redstonemango/mangrypt/fxml/security-setup.fxml"));
                Mangrypt.getBase().setSecondLayerRoot(loader.load());
            }
            catch (IOException e) {
                throw new RuntimeException(e); // I love happy compilers
            }
        });

        backMenuItem.setOnAction(_ -> Mangrypt.getBase().playTransition(() -> {
            ConfigIO.save();
            ConfigIO.cleanup();

            try {
                FXMLLoader loader = new FXMLLoader(Utilities.class.getResource("/io/github/redstonemango/mangrypt/fxml/vault-selection.fxml"));
                Mangrypt.getBase().setSceneRoot(loader.load());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public static String getSupportedMimeType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "mp3" -> "audio/mpeg";
            case "aac" -> "audio/aac";
            case "wav" -> "audio/wav";
            case "mp4" -> "video/mp4";
            default -> throw new IllegalArgumentException("Extension not supported: " + ext);
        };
    }
}
