package io.github.redstonemango.mangrypt.graphic.controller;

import io.github.redstonemango.mangrypt.graphic.elementBase.FolderListEntryBase;
import io.github.redstonemango.mangrypt.logic.Configuration;
import javafx.event.ActionEvent;
import javafx.scene.control.ListView;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class FolderListEntry extends FolderListEntryBase {

    private final Runnable onSelect;
    private final Runnable onDelete;

    public FolderListEntry(Configuration.Folder folder, Runnable onSelect, Runnable onDelete, ListView<?> view) {
        nameLabel.setText(folder.getName());
        if (folder.getName().startsWith(".")) nameLabel.setFont(Font.font("", FontWeight.NORMAL, FontPosture.ITALIC, 17));
        descriptionLabel.setText(folder.getDescription());
        this.onSelect = onSelect;
        this.onDelete = onDelete;
        prefWidthProperty().bind(view.widthProperty().subtract(16));
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
