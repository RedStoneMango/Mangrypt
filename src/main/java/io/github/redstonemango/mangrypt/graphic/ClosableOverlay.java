package io.github.redstonemango.mangrypt.graphic;

import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public class ClosableOverlay {

    public static void apply(Pane root, Region panelContainer, Runnable onCancel) {
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                onCancel.run();
            }
        });
        root.setOnMouseClicked(e -> {
            Point2D scenePos = panelContainer.localToScene(0, 0);
            double width = panelContainer.getWidth();
            double height = panelContainer.getHeight();
            if (!(e.getSceneX() >= scenePos.getX()
                    && e.getSceneX() < scenePos.getX() + width
                    && e.getSceneY() >= scenePos.getY()
                    && e.getSceneY() < scenePos.getY() + height))
            {
                onCancel.run();
            }
        });
    }
}
