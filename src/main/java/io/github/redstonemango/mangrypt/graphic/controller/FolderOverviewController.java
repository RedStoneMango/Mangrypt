package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.logic.ConfigIO;
import io.github.redstonemango.mangrypt.logic.Configuration;
import io.github.redstonemango.mangrypt.logic.Utilities;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.stream.Collectors;

public class FolderOverviewController {

    private boolean showHiddenFolders = false;

    @FXML ListView<Configuration.Folder> folderView;
    @FXML Label addContainer;
    @FXML Label configureContainer;
    @FXML ImageView addImage;
    @FXML ImageView configureImage;

    @FXML
    private void initialize() {
        Utilities.applyCustomNodeCellFactory(folderView, folder -> new FolderListEntry(folder, () -> onFolderOpen(folder), () -> onFolderDelete(folder), folderView));
        updateFolderView();
    }

    private void onFolderOpen(Configuration.Folder folder) {

    }

    private void onFolderDelete(Configuration.Folder folder) {
        Mangrypt.getBase().showConfirmationDialog("Delete folder", "Do you really want to delete the folder '" + folder.getName() + "' among with all data it's containing?", () -> {
            ConfigIO.getConfig().getFolders().remove(folder);
            updateFolderView();
        });
    }


    @FXML
    private void onAddAnimationStart() {
        FadeTransition transition = new FadeTransition(Duration.millis(250), addImage);
        transition.setFromValue(1);
        transition.setToValue(0.45);
        transition.play();
    }
    @FXML
    private void onAddAnimationEnd() {
        FadeTransition transition = new FadeTransition(Duration.millis(250), addImage);
        transition.setFromValue(0.45);
        transition.setToValue(1);
        transition.play();
    }
    @FXML
    private void onConfigureAnimationStart() {
        RotateTransition transition = new RotateTransition(Duration.millis(150), configureImage);
        transition.setFromAngle(0);
        transition.setToAngle(45);
        transition.play();
    }
    @FXML
    private void onConfigureAnimationEnd() {
        RotateTransition transition = new RotateTransition(Duration.millis(150), configureImage);
        transition.setFromAngle(45);
        transition.setToAngle(0);
        transition.play();
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
        ContextMenu menu = new ContextMenu();
        CheckMenuItem showHiddenFoldersItem = new CheckMenuItem("Show hidden folders");
        MenuItem passwordChangeItem = new MenuItem("Change password & passphrase");
        MenuItem backMenuItem = new MenuItem("Back to vault overview");
        menu.getItems().addAll(showHiddenFoldersItem, passwordChangeItem, new SeparatorMenuItem(), backMenuItem);
        Point2D containerPos = configureImage.localToScreen(0, 0);
        menu.show(configureContainer, containerPos.getX(), containerPos.getY() + configureImage.getFitHeight());
        menu.setX(containerPos.getX() - menu.getWidth()); // Now that the layout is loaded, we can re-position the menu

        showHiddenFoldersItem.setSelected(showHiddenFolders);
        showHiddenFoldersItem.selectedProperty().addListener((_, _, b) -> {
            showHiddenFolders = b;
            updateFolderView();
        });

        passwordChangeItem.setOnAction(_ -> {
            try {
                FXMLLoader loader = new FXMLLoader(ConfigIO.class.getResource("/io/github/redstonemango/mangrypt/fxml/security-setup.fxml"));
                Mangrypt.getBase().setSecondLayerRoot(loader.load());
            }
            catch (IOException e) {
                throw new RuntimeException(e); // I love happy compilers
            }
        });

        backMenuItem.setOnAction(_ -> {
            ConfigIO.save();
            ConfigIO.cleanup();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/redstonemango/mangrypt/fxml/vault-selection.fxml"));
                Mangrypt.getBase().setSceneRoot(loader.load());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void updateFolderView() {
        folderView.getItems().clear();
        folderView.getItems().addAll(ConfigIO.getConfig().getFolders().stream().filter(folder -> showHiddenFolders || !folder.getName().startsWith(".")).toList());
    }

}
