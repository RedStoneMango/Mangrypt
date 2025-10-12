package io.github.redstonemango.mangrypt.back.dataTypes;

import io.github.redstonemango.mangrypt.back.Configuration;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FolderElement extends FileSystemElement {

    private List<FileSystemElement> content;

    public static Image buildIconImage() {
        return new Image(Objects.requireNonNull(FolderElement.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/data-type-icon/folder.png"))); // Dummy value for now
    }

    public List<FileSystemElement> getContent() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, Configuration.class, ContentAdder.class);
        return content;
    }

    @Override
    public void ensureFields() {
        super.ensureFields();
        if (content == null) {
            content = new ArrayList<>();
        }
        content.forEach(FileSystemElement::ensureFields);
    }

    @Override
    public void zeroOut() {
        super.zeroOut();
        content.forEach(FileSystemElement::zeroOut);
        content.clear();
        content = null;
    }
}
