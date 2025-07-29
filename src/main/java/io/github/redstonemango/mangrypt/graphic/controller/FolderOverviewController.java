package io.github.redstonemango.mangrypt.graphic.controller;

import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class FolderOverviewController {

    @FXML ListView<Void> folderView;
    @FXML Label addContainer;
    @FXML Label configureContainer;
    @FXML ImageView addImage;
    @FXML ImageView configureImage;


    @FXML
    private void onAddAnimationStart() {
        FadeTransition transition = new FadeTransition(Duration.millis(250), addImage);
        transition.setFromValue(1);
        transition.setToValue(0.57);
        transition.play();
    }
    @FXML
    private void onAddAnimationEnd() {
        FadeTransition transition = new FadeTransition(Duration.millis(250), addImage);
        transition.setFromValue(0.57);
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

    }

}
