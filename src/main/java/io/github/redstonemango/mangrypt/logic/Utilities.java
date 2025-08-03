package io.github.redstonemango.mangrypt.logic;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.function.Consumer;
import java.util.function.Function;

public class Utilities {

    public static <T> void applyCustomNodeCellFactory(ListView<T> listView, Function<T, Node> nodeFunction) {
        applyCustomNodeCellFactory(listView, nodeFunction, _ -> {});
    }

    public static <T> void applyCustomNodeCellFactory(ListView<T> listView, Function<T, Node> nodeFunction, Consumer<T> onDoubleClick) {
        listView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<T> call(ListView<T> lv) {
                return new ListCell<>() {

                    private long lastClick = -1;

                    @Override
                    protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setGraphic(nodeFunction.apply(item));
                            setPadding(new Insets(0));
                            setOnMouseClicked(_ -> {
                                if (System.currentTimeMillis() - lastClick <= 250) onDoubleClick.accept(getItem());
                                lastClick = System.currentTimeMillis();
                            });
                        }
                    }
                };
            }
        });
    }

}
