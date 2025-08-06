package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangoutils.OperatingSystem;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.logic.ConfigIO;
import io.github.redstonemango.mangrypt.logic.Utilities;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.Objects;
import java.util.regex.Pattern;

public class VaultSelectionController {

    public static final Pattern NAME_PATTERN = Pattern.compile("^[^\\\\/:*?\"<>|\\s]+$");

    @FXML
    private ImageView background;
    @FXML
    private ListView<File> vaultList;
    @FXML
    private Button openVaultButton;
    @FXML
    private Button deleteVaultButton;
    @FXML
    private TextField filterField;

    @FXML
    private void initialize() {
        Utilities.applyCustomNodeCellFactory(vaultList, file -> {
            Label label = new Label(file.getName());
            label.setOnMouseEntered(_ -> label.setUnderline(true));
            label.setOnMouseExited(_ -> label.setUnderline(false));
            label.setOnMouseClicked(_ -> OperatingSystem.loadCurrentOS().browse(file));
            label.setCursor(Cursor.HAND);
            label.setTextFill(Color.LIGHTGREEN);
            label.setPadding(new Insets(0, 0, 0, 10));
            return label;
        }, file -> {
            vaultList.getSelectionModel().select(file);
            onOpen();
        });

        updateVaultList();
        vaultList.getSelectionModel().selectedItemProperty().addListener((_, _, f) -> {
            openVaultButton.setDisable(f == null);
            deleteVaultButton.setDisable(f == null);
        });
        vaultList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !vaultList.getSelectionModel().isEmpty()) {
                onOpen();
            }
        });

        filterField.textProperty().addListener((_, _, _) -> updateVaultList());
    }

    private void updateVaultList() {
        vaultList.getItems().clear();
        vaultList.getItems().addAll(ConfigIO.getVaultDirectory().listFiles((_, name) -> name.endsWith(".mgvault") && name.contains(filterField.getText())));
    }

    @FXML
    private void onOpen() {
        ConfigIO.useVault(Objects.requireNonNull(vaultList.getSelectionModel().getSelectedItem(), "Selected file must not be null"));
        ConfigIO.authenticateUserAndLoadConfig();
    }

    @FXML
    private void onDelete() {
        File file = vaultList.getSelectionModel().getSelectedItem();
        Mangrypt.getBase().showConfirmationDialog("Delete Vault", "Do you really want to delete '" + file.getName() + "'?", () -> {
            file.delete();
            updateVaultList();
        });
    }

    @FXML
    private void onAdd() {
        Mangrypt.getBase().showInputDialog("Please enter the name for the new vault's file", "File name", "", true, name -> NAME_PATTERN.matcher(name).matches() && !buildAbstractVaultFile(name).exists(), name -> {
            if (!name.isEmpty() && !NAME_PATTERN.matcher(name).matches()) return "Invalid characters in file name";
            if (buildAbstractVaultFile(name).exists()) return "This file already exists";
            return null;
        }, name -> {
            ConfigIO.useVault(buildAbstractVaultFile(name));
            ConfigIO.authenticateUserAndLoadConfig();
        });
    }

    private static File buildAbstractVaultFile(String name) {
        String filename = name.endsWith(".mgvault") ? name : name + ".mgvault";
        return new File(ConfigIO.getVaultDirectory(), filename);
    }

    @FXML
    private void onOpenDirectory() {
        OperatingSystem.loadCurrentOS().open(ConfigIO.getVaultDirectory());
    }
}