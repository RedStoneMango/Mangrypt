package io.github.redstonemango.mangrypt.front.controller;

import io.github.redstonemango.mangoutils.NameConverter;
import io.github.redstonemango.mangoutils.OperatingSystem;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.dataTypes.*;
import io.github.redstonemango.mangrypt.front.ListEntry;
import io.github.redstonemango.mangrypt.back.ConfigIO;
import io.github.redstonemango.mangrypt.back.Utilities;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FileSystemController {

    private final BooleanProperty showHiddenContentProperty = new SimpleBooleanProperty(false);
    private FolderElement currentFolder = ConfigIO.getConfig().getRootFolder(); // In the beginning, we are in the root folder
    private boolean ignorePathChange = false;


    @FXML ListView<FileSystemElement> contentView;
    @FXML Label nameLabel;
    @FXML ImageView addImage;
    @FXML ImageView configureImage;
    @FXML TextField pathField;
    @FXML Button parentDirButton;

    private ListView<String> pathCompletionList;
    private ContextMenu pathCompletionMenu;

    @FXML
    private void initialize() {
        Utilities.applyCustomNodeCellFactory(contentView, element -> {
            FolderElement parent = element.getParent();
            assert parent != null : "Only null when referencing root's parent, which should never happen";
            return new ListEntry(
                    element.getName(),
                    element.getDescription(),
                    () -> onOpen(element),
                    () -> onDelete(element),
                    () -> onRename(element),
                    () -> onChangeDescription(element),
                    () -> onExport(element),
                    () -> onExport(parent),
                    element.runIconImageBuild(),
                    element instanceof FolderElement,
                    contentView);
            },
            new Insets(0, 0, 1, 0));

        updateContentView(null);
        preparePathPopup();


        contentView.getStyleClass().add("file-system-list");
        contentView.getSelectionModel().selectedItemProperty().addListener(
                (_, _, item) -> {
                    if (item == null) return;
                    updatedPathFieldTarget(item);
        });

        String name = ConfigIO.getVaultFile().getName().substring(0, ConfigIO.getVaultFile().getName().length() - ".mgvault".length());
        name = NameConverter.convert(
                name,
                NameConverter.NamingConvention.MIXED_CASE_TYPES,
                NameConverter.NamingConvention.PLAIN_TEXT,
                true);

        nameLabel.setText(name);

        showHiddenContentProperty.addListener((_, _, _) -> updateContentView(null));

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

    private void onDelete(FileSystemElement element) {
        Mangrypt.getBase().showConfirmationDialog(
                "Delete '" + element.getName() + "'",
                "Do you really want to delete '" + element.getName() + "'" +
                        (element instanceof FolderElement ? " and all data it contains" : "") + "?",
                () -> {
                    currentFolder.getContent().remove(element.getName());
                    element.zeroOut();
                    updateContentView(null);
                    ConfigIO.markShouldSave();
                }
        );
    }

    private void onRename(FileSystemElement element) {
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
                    element.setName(name);
                    updateContentView(null);
                    ConfigIO.markShouldSave();
                }
        );
    }

    private void onChangeDescription(FileSystemElement element) {
        Mangrypt.getBase().showInputDialog("Please set a new description for the element",
                "Description",
                element.getDescription(),
                true,
                description -> {
                    element.setDescription(description);
                    contentView.refresh();
                    ConfigIO.markShouldSave();
                }
        );
    }

    private void onExport(FileSystemElement element) {
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
        MenuItem passwordChangeItem = new MenuItem("Change password & passphrase");
        MenuItem backMenuItem = new MenuItem("Back to vault overview");
        ContextMenu menu = new ContextMenu(showHiddenFoldersItem, passwordChangeItem, new SeparatorMenuItem(), backMenuItem);
        Point2D imagePos = configureImage.localToScreen(0, 0);
        menu.show(configureImage.getParent(), imagePos.getX(), imagePos.getY());
        menu.setX(imagePos.getX() - menu.getWidth());

        showHiddenFoldersItem.setSelected(showHiddenContentProperty.get());
        showHiddenFoldersItem.selectedProperty().addListener((_, _, b) ->
                showHiddenContentProperty.set(b));

        passwordChangeItem.setOnAction(_ -> {
            try {
                FXMLLoader loader = new FXMLLoader(Utilities.class.getResource("/io/github/redstonemango/mangrypt/fxml/security-setup.fxml"));
                Mangrypt.getBase().setSecondLayerRoot(loader.load());
            }
            catch (IOException e) {
                throw new RuntimeException(e); // I love happy compilers
            }
        });

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

    private void updatedPathFieldTarget(FileSystemElement element) {
        ignorePathChange = true;
        String text = element.buildPath();
        if (element == currentFolder) text = text + "/"; // If we are IN the folder, add '/'. Do not add if folder's just selected
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
