package io.github.redstonemango.mangrypt.back.dataTypes;

import io.github.redstonemango.mangrypt.back.Configuration;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import javafx.scene.image.Image;

import java.util.*;

public class FolderElement extends FileSystemElement {

    private Map<String, FileSystemElement> content;

    public static Image buildIconImage() {
        return new Image(Objects.requireNonNull(FolderElement.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/data-type-icon/folder.png"))); // Dummy value for now
    }

    public Map<String, FileSystemElement> getContent() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, Configuration.class, ContentAdder.class);
        return content;
    }

    @Override
    public void ensureFields(FolderElement parent) {
        super.ensureFields(parent);
        if (content == null) {
            content = new HashMap<>();
        }
        content.values().forEach(element -> element.ensureFields(this));
    }

    @Override
    public void zeroOut() {
        super.zeroOut();
        content.values().forEach(FileSystemElement::zeroOut);
        content.clear();
        content = null;
    }
}
