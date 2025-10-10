package io.github.redstonemango.mangrypt.front;

import io.github.redstonemango.mangoutils.MangoIO;
import io.github.redstonemango.mangrypt.Mangrypt;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Popup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Pattern;

public class WebWrapper extends VBox {

    public static final Pattern WEBSITE_URL_PATTERN = Pattern.compile("^((ftp|http|https)://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(/[^\\s?#]*)?(\\?[^\\s#]*)?(#\\S*)?/?$");

    private final ContextMenu contextMenu;
    private final CheckMenuItem javaScriptMenuItem = new CheckMenuItem("Enable javascript");
    private final File tmpFile;

    public WebWrapper(String link, boolean javaScriptEnabled) throws IOException {
        WebView view = new WebView();
        WebEngine engine = view.getEngine();
        engine.load(link);
        view.setContextMenuEnabled(false);
        tmpFile = Files.createTempDirectory("mgwv").toFile();
        engine.setUserDataDirectory(tmpFile);

        javaScriptMenuItem.setSelected(javaScriptEnabled);
        javaScriptMenuItem.selectedProperty().addListener((_, _, _) -> engine.reload());
        engine.javaScriptEnabledProperty().bindBidirectional(javaScriptMenuItem.selectedProperty());

        MenuItem backMenuItem = new MenuItem("Back In History");
        backMenuItem.setOnAction(_ -> engine.getHistory().go(-1));
        backMenuItem.setDisable(true);
        MenuItem forwardMenuItem = new MenuItem("Forward In History");
        forwardMenuItem.setOnAction(_ -> engine.getHistory().go(1));
        forwardMenuItem.setDisable(true);
        MenuItem reloadMenuItem = new MenuItem("Reload Page");
        reloadMenuItem.setOnAction(_ -> engine.reload());
        MenuItem gotoMenuItem = new MenuItem("!! Open URL !!");
        gotoMenuItem.setOnAction(_ -> {
            Mangrypt.getBase().showConfirmationDialog("Open untrusted URL", "You are about to open an URL unrelated to this dataset. Do you really want to continue?", () -> {
                Mangrypt.getBase().showInputDialog("Please enter the web address / URL to open", "Address", engine.getLocation(), true,
                    s -> WEBSITE_URL_PATTERN.matcher(s).matches(),
                    s -> WEBSITE_URL_PATTERN.matcher(s).matches() ? null : "Not a valid website link",
                    engine::load
                );
            });
        });

        Runnable updateMenuItems = () -> {
            int index = engine.getHistory().getCurrentIndex();
            int size = engine.getHistory().getEntries().size();

            backMenuItem.setDisable(index <= 0);
            forwardMenuItem.setDisable(index >= size - 1);
        };
        engine.getHistory().currentIndexProperty().addListener((_, _, _) -> updateMenuItems.run());


        contextMenu = new ContextMenu(reloadMenuItem, new SeparatorMenuItem(), backMenuItem, forwardMenuItem, new SeparatorMenuItem(), javaScriptMenuItem, new SeparatorMenuItem(), gotoMenuItem);

        setOnMouseClicked(e -> {
            contextMenu.hide();
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(view, e.getScreenX(), e.getScreenY());
            }
        });

        getChildren().addAll(view);

        Popup popup = new Popup();
        BorderPane pane = new BorderPane();
        Label infoLabel1 = new Label("!! This is NOT an encrypted connection !!");
        infoLabel1.setTextFill(Color.RED);
        Label infoLabel2 = new Label("Mangrypt does encrypt the configuration for accessing this site and deletes local storage files on exit.");
        infoLabel2.setWrapText(true);
        infoLabel2.setTextFill(Color.RED);
        Label infoLabel3 = new Label("However, you are NOT anonymous !");
        infoLabel3.setTextFill(Color.RED);
        pane.setCenter(new VBox(5, infoLabel1, infoLabel2, infoLabel3));
        Label closeLabel = new Label("x");
        BorderPane.setMargin(closeLabel, new Insets(0, 5, 5, 5));
        closeLabel.setCursor(Cursor.HAND);
        closeLabel.setOnMouseClicked(_ -> popup.hide());
        pane.setRight(closeLabel);
        pane.setPrefWidth(280);
        pane.setPrefHeight(120);
        pane.setPadding(new Insets(5, 0, 0, 5));
        popup.getContent().add(pane);
        pane.setStyle("-fx-background-color: white;");

        Platform.runLater(() -> {
            double x = view.localToScreen(0, 0).getX() + view.getWidth();
            double y = view.localToScreen(0, 0).getY();
            popup.show(this, x, y);
            popup.setX(x - popup.getWidth());
        });
    }

    public void cleanup(boolean ignoreError) {
        try {
            MangoIO.deleteDirectoryRecursively(tmpFile);
        } catch (IOException e) {
            if (!ignoreError) {
                throw new RuntimeException("Unable to cleanup directory", e);
            }
        }
    }

    public boolean isJavaScriptEnabled() {
        return javaScriptMenuItem.isSelected();
    }

    public BooleanProperty javaScriptEnabledProperty() {
        return javaScriptMenuItem.selectedProperty();
    }

    public void setJavaScriptEnabled(boolean javaScriptEnabled) {
        javaScriptMenuItem.setSelected(javaScriptEnabled);
    }
}
