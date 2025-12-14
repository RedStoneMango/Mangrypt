package io.github.redstonemango.mangrypt.front.controller;

import io.github.redstonemango.mangoutils.NameConverter;
import io.github.redstonemango.mangoutils.OperatingSystem;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.PseudoClipboard;
import io.github.redstonemango.mangrypt.back.dataTypes.*;
import io.github.redstonemango.mangrypt.front.ListEntry;
import io.github.redstonemango.mangrypt.back.ConfigIO;
import io.github.redstonemango.mangrypt.back.Utilities;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class FileSystemController {

    private final PseudoClipboard clipboard = new PseudoClipboard();

    private final BooleanProperty showHiddenContentProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty wraparoundNavigationProperty = new SimpleBooleanProperty(false);
    private FolderElement currentFolder = ConfigIO.getConfig().getRootFolder(); // In the beginning, we are in the root folder
    private boolean ignorePathChange = false;
    private ObservableList<FileSystemElement> selectedElements;


    @FXML ListView<FileSystemElement> contentView;
    @FXML Label nameLabel;
    @FXML ImageView addImage;
    @FXML ImageView configureImage;
    @FXML TextField pathField;
    @FXML Button parentDirButton;

    private ListView<String> pathCompletionList;
    private ContextMenu pathCompletionMenu;
    private ContextMenu contentViewMenu;

    @FXML
    private void initialize() {
        Utilities.applyCustomNodeCellFactory(contentView, element -> {
                    FolderElement parent = element.getParent();
                    assert parent != null : "Only null when referencing root's parent, which should never happen";
                    return new ListEntry(
                            element.getName(),
                            element.getDescription(),
                            () -> onOpen(element),
                            () -> delete(element),
                            this::onDeleteSelection,
                            this::onRename,
                            this::onChangeDescription,
                            this::onExport,
                            () -> export(parent),
                            this::onCopy,
                            this::onCut,
                            this::onPaste,
                            () -> onPasteInto((FolderElement) element),
                            () -> {
                                onCopy();
                                onPaste();
                            },
                            element.runIconImageBuild(),
                            element instanceof FolderElement,
                            clipboard,
                            selectedElements,
                            contentView);
                },
                new Insets(0, 0, 1, 0));

        updateContentView(null);
        preparePathPopup();


        contentView.getStyleClass().add("file-system-list");
        contentView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedElements = contentView.getSelectionModel().getSelectedItems();
        selectedElements.addListener((ListChangeListener<? super FileSystemElement>) l -> {
            if (l.getList().isEmpty()) updatedPathFieldTarget(null);
            else if (l.getList().size() == 1) updatedPathFieldTarget(l.getList().getFirst());
            else updatedPathFieldTargetToMultiple(l.getList().size());
        });
        prepareContentViewMenu();
        contentView.setOnKeyPressed(e -> {
            boolean controlDown = OperatingSystem.isMac() ? e.isMetaDown() : e.isControlDown();
            if (controlDown && e.getCode() == KeyCode.C) {
                onCopy();
            }
            else if (controlDown && e.getCode() == KeyCode.X) {
                onCut();
            }
            else if (controlDown && e.getCode() == KeyCode.V) {
                onPaste();
            }
            else if (controlDown && e.getCode() == KeyCode.D) {
                onCopy();
                onPaste();
            }
            else if (e.getCode() == KeyCode.DELETE) {
                if (!selectedElements.isEmpty()) onDeleteSelection();
            }
            else if (e.getCode() == KeyCode.ENTER) {
                if (!selectedElements.isEmpty()) onOpen(selectedElements.getFirst());
            }
            else if ((e.isAltDown() && e.getCode() == KeyCode.UP) || e.getCode() == KeyCode.ESCAPE) {
                if (!parentDirButton.isDisabled()) onParentDir();
            }
        });

        String name = ConfigIO.getVaultFile().getName().substring(0, ConfigIO.getVaultFile().getName().length() - ".mgvault".length());
        name = NameConverter.convert(
                name,
                NameConverter.NamingConvention.MIXED_CASE_TYPES,
                NameConverter.NamingConvention.PLAIN_TEXT,
                true);

        nameLabel.setText(name);

        wraparoundNavigationProperty.set(ConfigIO.getConfig().isWraparoundNavigation());
        wraparoundNavigationProperty.addListener((_, _, b) -> {
            ConfigIO.getConfig().setWraparoundNavigation(b);
            ConfigIO.markShouldSave();
        });
        showHiddenContentProperty.set(ConfigIO.getConfig().isShowHidden());
        showHiddenContentProperty.addListener((_, _, b) -> {
            updateContentView(null);
            ConfigIO.getConfig().setShowHidden(b);
            ConfigIO.markShouldSave();
        });

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

        Platform.runLater(() -> {
            Utilities.registerHoverAnimation(configureImage);
            Utilities.registerHoverAnimation(addImage);
        });
    }

    private void prepareContentViewMenu() {
        MenuItem parentDirItem = new MenuItem("Open Parent Folder");
        parentDirItem.setOnAction(_ -> onParentDir());
        parentDirItem.disableProperty().bind(parentDirButton.disabledProperty());
        MenuItem exportItem = new MenuItem("Export Current Folder");
        exportItem.setOnAction(_ -> export(currentFolder));
        MenuItem pasteItem = new MenuItem("Paste Elements");
        pasteItem.setOnAction(_ -> onPaste());
        contentViewMenu = new ContextMenu(parentDirItem, new SeparatorMenuItem(), exportItem, pasteItem);
        contentView.setOnMouseClicked(e -> {
            if (contentViewMenu.isShowing()) contentViewMenu.hide();
            if (e.getButton() == MouseButton.SECONDARY && isNotFilledCell(e)) {
                pasteItem.setDisable(clipboard.isEmpty());
                contentViewMenu.show(contentView, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private boolean isNotFilledCell(MouseEvent event) {
        Node target = (Node) event.getTarget();

        while (target != null && !(target instanceof ListCell)) {
            target = target.getParent();
        }

        if (target instanceof ListCell<?> cell) {
            return cell.isEmpty();
        } else {
            return false;
        }
    }

    private void onOpen(FileSystemElement element) {
        if (element instanceof FolderElement folder) {
            openFolder(folder);
        }
        else if (element instanceof DataElement data) {
            List<DataElement> availableData = new ArrayList<>();
            contentView.getItems().forEach(item -> {
                if (item instanceof DataElement this_data) {
                    availableData.add(this_data);
                }
            });

            Mangrypt.getBase().showData(availableData, data);
        }
        else {
            Mangrypt.getBase().showErrorAlert("The specified FileSystemElement is neither a FolderElement nor a DataElement");

            throw new RuntimeException("element has to be an instance of " + FolderElement.class.getName() + " or " +
                    DataElement.class.getName() + ". Found" + element.getClass().getName() + "instead");
        }
    }

    private void onDeleteSelection() {
        if (selectedElements.size() == 1) {
            delete(selectedElements.getFirst());
            return;
        }

        Mangrypt.getBase().showConfirmationDialog(
                "Delete " + selectedElements.size() + " elements",
                "Do you really want to delete " + selectedElements.size() + " elements and all data they " +
                        "might contain?",
                () -> {
                    selectedElements.forEach(element -> {
                        currentFolder.getContent().remove(element.getName());
                        element.zeroOut();
                    });
                    updateContentView(null);
                    ConfigIO.markShouldSave();
                }
        );
    }
    private void delete(FileSystemElement element) {
        Mangrypt.getBase().showConfirmationDialog(
                "Delete '" + element.getName() + "'",
                "Do you really want to delete '" + element.getName() + "'" +
                        (element instanceof FolderElement ? " and all data it contains" : "") + "?" +
                        (selectedElements.size() > 1 ? "\n\nOther selected items will not be deleted." : ""),
                () -> {
                    currentFolder.getContent().remove(element.getName());
                    updateContentView(null);
                    element.zeroOut();
                    ConfigIO.markShouldSave();
                }
        );
    }

    private void onRename() {
        if (selectedElements.size() == 1) {
            renameSingle(selectedElements.getFirst());
            return;
        }

        Mangrypt.getBase().showInputDialog(
                "Please set a new base name for the elements",
                "Base name",
                "",
                true,
                name -> {
                    if (name.isBlank()) return false;
                    return !name.equals(".");
                },
                name -> {
                    if (name.startsWith(".")) return "Elements starting with . are hidden";
                    return null;
                },
                baseName -> {

                    selectedElements.forEach(element -> {
                        String name = nextFreeName(currentFolder, baseName, false);
                        currentFolder.getContent().remove(element.getName()); // Remove old
                        element.setName(name);
                        currentFolder.getContent().put(name, element); // Add new
                    });

                    updateContentView(null);
                    ConfigIO.markShouldSave();
                }
        );
    }

    private String nextFreeName(FolderElement folder, String baseName, boolean allowBaseOnly) {
        if (allowBaseOnly && !folder.getContent().containsKey(baseName)) return baseName;

        AtomicInteger number = new AtomicInteger(1);
        String finalName = baseName + " (" + number.getAndIncrement() + ")";
        while (folder.getContent().containsKey(finalName)) {
            finalName = baseName + " (" + number.getAndIncrement() + ")";
        }
        return finalName;
    }

    private void renameSingle(FileSystemElement element) {
        Set<String> existingNames = currentFolder.getContent().keySet();
        String oldName = element.getName();

        Mangrypt.getBase().showInputDialog(
                "Please set a new name for the element",
                "Element name",
                element.getName(),
                true,
                name -> {
                    if (existingNames.contains(name) && !name.equals(oldName)) return false;
                    if (name.isBlank()) return false;
                    return !name.equals(".");
                },
                name -> {
                    if (existingNames.contains(name) && !name.equals(oldName)) return "Such an element already exists";
                    else if (name.startsWith(".")) return "Elements starting with . are hidden";
                    return null;
                },
                name -> {
                    currentFolder.getContent().remove(element.getName()); // Remove old
                    element.setName(name);
                    currentFolder.getContent().put(name, element); // Add new
                    updateContentView(null);
                    ConfigIO.markShouldSave();
                }
        );
    }

    private void onChangeDescription() {
        Mangrypt.getBase().showInputDialog("Please set a new description for the element" +
                        (selectedElements.size() == 1 ? "" : "s"),
                "Description",
                selectedElements.size() == 1 ? selectedElements.getFirst().getDescription() : "",
                true,
                description -> {
                    selectedElements.forEach(element -> element.setDescription(description));
                    contentView.refresh();
                    ConfigIO.markShouldSave();
                }
        );
    }

    private void onExport() {
        if (selectedElements.size() == 1) export(selectedElements.getFirst());
        else {
            // Create temporary pseudo-folder to wrap all elements in one
            FolderElement tempFolder = new FolderElement();
            tempFolder.ensureFields("Elements", null);
            selectedElements.forEach(element -> tempFolder.getContent().put(element.getName(), element));
            export(tempFolder);
        }
    }

    private void export(FileSystemElement element) {
        String extension = element instanceof DataElement data ? data.fileExtension() : ".zip";

        Utilities.showSavingFileChooser(
                "Export '" + element.getName() + "' as " + extension,
                element.getName() + extension,
                file -> {

                    Runnable exportAction = () -> {
                        boolean success = element.exportTo(file, true);
                        ButtonType browseButton = new ButtonType("Browse File");

                        if (success) {
                            Mangrypt.getBase().showAlert(
                                    Alert.AlertType.INFORMATION,
                                    "Exported data",
                                    """
                                            Successfully exported your data.
                                            Do you want to browse browse the file?""",
                                    true,
                                    btn -> {
                                        if (btn == browseButton) {
                                            OperatingSystem.loadCurrentOS().browse(file);
                                        }
                                    }, browseButton, new ButtonType("Stay in Application"));
                        }

                        else {
                            Mangrypt.getBase().showAlert(
                                    Alert.AlertType.WARNING,
                                    "Export failure",
                                    """
                                            The export was not successful.
                                            Still, some data might have been exported correctly.
                                            Do you want to try browsing the file?""",
                                    true,
                                    btn -> {
                                        if (btn == browseButton) {
                                            OperatingSystem.loadCurrentOS().browse(file);
                                        }
                                    }, browseButton, ButtonType.CLOSE);
                        }
                    };

                    if (file.exists()) {
                        Mangrypt.getBase().showConfirmationDialog(
                                "File already exist",
                                "Do you want to overwrite '" + file.getName() + "'?",
                                exportAction
                        );
                    }
                    else {
                        exportAction.run();
                    }
                },
                extension);
    }

    private void onCopy() {
        clipboard.copy(selectedElements);
    }
    private void onCut() {
        clipboard.copy(selectedElements);
        selectedElements.forEach(element -> {
            currentFolder.getContent().remove(element.getName());
            element.zeroOut();
        });

        if (!selectedElements.isEmpty()) {
            updateContentView(null);
            ConfigIO.markShouldSave();
        }
    }
    private void onPaste() {
        clipboard.paste().ifPresent(elements -> {
            AtomicReference<FileSystemElement> lastAdded = new AtomicReference<>();
            elements.forEach(element -> {
                lastAdded.set(element);
                element.updateParent(currentFolder);
                String name = nextFreeName(currentFolder, element.getName(), true);
                currentFolder.getContent().put(name, element);
                element.setName(name);
            });

            updateContentView(lastAdded.get());
            ConfigIO.markShouldSave();
        });
    }
    private void onPasteInto(FolderElement folder) {
        clipboard.paste().ifPresent(elements -> {
            elements.forEach(element -> {
                element.updateParent(folder);
                String name = nextFreeName(folder, element.getName(), true);
                folder.getContent().put(name, element);
                element.setName(name);
            });

            updateContentView(null); // Theoretically not needed, but it's a nice feedback that paste worked
            ConfigIO.markShouldSave();
        });
    }

    @FXML
    private void onAdd() {
        Set<String> existingNames = currentFolder.getContent().keySet();

        MenuItem folderItem = new MenuItem("Folder");
        ImageView folderIcon = new ImageView(FolderElement.buildIconImage());
        folderIcon.setPreserveRatio(true);
        folderIcon.setFitHeight(20);
        folderItem.setGraphic(folderIcon);
        folderItem.setOnAction(_ -> ContentAdder.addFolder(currentFolder, this::updateContentView, existingNames));

        MenuItem textItem = new MenuItem("Text Data");
        ImageView textIcon = new ImageView(TextDataElement.buildIconImage());
        textIcon.setPreserveRatio(true);
        textIcon.setFitHeight(20);
        textItem.setGraphic(textIcon);
        textItem.setOnAction(_ -> ContentAdder.addTextElement(currentFolder, this::updateContentView, existingNames));

        MenuItem imageItem = new MenuItem("Image Data");
        ImageView imageIcon = new ImageView(ImageDataElement.buildIconImage());
        imageIcon.setPreserveRatio(true);
        imageIcon.setFitHeight(20);
        imageItem.setGraphic(imageIcon);
        imageItem.setOnAction(_ -> ContentAdder.addImageElement(currentFolder, this::updateContentView, existingNames));

        MenuItem mediaItem = new MenuItem("Audio/Video Data");
        ImageView mediaIcon = new ImageView(MediaDataElement.buildIconImage());
        mediaIcon.setPreserveRatio(true);
        mediaIcon.setFitHeight(20);
        mediaItem.setGraphic(mediaIcon);
        mediaItem.setOnAction(_ -> ContentAdder.addMediaElement(currentFolder, this::updateContentView, existingNames));

        ContextMenu menu = new ContextMenu(folderItem, new SeparatorMenuItem(), textItem, imageItem, mediaItem);
        Point2D imagePos = addImage.localToScreen(0, 0);
        menu.show(addImage.getParent(), imagePos.getX() + addImage.getFitWidth(), imagePos.getY());
    }
    @FXML
    private void onConfigure() {
        CheckMenuItem showHiddenFoldersItem = new CheckMenuItem("Show hidden elements");
        CheckMenuItem wraparoundNavigationItem = new CheckMenuItem("Wraparound navigation");
        MenuItem passwordChangeItem = new MenuItem("Change password & passphrase");
        MenuItem vaultSaveItem = new MenuItem("Save Vault");
        MenuItem backMenuItem = new MenuItem("Back to vault overview");
        ContextMenu menu = new ContextMenu(
                showHiddenFoldersItem,
                wraparoundNavigationItem,
                passwordChangeItem,
                vaultSaveItem,
                new SeparatorMenuItem(),
                backMenuItem
        );
        Point2D imagePos = configureImage.localToScreen(0, 0);
        menu.show(configureImage.getParent(), imagePos.getX(), imagePos.getY());
        menu.setX(imagePos.getX() - menu.getWidth());

        showHiddenFoldersItem.setSelected(showHiddenContentProperty.get());
        showHiddenFoldersItem.selectedProperty().addListener((_, _, b) ->
                showHiddenContentProperty.set(b));

        wraparoundNavigationItem.setSelected(wraparoundNavigationProperty.get());
        wraparoundNavigationItem.selectedProperty().addListener((_, _, b) ->
                wraparoundNavigationProperty.set(b));

        passwordChangeItem.setOnAction(_ -> {
            try {
                FXMLLoader loader = new FXMLLoader(Utilities.class.getResource("/io/github/redstonemango/mangrypt/fxml/security-setup.fxml"));
                Mangrypt.getBase().setSecondLayerRoot(loader.load());
            }
            catch (IOException e) {
                throw new RuntimeException(e); // I love happy compilers
            }
        });

        vaultSaveItem.setOnAction(_ -> Mangrypt.getBase().savingRoutine(() ->
                Mangrypt.getBase().showInfoAlert("\nDone Saving Your Vault!"),
            false, true));

        backMenuItem.setOnAction(_ -> Mangrypt.getBase().playTransition(() -> Mangrypt.getBase().savingRoutine()));
    }

    @FXML
    private void onParentDir() {
        FolderElement parent = currentFolder.getParent();
        if (parent == null) {
            Mangrypt.getBase().showWarningAlert("Your current folder does not have a parent registered");
            return;
        }
        openFolder(parent);
    }

    private void updateContentView(@Nullable FileSystemElement selectElement) {
        contentView.getItems().clear();
        contentView.getItems().addAll(
                currentFolder.getContent().values().stream()
                        .filter(object -> (showHiddenContentProperty.get() || !object.getName().startsWith(".")))
                        .sorted(Comparator.comparing(FileSystemElement::getName))
                        .toList());

        if (selectElement != null) {
            contentView.scrollTo(selectElement);
            contentView.getSelectionModel().select(selectElement);
        }
    }

    private void openFolder(FolderElement folder) {
        currentFolder = folder;
        updateContentView(null);
        parentDirButton.setDisable(folder == ConfigIO.getConfig().getRootFolder());
        updatedPathFieldTarget(folder);
    }

    private void updatedPathFieldTarget(@Nullable FileSystemElement element) {
        ignorePathChange = true;
        String text = element != null && (showHiddenContentProperty.get() || !element.getName().startsWith("."))
                ? element.buildPath() : currentFolder.buildPath();
        if (element == currentFolder) text = text + "/"; // If we are IN the folder, add '/'. Do not add if folder's just selected
        text = text.replaceAll("/{2,}", "/"); // Normalize
        pathField.setText(text);
        pathField.positionCaret(text.length());
    }

    private void updatedPathFieldTargetToMultiple(int selectedCount) {
        ignorePathChange = true;
        String text = currentFolder.buildPath() + "/... [" + selectedCount + "]";
        text = text.replaceAll("/{2,}", "/"); // Normalize
        pathField.setText(text);
        pathField.positionCaret(text.length());
    }

    private void updateSelection() {
        FileSystemElement element = FileSystemElement.fromPath(pathField.getText());
        FolderElement folder = element instanceof FolderElement _folder ? _folder : element.getParent();
        assert folder != null: "Folder is only null when referencing root's parent, which should never happen";
        DataElement data = element instanceof DataElement _data ? _data : null;

        if (folder != currentFolder) {
            openFolder(folder);
        }

        if (data != null) {
            contentView.scrollTo(data);
            contentView.getSelectionModel().select(data);
        }

        updatedPathFieldTarget(data != null ? data : folder);
    }

    private void preparePathPopup() {
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

        pathCompletionMenu.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!pathCompletionMenu.isFocused()) return;

            if (e.getCode() == KeyCode.ENTER) {
                String selected = pathCompletionList.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    pathCompletionMenu.hide();
                    updateSelection();
                    return;
                }

                String pre = pathField.getText().substring(0, pathField.getText().lastIndexOf("/") + 1);
                ignorePathChange = true;
                String newText = pre + selected;
                if (!newText.startsWith("/")) newText = "/" + newText;

                pathField.setText(newText);
                pathField.positionCaret(newText.length());
                showPathPopup();
                updatePathPopup(newText);
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

    private void updatePathPopup(String path) {
        pathCompletionList.getItems().clear();
        FileSystemElement pos = FileSystemElement.fromPath(path);
        FolderElement currentPos = pos instanceof FolderElement folder ? folder : pos.getParent();
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
            if (option instanceof FolderElement) name = name + "/";
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
