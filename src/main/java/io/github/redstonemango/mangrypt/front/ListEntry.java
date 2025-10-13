package io.github.redstonemango.mangrypt.front;

import io.github.redstonemango.mangrypt.front.elementBase.ListEntryBase;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.jetbrains.annotations.Nullable;

public class ListEntry extends ListEntryBase {

    public static final Image ARROW_IMAGE = new Image(ListEntry.class.getResource("/io/github/redstonemango/mangrypt/image/arrow.png").toExternalForm());

    private final Runnable onSelect;
    private final Runnable onDelete;

    public ListEntry(String name, String description, Runnable onSelect, Runnable onDelete, Runnable onSetName,
                     Runnable onSetDescription, Runnable onExport, @Nullable Image icon, boolean folder,
                     ListView<?> view) {
        nameLabel.setText(name);
        if (name.startsWith(".")) nameLabel.setFont(Font.font("", FontWeight.NORMAL, FontPosture.ITALIC, 17));
        descriptionLabel.setText(description);
        this.onSelect = onSelect;
        this.onDelete = onDelete;
        prefWidthProperty().bind(view.widthProperty().subtract(30));
        if (description == null || description.isBlank()) {
            nameLabel.setTranslateY(7); // If there is no description, shift the name label down
            descriptionLabel.setVisible(false);
        }

        getStyleClass().add("element-list-entry");
        if (folder) {
            getStyleClass().add("folder");
            ((ImageView) selectButton.getGraphic()).setImage(ARROW_IMAGE);
        }

        if (icon == null) {
            setLeft(null);
        }
        else {
            iconView.setImage(icon);
            Platform.runLater(() -> {
                double target = nameLabel.localToScene(0, 0).getY();
                double element = iconView.localToScene(0, 0).getY();
                double difference = target - element;
                iconView.setTranslateY(difference - 5);
                iconView.setTranslateX(5);
            });
        }

        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(_ -> onSelect.run());
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(_ -> onDelete.run());
        MenuItem nameItem = new MenuItem("Change Name");
        nameItem.setOnAction(_ -> onSetName.run());
        MenuItem descriptionItem = new MenuItem("Change Description");
        descriptionItem.setOnAction(_ -> onSetDescription.run());
        MenuItem exportItem = new MenuItem("Export");
        exportItem.setOnAction(_ -> onExport.run());
        ContextMenu menu = new ContextMenu(openItem, deleteItem, new SeparatorMenuItem(), nameItem, descriptionItem, new SeparatorMenuItem(), exportItem);
        setOnMouseClicked(e -> {
            if (menu.isShowing()) menu.hide();
            if (e.getButton() == MouseButton.SECONDARY) {
                menu.show(this, e.getScreenX(), e.getScreenY());
            }
        });
    }

    @Override
    protected void onSelect(ActionEvent actionEvent) {
        onSelect.run();
    }

    @Override
    protected void onDelete(ActionEvent actionEvent) {
        onDelete.run();
    }
}
