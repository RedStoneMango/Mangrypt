package io.github.redstonemango.mangrypt.logic;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.function.Function;

public class Utilities {

    public static <T> void applyCustomNodeCellFactory(ListView<T> listView, Function<T, Node> nodeFunction) {
        listView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<T> call(ListView<T> lv) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setGraphic(nodeFunction.apply(item));
                            setPadding(new Insets(0));
                        }
                    }
                };
            }
        });
    }

}
