package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangoutils.NameConverter;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.ListEntry;
import io.github.redstonemango.mangrypt.logic.ConfigIO;
import io.github.redstonemango.mangrypt.logic.Configuration;
import io.github.redstonemango.mangrypt.logic.Utilities;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.util.Locale;

public class FolderOverviewController {

    private final BooleanProperty showHiddenFoldersProperty = new SimpleBooleanProperty(false);

    @FXML ListView<Configuration.Folder> folderView;
    @FXML Label nameLabel;
    @FXML ImageView addImage;
    @FXML ImageView configureImage;
    @FXML TextField filterField;

    @FXML
    private void initialize() {
        Utilities.applyCustomNodeCellFactory(folderView, folder -> new ListEntry(folder.getName(), folder.getDescription(), () -> onFolderOpen(folder), () -> onFolderDelete(folder), () -> onFolderRename(folder), () -> onFolderChangeDescription(folder), folderView, null));
        updateFolderView();

        String name = ConfigIO.getVaultFile().getName().substring(0, ConfigIO.getVaultFile().getName().length() - ".mgvault".length());
        name = NameConverter.convert(name, NameConverter.NamingConvention.MIXED_CASE_TYPES, NameConverter.NamingConvention.PLAIN_TEXT, true);
        nameLabel.setText(name);

        showHiddenFoldersProperty.addListener((_, _, _) -> updateFolderView());
        filterField.textProperty().addListener((_, _, _) -> updateFolderView());

        Platform.runLater(() -> {
            Utilities.registerHoverAnimation(configureImage);
            Utilities.registerHoverAnimation(addImage);
        });
    }

    private void onFolderOpen(Configuration.Folder folder) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/data-list.fxml"));
            Mangrypt.getBase().setSceneRoot(loader.load());
            DataListController controller = loader.getController();
            controller.init(folder);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onFolderDelete(Configuration.Folder folder) {
        Mangrypt.getBase().showConfirmationDialog("Delete folder", "Do you really want to delete the folder '" + folder.getName() + "' along with all contained data?", () -> {
            ConfigIO.getConfig().getFolders().remove(folder);
            updateFolderView();
        });
    }

    private void onFolderRename(Configuration.Folder folder) {
        Mangrypt.getBase().showInputDialog("Please set a new name for the folder", "Folder name", folder.getName(), true, name -> !name.isBlank() && !name.equals("."), name -> name.startsWith(".") ? "Folders starting with . are hidden" : null, name -> {
            folder.setName(name);
            updateFolderView();
        });
    }

    private void onFolderChangeDescription(Configuration.Folder folder) {
        Mangrypt.getBase().showInputDialog("Please set a new description for the folder", "Description", folder.getDescription(), true, description -> {
            folder.setDescription(description);
            folderView.refresh();
        });
    }

    @FXML
    private void onAdd() {
        Mangrypt.getBase().showInputDialog("Please set a name for the new folder", "Folder name", "", true, name -> !name.isBlank() && !name.equals("."), name -> name.startsWith(".") ? "Folders starting with . are hidden" : null, name -> {
            Configuration.Folder folder = new Configuration.Folder(name);
            ConfigIO.getConfig().getFolders().add(folder);
            updateFolderView();
        });
    }
    @FXML
    private void onConfigure() {
        Utilities.showConfigureDialog(configureImage, showHiddenFoldersProperty, true);
    }

    private void updateFolderView() {
        folderView.getItems().clear();
        folderView.getItems().addAll(ConfigIO.getConfig().getFolders().stream().filter(folder -> (showHiddenFoldersProperty.get() || !folder.getName().startsWith(".")) && folder.getName().toLowerCase(Locale.ROOT).contains(filterField.getText().toLowerCase(Locale.ROOT))).toList());
    }
}
