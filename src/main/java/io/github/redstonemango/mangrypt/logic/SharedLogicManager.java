package io.github.redstonemango.mangrypt.logic;

import io.github.redstonemango.mangrypt.Mangrypt;
import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.IOException;

public class SharedLogicManager {

    public static void registerHoverAnimation(Node node) {
        node.getParent().setOnMouseEntered(_ -> {
            FadeTransition transition = new FadeTransition(Duration.millis(250), node);
            transition.setFromValue(1);
            transition.setToValue(0.45);
            transition.play();
        });
        node.getParent().setOnMouseExited(_ -> {
            FadeTransition transition = new FadeTransition(Duration.millis(250), node);
            transition.setFromValue(0.45);
            transition.setToValue(1);
            transition.play();
        });
    }

    public static void showConfigureDialog(ImageView configureImage, BooleanProperty showHiddenContentProperty, boolean folderOverview) {
        CheckMenuItem showHiddenFoldersItem = new CheckMenuItem("Show hidden " + (folderOverview ? "folders" : "datasets"));
        MenuItem passwordChangeItem = new MenuItem("Change password & passphrase");
        MenuItem backMenuItem = new MenuItem("Back to vault overview");
        ContextMenu menu = new ContextMenu(showHiddenFoldersItem, passwordChangeItem, new SeparatorMenuItem(), backMenuItem);
        Point2D imagePos = configureImage.localToScreen(0, 0);
        menu.show(configureImage.getParent(), imagePos.getX(), imagePos.getY());
        if (folderOverview) menu.setX(imagePos.getX() - menu.getWidth());
        else menu.setX(imagePos.getX() + configureImage.getFitWidth());

        showHiddenFoldersItem.setSelected(showHiddenContentProperty.get());
        showHiddenFoldersItem.selectedProperty().addListener((_, _, b) -> showHiddenContentProperty.set(b));

        passwordChangeItem.setOnAction(_ -> {
            try {
                FXMLLoader loader = new FXMLLoader(SharedLogicManager.class.getResource("/io/github/redstonemango/mangrypt/fxml/security-setup.fxml"));
                Mangrypt.getBase().setSecondLayerRoot(loader.load());
            }
            catch (IOException e) {
                throw new RuntimeException(e); // I love happy compilers
            }
        });

        backMenuItem.setOnAction(_ -> Mangrypt.getBase().playTransition(() -> {
            ConfigIO.save();
            ConfigIO.cleanup();

            try {
                FXMLLoader loader = new FXMLLoader(SharedLogicManager.class.getResource("/io/github/redstonemango/mangrypt/fxml/vault-selection.fxml"));
                Mangrypt.getBase().setSceneRoot(loader.load());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

}
