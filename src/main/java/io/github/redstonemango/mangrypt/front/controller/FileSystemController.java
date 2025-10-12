package io.github.redstonemango.mangrypt.front.controller;

import io.github.redstonemango.mangoutils.NameConverter;
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
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FileSystemController {

    private final BooleanProperty showHiddenContentProperty = new SimpleBooleanProperty(false);

    private FolderElement currentFolder = ConfigIO.getConfig().getRootFolder(); // In the beginning, we are in the root folder

    @FXML ListView<FileSystemElement> contentView;
    @FXML Label nameLabel;
    @FXML ImageView addImage;
    @FXML ImageView configureImage;
    @FXML TextField pathField;
    @FXML Button parentDirButton;

    @FXML
    private void initialize() {
        Utilities.applyCustomNodeCellFactory(contentView, element ->
                new ListEntry(
                        element.getName(),
                        element.getDescription(),
                        () -> onOpen(element),
                        () -> onDelete(element),
                        () -> onRename(element),
                        () -> onChangeDescription(element),
                        () -> onFolderExport(element),
                        element.runIconImageBuild(),
                        element instanceof FolderElement,
                        contentView));

        updateContentView(null);

        String name = ConfigIO.getVaultFile().getName().substring(0, ConfigIO.getVaultFile().getName().length() - ".mgvault".length());
        name = NameConverter.convert(name, NameConverter.NamingConvention.MIXED_CASE_TYPES, NameConverter.NamingConvention.PLAIN_TEXT, true);
        nameLabel.setText(name);

        showHiddenContentProperty.addListener((_, _, _) -> updateContentView(null));

        Platform.runLater(() -> {
            Utilities.registerHoverAnimation(configureImage);
            Utilities.registerHoverAnimation(addImage);
        });
    }

    private void onOpen(FileSystemElement element) {
        if (element instanceof FolderElement folder) {
            Mangrypt.getBase().showInfoAlert("Sorry, but this feature is not yet implemented");
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
            throw new RuntimeException("element has to be an instance of " + FolderElement.class.getName() + " or " + DataElement.class.getName() + ". Found" + element.getClass().getName() + "instead");
        }
    }

    private void onDelete(FileSystemElement element) {
        Mangrypt.getBase().showConfirmationDialog("Delete '" + element.getName() + "'", "Do you really want to delete '" + element + "'" + (element instanceof FolderElement ? " and all data it contains" : "") + "?", () -> {
            currentFolder.getContent().remove(element);
            element.zeroOut();
            contentView.refresh();
        });
    }

    private void onRename(FileSystemElement element) {
        Mangrypt.getBase().showInputDialog("Please set a new name for the element", "Element name", element.getName(), true, name -> !name.isBlank() && !name.equals("."), name -> name.startsWith(".") ? "Elements starting with . are hidden" : null, name -> {
            element.setName(name);
            updateContentView(null);
        });
    }

    private void onChangeDescription(FileSystemElement element) {
        Mangrypt.getBase().showInputDialog("Please set a new description for the element", "Description", element.getDescription(), true, description -> {
            element.setDescription(description);
            contentView.refresh();
        });
    }

    private void onFolderExport(FileSystemElement element) {
        Mangrypt.getBase().showInfoAlert("Sorry, but this feature is not yet implemented");
    }

    @FXML
    private void onAdd() {
        Set<String> existingNames = contentView.getItems().stream()
                .map(FileSystemElement::getName)
                .collect(Collectors.toSet());

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
            boolean saveSuccess;
            try {
                saveSuccess = ConfigIO.save();
            } catch (Exception e) {
                e.printStackTrace(System.err);
                saveSuccess = false;
            }

            if (saveSuccess) {
                close();
            }
            else {
                Mangrypt.getBase().showAlert(Alert.AlertType.ERROR, "Save Error", "Mangrypt was unable to save your vault. Do you still want to close the it (data may be lost)", true, btn -> {
                    if (btn == ButtonType.YES) {
                        close();
                    }
                }, ButtonType.YES, ButtonType.NO);
            }
        }));
    }

    private void close() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class);
        ConfigIO.cleanup();

        try {
            FXMLLoader loader = new FXMLLoader(Utilities.class.getResource("/io/github/redstonemango/mangrypt/fxml/vault-selection.fxml"));
            Mangrypt.getBase().setSceneRoot(loader.load());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateContentView(@Nullable FileSystemElement selectElement) {
        contentView.getItems().clear();
        contentView.getItems().addAll(
                currentFolder.getContent().stream()
                        .filter(object -> (showHiddenContentProperty.get() || !object.getName().startsWith(".")))
                        .toList());

        if (selectElement != null) {
            contentView.scrollTo(selectElement);
            contentView.getSelectionModel().select(selectElement);
        }
    }
}
