package io.github.redstonemango.mangrypt.back;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.dataTypes.MediaDataElement;
import io.github.redstonemango.mangrypt.front.FileChooserNode;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Utilities {

    public static final StackWalker WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static void ensureAuthorizedAccess(int skipLayers, Class<?>... authorizedClasses) throws SecurityException {
        String methodName = WALKER.walk(frames ->
                frames.skip(skipLayers).findFirst()
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

    public static void ensureAuthorizedAccess(Class<?>... authorizedClasses) throws SecurityException {
        ensureAuthorizedAccess(2, authorizedClasses); // Skip 2 layers: this method and this method's caller
    }

    public static <T> void applyCustomNodeCellFactory(ListView<T> listView, Function<T, Node> nodeFunction, Insets padding) {
        applyCustomNodeCellFactory(listView, nodeFunction, _ -> {}, padding);
    }

    public static <T> void applyCustomNodeCellFactory(ListView<T> listView, Function<T, Node> nodeFunction,
                                                      Consumer<T> onDoubleClick, Insets padding) {
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
                            setPadding(padding);
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

    public static String getSupportedMimeType(String ext) {
        return switch (ext) {
            case "mp3", ".mp3" -> "audio/mpeg";
            case "aac", ".aac" -> "audio/aac";
            case "wav", ".wav" -> "audio/wav";
            case "mp4", ".mp4" -> "video/mp4";
            default -> throw new IllegalArgumentException("Extension not supported: " + ext);
        };
    }

    public static void showSavingFileChooser(String title, Consumer<File> onSelected, String... extensions) {
        File userHome = new File(System.getProperty("user.home"));
        FileChooserNode chooser = new FileChooserNode(title, true, userHome,
                selectedFile -> {
                    Mangrypt.getBase().setSecondLayerRoot(null);
                    onSelected.accept(selectedFile);
                },
                () -> Mangrypt.getBase().setSecondLayerRoot(null), extensions);
        StackPane background = new StackPane();
        StackPane root = new StackPane();
        chooser.prepareMangryptLayout(root, background);
        Utilities.registerClosableOverlay(root, () -> Mangrypt.getBase().setSecondLayerRoot(null), background);
        Mangrypt.getBase().setSecondLayerRoot(root);
        Platform.runLater(chooser::requestFocus);
    }
}
