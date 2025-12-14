package io.github.redstonemango.mangrypt.front;

import io.github.redstonemango.mangrypt.back.PseudoClipboard;
import io.github.redstonemango.mangrypt.back.dataTypes.FileSystemElement;
import io.github.redstonemango.mangrypt.front.elementBase.ListEntryBase;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ListEntry extends ListEntryBase {

    public static final Image ARROW_IMAGE = new Image(ListEntry.class.getResource("/io/github/redstonemango/mangrypt/image/arrow.png").toExternalForm());

    private final Runnable onSelect;
    private final Runnable onDelete;

    public ListEntry(String name, String description, Runnable onSelect, Runnable onDelete, Runnable onDeleteSelection,
                     Runnable onSetName, Runnable onSetDescription, Runnable onExport, Runnable onExportParent,
                     Runnable onCopy, Runnable onCut, Runnable onPaste, Runnable onPasteInto, Runnable onClone,
                     Runnable onSymlink, @Nullable Image icon, boolean folder, boolean symlink, PseudoClipboard clipboard,
                     List<FileSystemElement> selectedElements, ListView<?> view) {
        nameLabel.setText(name);
        if (name.startsWith(".")) nameLabel.setFont(Font.font("", FontWeight.NORMAL, FontPosture.ITALIC, 17));
        descriptionLabel.setText(description);
        this.onSelect = onSelect;
        this.onDelete = onDelete;
        prefWidthProperty().bind(view.widthProperty().subtract(32));
        if (description == null || description.isBlank()) {
            nameLabel.setTranslateY(7); // If there is no description, shift the name label down
            descriptionLabel.setVisible(false);
        }

        if (folder) {
            ((ImageView) selectButton.getGraphic()).setImage(ARROW_IMAGE);
        }

        if (icon == null) {
            setLeft(null);
        }
        else {
            iconView.setImage(icon);
        }

        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(_ -> onSelect.run());
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(_ -> onDeleteSelection.run());
        MenuItem nameItem = new MenuItem("Change Name");
        nameItem.setOnAction(_ -> onSetName.run());
        MenuItem descriptionItem = new MenuItem("Change Description");
        descriptionItem.setOnAction(_ -> onSetDescription.run());
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(_ -> onCopy.run());
        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setOnAction(_ -> onCut.run());
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setOnAction(_ -> onPaste.run());
        MenuItem pasteIntoItem = new MenuItem("Paste Into");
        pasteIntoItem.setOnAction(_ -> onPasteInto.run());
        MenuItem cloneItem = new MenuItem("Clone");
        cloneItem.setOnAction(_ -> onClone.run());
        MenuItem exportItem = new MenuItem("Export Item");
        exportItem.setOnAction(_ -> onExport.run());
        MenuItem exportParentItem = new MenuItem("Export Current Folder");
        exportParentItem.setOnAction(_ -> onExportParent.run());
        Menu editMenu = new Menu("Edit");
        editMenu.getItems().addAll(nameItem, descriptionItem);
        Menu clipboardMenu = new Menu("Clipboard");
        clipboardMenu.getItems().addAll(copyItem, cutItem, pasteItem, cloneItem);
        if (folder) clipboardMenu.getItems().add(3, pasteIntoItem);
        MenuItem symlinkMenu = new MenuItem("Create Symlink");
        symlinkMenu.setOnAction(_ -> onSymlink.run());
        symlinkMenu.setDisable(symlink);
        Menu exportMenu = new Menu("Export");
        exportMenu.getItems().addAll(exportItem, exportParentItem);
        ContextMenu menu = new ContextMenu(
                openItem, deleteItem, symlinkMenu,
                new SeparatorMenuItem(),
                editMenu,
                new SeparatorMenuItem(),
                clipboardMenu,
                new SeparatorMenuItem(),
                exportMenu
        );
        setOnMouseClicked(e -> {
            if (menu.isShowing()) menu.hide();
            if (e.getButton() == MouseButton.SECONDARY) {
                pasteItem.setDisable(clipboard.isEmpty());
                pasteIntoItem.setDisable(clipboard.isEmpty());
                openItem.setDisable(selectedElements.size() > 1);
                exportItem.setText("Export Item" + (selectedElements.size() > 1 ? "s" : ""));
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
