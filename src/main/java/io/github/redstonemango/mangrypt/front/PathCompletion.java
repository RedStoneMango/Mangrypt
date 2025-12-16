package io.github.redstonemango.mangrypt.front;

import io.github.redstonemango.mangrypt.back.dataTypes.DataElement;
import io.github.redstonemango.mangrypt.back.dataTypes.FileSystemElement;
import io.github.redstonemango.mangrypt.back.dataTypes.FolderElement;
import io.github.redstonemango.mangrypt.back.dataTypes.SymlinkElement;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PathCompletion {

    private final TextField pathField;
    private final FolderElement currentFolder;
    private final BooleanProperty showHiddenContentProperty;
    private final Consumer<FileSystemElement> onSelect;
    private ListView<String> pathCompletionList;
    private ContextMenu pathCompletionMenu;
    public boolean ignorePathChange = false;

    public PathCompletion(TextField pathField, FolderElement currentFolder, BooleanProperty showHiddenContentProperty,
                          Consumer<FileSystemElement> onSelect) {
        this.pathField = pathField;
        this.currentFolder = currentFolder;
        this.showHiddenContentProperty = showHiddenContentProperty;
        this.onSelect = onSelect;

        pathField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateSelection();
            }
        });
        pathField.textProperty().addListener((_, _, _) -> {
            if (ignorePathChange) {
                ignorePathChange = false;
            }
            else {
                updatePathPopup(pathField.getText());
            }
        });
        pathField.focusedProperty().addListener((_, _, focused) -> {
            if (focused) {
                showPathPopup();
                updatePathPopup(pathField.getText());
            }
            else {
                pathCompletionMenu.hide();
            }
        });
        preparePathPopup();
    }

    public void updatedPathFieldTarget(@Nullable FileSystemElement element) {
        ignorePathChange = true;
        String text = element != null && (showHiddenContentProperty.get() || !element.getName().startsWith("."))
                ? element.buildPath() : currentFolder.buildPath();
        if (element == currentFolder) text = text + "/"; // If we are IN the folder, add '/'. Do not add if folder's just selected
        text = text.replaceAll("/{2,}", "/"); // Normalize
        pathField.setText(text);
        pathField.positionCaret(text.length());
    }

    public void updatedPathFieldTargetToMultiple(int selectedCount) {
        ignorePathChange = true;
        String text = currentFolder.buildPath() + "/... [" + selectedCount + "]";
        text = text.replaceAll("/{2,}", "/"); // Normalize
        pathField.setText(text);
        pathField.positionCaret(text.length());
    }

    public void updateSelection() {
        FileSystemElement element = FileSystemElement.fromPath(pathField.getText());
        FolderElement folder = element instanceof FolderElement _folder ? _folder : element.getParent();
        assert folder != null: "Folder is only null when referencing root's parent, which should never happen";
        DataElement data = element instanceof DataElement _data ? _data : null;
        updatedPathFieldTarget(data != null ? data : folder);

        onSelect.accept(element);
    }

    public void preparePathPopup() {
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
            cell.setOnMouseClicked(_ -> doCompletion(cell.getItem()));
            return cell;
        });
        CustomMenuItem scrollableItem = new CustomMenuItem(pathCompletionList, false);
        pathCompletionMenu = new ContextMenu(scrollableItem);

        pathCompletionMenu.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!pathCompletionMenu.isFocused()) return;

            if (e.getCode() == KeyCode.ENTER) {
                String selected = pathCompletionList.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    pathCompletionMenu.hide();
                    updateSelection();
                    return;
                }

                doCompletion(selected);
            }
            else if ((e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.UP) && !pathCompletionList.getItems().isEmpty()) {
                int size = pathCompletionList.getItems().size();
                int currentIndex = pathCompletionList.getSelectionModel().getSelectedIndex();
                int direction = e.getCode() == KeyCode.DOWN ? 1 : -1;
                int i = (currentIndex + direction + size) % size;
                pathCompletionList.getSelectionModel().select(i);
                pathCompletionList.scrollTo(i);
            }
            else if (e.getCode() == KeyCode.ESCAPE) {
                pathCompletionMenu.hide();
            }
        });

        pathCompletionList.getStyleClass().remove("list-view");
        pathCompletionList.getStyleClass().add("popup-list");
        pathCompletionMenu.getItems().getFirst().getStyleClass().remove("menu-item");
        pathCompletionMenu.getItems().getFirst().getStyleClass().add("popup-menu-root");
    }

    private void doCompletion(String selected) {
        String pre = pathField.getText().substring(0, pathField.getText().lastIndexOf("/") + 1);
        ignorePathChange = true;
        String newText = pre + selected;
        if (!newText.startsWith("/")) newText = "/" + newText;

        pathField.setText(newText);
        pathField.positionCaret(newText.length());
        showPathPopup();
        updatePathPopup(newText);
    }

    private void updatePathPopup(String path) {
        pathCompletionList.getItems().clear();
        FileSystemElement pos = FileSystemElement.fromPath(path);
        FolderElement currentPos = pos instanceof FolderElement folder ? folder : pos.getParent();
        if (pos instanceof SymlinkElement ln && ln.resolveTargetElement() instanceof FolderElement lnFolder) {
            currentPos = lnFolder;
        }
        if (currentPos == null) {
            pathCompletionMenu.hide();
            return;
        }

        String filter = path.substring(path.lastIndexOf("/") + 1);
        Set<FileSystemElement> options = currentPos.getContent().values().stream()
                .filter(object -> (showHiddenContentProperty.get() || !object.getName().startsWith(".")))
                .filter(element -> element.getName().contains(filter))
                .collect(Collectors.toSet());

        if (options.isEmpty()) {
            pathCompletionMenu.hide();
            return;
        }


        Font font = Font.font("", 12);
        double maxTextWidth = 0;
        for (FileSystemElement option : options) {
            String name = option.getName();
            if (option instanceof FolderElement ||
                    (option instanceof SymlinkElement ln && ln.resolveTargetElement() instanceof FolderElement)) {
                name = name + "/";
            }
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
        pathCompletionList.scrollTo(0);
        pathCompletionList.getSelectionModel().select(null);
    }

    private void showPathPopup() {
        Point2D fieldPos = pathField.localToScreen(0, 0);
        if (fieldPos == null) return;
        pathCompletionMenu.show(pathField, fieldPos.getX(), fieldPos.getY() + pathField.getHeight());
    }

}
