package io.github.redstonemango.mangrypt.front;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class FileChooserNode extends VBox {

    private final TreeView<File> view;
    private final TextField pathField;
    private Button doneButton;
    private ContextMenu pathCompletionMenu;
    private ListView<String> pathCompletionList;
    private final Label titleLabel;
    private boolean ignorePathChange = false;

    public FileChooserNode(String title, boolean saveMode, File initialDirectory, Consumer<File> onAction, Runnable onCancel, String... allowedExtensions) {
        List<String> extensions = normalizeAllowedExtensions(allowedExtensions);

        titleLabel = new Label(title);
        titleLabel.setFont(Font.font("", FontWeight.NORMAL, FontPosture.REGULAR, 20));

        createPathPopup();

        pathField = new TextField();
        pathField.setFocusTraversable(false);
        pathField.textProperty().addListener((_, _, _) -> {
            if (ignorePathChange) {
                ignorePathChange = false;
            }
            else {
                updatePathPopup(pathField.getText());
            }

            if (saveMode && doneButton != null) {
                File file = fileFromString(pathField.getText());
                doneButton.setDisable(!isValidFileExtension(file, extensions) || file.isDirectory());
            }
        });
        pathField.focusedProperty().addListener((_, _, focused) -> {
            if (focused) {
                showPathPopup();
                updatePathPopup(pathField.getText());
            }
            else pathCompletionMenu.hide();
        });
        EventHandler<KeyEvent> keyEvent = e -> {
            if (!(pathField.isFocused() || pathCompletionMenu.isFocused())) return;

            if (e.getCode() == KeyCode.ENTER) {
                boolean focus = true;
                if (pathCompletionMenu.isShowing() && pathCompletionList.getSelectionModel().getSelectedItem() != null) {
                    String pre = pathField.getText().substring(0, pathField.getText().lastIndexOf("/") + 1);
                    ignorePathChange = true;
                    pathField.setText(pre + pathCompletionList.getSelectionModel().getSelectedItem());
                    pathField.positionCaret(pathField.getText().length());
                    showPathPopup();
                    updatePathPopup(pathField.getText());

                    focus = false;
                }

                select(pathField.getText(), focus);
                Platform.runLater(() -> updatePathPopup(pathField.getText()));
            }

            else if ((e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.UP) && !pathCompletionList.getItems().isEmpty()) {
                int size = pathCompletionList.getItems().size();
                int currentIndex = pathCompletionList.getSelectionModel().getSelectedIndex();
                int direction = e.getCode() == KeyCode.DOWN ? 1 : -1;
                int i = (currentIndex + direction + size) % size;
                pathCompletionList.getSelectionModel().select(i);
                pathCompletionList.scrollTo(i);
                e.consume();
            }
        };
        pathCompletionMenu.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent);
        pathField.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent);


        Path workingDirRoot = Path.of(System.getProperty("user.dir")).getRoot();
        TreeItem<File> rootItem = createNode(workingDirRoot == null ? new File(System.getProperty("user.home")) : workingDirRoot.toFile());
        view = new TreeView<>(rootItem);
        view.setShowRoot(true);
        VBox.setVgrow(view, Priority.ALWAYS);
        view.getSelectionModel().select(rootItem);

        view.setCellFactory(_ -> {
            final long[] lastClick = {-1};
            TreeCell<File> cell = new TreeCell<>() {
                @Override
                protected void updateItem(File file, boolean empty) {
                    super.updateItem(file, empty);
                    if (empty || file == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(file.getName().isEmpty() ? file.getAbsolutePath() : file.getName());
                        boolean valid = isValidFileExtension(file, extensions) || file.isDirectory();
                        pseudoClassStateChanged(PseudoClass.getPseudoClass("invalid-content"), !valid);
                    }
                }
            };
            cell.setOnMouseClicked(_ -> {
                if (cell.getItem() == null) return;
                if (!cell.getItem().isFile()) return;

                if (System.currentTimeMillis() - lastClick[0] < 250) {
                    doneButton.fire();
                }
                lastClick[0] = System.currentTimeMillis();
            });
            return cell;
        });
        view.getSelectionModel().selectedItemProperty().addListener((_, _, item) -> {
            if (item == null) {
                doneButton.setDisable(true);
                return;
            }

            doneButton.setDisable(!isValidFileExtension(item.getValue(), extensions) || item.getValue().isDirectory());
            ignorePathChange = true;
            pathField.setText(item.getValue().getAbsolutePath() + (item.getValue().isDirectory() ? "/" : ""));
            pathField.positionCaret(pathField.getText().length());
        });

        ButtonBar buttonBar = new ButtonBar();
        doneButton = new Button(saveMode ? "Save" : "Select");
        doneButton.setDefaultButton(true);
        doneButton.setDisable(true);
        doneButton.setOnAction(_ -> {
            if (saveMode) {
                File file = fileFromString(pathField.getText());
                if (isValidFileExtension(file, extensions) && !file.isDirectory()) onAction.accept(file);
                return;
            }

            if (view.getSelectionModel().getSelectedItem() == null) return;
            File file = view.getSelectionModel().getSelectedItem().getValue();
            if (isValidFileExtension(file, extensions) && !file.isDirectory()) onAction.accept(file);
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(_ -> onCancel.run());
        buttonBar.getButtons().addAll(cancelButton, doneButton);
        buttonBar.setPadding(new Insets(0, 10, 0, 10));

        getChildren().addAll(titleLabel, pathField, view, buttonBar);
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(10, 0, 10, 0));

        // Select initial dir
        ignorePathChange = true;
        pathField.setText(initialDirectory.getAbsolutePath());
        select(initialDirectory.getAbsolutePath(), true);
    }

    /**
     * This method is not part of the file chooser logic itself, but was added for smoother intergety with the Mangrypt layout.
     */
    public void prepareMangryptLayout(StackPane root, StackPane backgroundBox) {
        titleLabel.getStyleClass().add("uncolored-label");
        setPadding(new Insets(5));

        backgroundBox.getChildren().add(this);
        backgroundBox.setPadding(new Insets(20));
        backgroundBox.setBackground(
                new Background(new BackgroundFill(Color.DARKGREEN, new CornerRadii(20), null))
        );

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(backgroundBox.widthProperty());
        clip.heightProperty().bind(backgroundBox.heightProperty());
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        backgroundBox.setClip(clip);

        root.getChildren().add(backgroundBox);
        root.setPickOnBounds(true);
        root.setStyle("-fx-background-color: transparent;");
        StackPane.setMargin(backgroundBox, new Insets(50));

        pathCompletionList.getStyleClass().remove("list-view");
        pathCompletionList.getStyleClass().add("popup-list");
        pathCompletionMenu.getItems().getFirst().getStyleClass().remove("menu-item");
        pathCompletionMenu.getItems().getFirst().getStyleClass().add("popup-menu-root");
    }

    private void select(String path, boolean requestFocus) {
        File f = fileFromString(path);
        TreeItem<File> item = findItemByFile(f);
        if (item == null) return;
        if (item.getValue().isDirectory() && !item.isExpanded()) item.setExpanded(true);
        view.getSelectionModel().select(item);
        view.scrollTo(view.getSelectionModel().getSelectedIndex());
        if (requestFocus) view.requestFocus();
    }

    private TreeItem<File> createNode(final File file) {
        return new TreeItem<>(file) {
            private boolean isLeaf;
            private boolean isFirstTimeChildren = true;
            private boolean isFirstTimeLeaf = true;

            @Override
            public boolean isLeaf() {
                if (isFirstTimeLeaf) {
                    isFirstTimeLeaf = false;
                    isLeaf = file.isFile();
                }
                return isLeaf;
            }

            @Override
            public ObservableList<TreeItem<File>> getChildren() {
                if (isFirstTimeChildren) {
                    isFirstTimeChildren = false;
                    super.getChildren().setAll(buildChildren(this));
                }
                return super.getChildren();
            }

            private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> treeItem) {
                File f = treeItem.getValue();
                if (f != null && f.isDirectory()) {
                    File[] files = f.listFiles();
                    if (files != null) {
                        ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();

                        for (File childFile : files) {
                            children.add(createNode(childFile));
                        }
                        return children;
                    }
                }
                return FXCollections.emptyObservableList();
            }
        };
    }

    private static boolean isValidFileExtension(File file, List<String> extensions) {
        return extensions.stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .anyMatch(ext -> file.getName().toLowerCase(Locale.ROOT).endsWith(ext)) || extensions.isEmpty();
    }

    private List<String> normalizeAllowedExtensions(String... allowedExtensions) {
        List<String> normalized = new ArrayList<>();
        for (String s : allowedExtensions) {
            if (s.startsWith(".") || s.isBlank()) normalized.add(s);
            else if (s.startsWith("*.")) normalized.add(s.substring("*".length()));
            else normalized.add("." + s);
        }
        return normalized;
    }

    private TreeItem<File> findItemByFile(File file) {
        TreeItem<File> current = view.getRoot();
        if (current == null) return null;

        Path targetPath = file.toPath().toAbsolutePath();

        for (Path pathPart : targetPath) {
            boolean found = false;

            if (!current.isExpanded()) {
                current.setExpanded(true);
            }

            ObservableList<TreeItem<File>> children = current.getChildren();

            for (TreeItem<File> child : children) {
                if (child.getValue().toPath().getFileName().equals(pathPart)) {
                    current = child;
                    found = true;
                    break;
                }
            }

            if (!found) {
                break;
            }
        }
        return current;
    }

    private File fileFromString(String s) {
        String root = view.getRoot().getValue().getAbsolutePath();
        return new File(s.isBlank() ? root : (s.startsWith(root) ? s : root + s));
    }

    private void updatePathPopup(String path) {
        pathCompletionList.getItems().clear();
        TreeItem<File> currentPos = findItemByFile(fileFromString(path));
        if (currentPos == null) {
            pathCompletionMenu.hide();
            return;
        }
        String filter = fileFromString(path).exists() ? "" : path.substring(path.lastIndexOf("/") + 1);
        File[] options = currentPos.getValue().listFiles((_, name) -> name.contains(filter));
        if (options == null) {
            pathCompletionMenu.hide();
            return;
        }


        Font font = Font.font("", 12);
        double maxTextWidth = 0;
        for (File option : options) {
            String name = option.getName();
            if (option.isDirectory()) name = name + "/";
            pathCompletionList.getItems().add(name);
            Text text = new Text(name);
            text.setFont(font);
            double width = text.getLayoutBounds().getWidth();
            maxTextWidth = Math.max(maxTextWidth, width);
        }

        double rowHeight = 24.0;
        int visibleRows = Math.min(pathCompletionList.getItems().size(), 10);
        pathCompletionList.setPrefHeight(visibleRows * rowHeight);
        pathCompletionList.setMaxHeight(visibleRows * rowHeight);
        double extraPadding = 60.0;
        pathCompletionList.setPrefWidth(maxTextWidth + extraPadding);
        pathCompletionList.setMaxWidth(maxTextWidth + extraPadding);

        if (pathCompletionList.getItems().isEmpty()) {
            pathCompletionMenu.hide();
        }
        else {
            showPathPopup();
        }
    }

    private void showPathPopup() {
        Point2D fieldPos = pathField.localToScreen(0, 0);
        if (fieldPos == null) return;
        pathCompletionMenu.show(pathField, fieldPos.getX(), fieldPos.getY() + pathField.getHeight());
    }

    private void createPathPopup() {
        pathCompletionList = new ListView<>();
        pathCompletionList.setPrefHeight(0);
        pathCompletionList.setMaxHeight(0);
        pathCompletionList.setCellFactory(_ -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String s, boolean b) {
                    super.updateItem(s, b);
                    if (s == null || b) {
                        setText(null);
                        setGraphic(null);
                    }
                    else {
                        setText(s);
                    }
                }
            };
            cell.setOnMouseEntered(_ -> cell.getListView().getSelectionModel().select(cell.getItem()));
            return cell;
        });

        CustomMenuItem scrollableItem = new CustomMenuItem(pathCompletionList, false);
        pathCompletionMenu = new ContextMenu(scrollableItem);
    }
}
