package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.logic.ConfigIO;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.IOException;

public class FolderOverviewController {

    @FXML ListView<Void> folderView;
    @FXML Label addContainer;
    @FXML Label configureContainer;
    @FXML ImageView addImage;
    @FXML ImageView configureImage;

    @FXML
    private void initialize() {

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

    }
    @FXML
    private void onConfigure() {
        try {
            FXMLLoader loader = new FXMLLoader(ConfigIO.class.getResource("/io/github/redstonemango/mangrypt/fxml/security-setup.fxml"));
            Mangrypt.getBase().setSecondLayerRoot(loader.load());
        }
        catch (IOException e) {
            throw new RuntimeException(e); // I love happy compilers
        }
    }

}
