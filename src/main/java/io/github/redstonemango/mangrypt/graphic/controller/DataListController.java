package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangoutils.NameConverter;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.ListEntry;
import io.github.redstonemango.mangrypt.logic.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.util.Locale;

public class DataListController {

    private final BooleanProperty showHiddenDataProperty = new SimpleBooleanProperty(false);
    private Configuration.Folder folder;

    @FXML private ListView<SecureData.Encrypted> dataView;
    @FXML private Label vaultNameLabel;
    @FXML private Label folderNameLabel;
    @FXML private ImageView addImage;
    @FXML private ImageView configureImage;
    @FXML private ImageView backImage;
    @FXML private TextField filterField;

    public void init(Configuration.Folder folder) throws IllegalStateException {
        if (this.folder != null) throw new IllegalStateException("Initialisation already happened");
        this.folder = folder;

        Utilities.applyCustomNodeCellFactory(dataView, data -> new ListEntry(data.getName(), data.getDescription(), () -> onOpen(data), () -> onDelete(data), () -> onRename(data), () -> onChangeDescription(data), dataView, data.getIcon()));
        updateDataView();

        String name = ConfigIO.getVaultFile().getName().substring(0, ConfigIO.getVaultFile().getName().length() - ".mgvault".length());
        name = NameConverter.convert(name, NameConverter.NamingConvention.MIXED_CASE_TYPES, NameConverter.NamingConvention.PLAIN_TEXT, true);
        vaultNameLabel.setText(name);

        folderNameLabel.setText(folder.getName());

        showHiddenDataProperty.addListener((_, _, _) -> updateDataView());
        filterField.textProperty().addListener((_, _, _) -> updateDataView());

        Platform.runLater(() -> {
            SharedLogicManager.registerHoverAnimation(backImage);
            SharedLogicManager.registerHoverAnimation(addImage);
            SharedLogicManager.registerHoverAnimation(configureImage);
        });
    }

    private void onOpen(SecureData.Encrypted data) {
        Mangrypt.getBase().showData(folder, data);
    }
    private void onDelete(SecureData.Encrypted data) {
        Mangrypt.getBase().showConfirmationDialog("Delete dataset", "Do you really want to delete the dataset '" + data.getName() + "'?", () -> {
            folder.getEncryptedData().remove(data);
            updateDataView();
        });
    }
    private void onRename(SecureData.Encrypted data) {
        Mangrypt.getBase().showInputDialog("Please set a new name for the dataset", "Name", data.getName(), true, name -> !name.isBlank() && !name.equals("."), name -> name.startsWith(".") ? "Folders starting with . are hidden" : null, name -> {
            data.setName(name);
            dataView.refresh();
        });
    }

    private void onChangeDescription(SecureData.Encrypted data) {
        Mangrypt.getBase().showInputDialog("Please set a new description for the dataset", "Description", data.getDescription(), true, description -> {
            data.setDescription(description);
            dataView.refresh();
        });
    }

    @FXML
    private void onAdd() {
        MenuItem textItem = new MenuItem("Text Data");
        ImageView textImage = new ImageView(SecureData.Encrypted.buildIconImage(SecureData.TYPE_TEXT));
        textImage.setPreserveRatio(true);
        textImage.setFitHeight(20);
        textItem.setGraphic(textImage);
        ContextMenu menu = new ContextMenu(textItem);
        Point2D imagePos = addImage.localToScreen(0, 0);
        menu.show(addImage.getParent(), imagePos.getX() + addImage.getFitWidth(), imagePos.getY());

        textItem.setOnAction(_ -> addTextData());
    }

    private void addTextData() {
        Mangrypt.getBase().showInputDialog("Please set a new name for the dataset", "Name", "", true, name -> !name.isBlank() && !name.equals("."), name -> name.startsWith(".") ? "Folders starting with . are hidden" : null, name -> {
            try {
                SecureData.Encrypted data = SecureData.Encrypted.newEncryptedTextData(name);
                folder.getEncryptedData().add(data);
                updateDataView();
            }
            catch (Exception e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error creating an encrypted text-data object", e);
            }
        });
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(SharedLogicManager.class.getResource("/io/github/redstonemango/mangrypt/fxml/folder-overview.fxml"));
            Mangrypt.getBase().setSceneRoot(loader.load());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void onConfigure() {
        SharedLogicManager.showConfigureDialog(configureImage, showHiddenDataProperty, false);
    }

    private void updateDataView() {
        dataView.getItems().clear();
        dataView.getItems().addAll(folder.getEncryptedData().stream().filter(data -> (showHiddenDataProperty.get() || !data.getName().startsWith(".")) && data.getName().toLowerCase(Locale.ROOT).contains(filterField.getText().toLowerCase(Locale.ROOT))).toList());
    }
}
