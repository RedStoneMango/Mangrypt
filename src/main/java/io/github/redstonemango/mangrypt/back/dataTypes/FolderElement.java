package io.github.redstonemango.mangrypt.back.dataTypes;

import io.github.redstonemango.mangrypt.back.Configuration;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FolderElement extends FileSystemElement {

    private Map<String, FileSystemElement> content;
    private transient @Nullable FolderElement parent;

    public static Image buildIconImage() {
        return new Image(Objects.requireNonNull(FolderElement.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/data-type-icon/folder.png"))); // Dummy value for now
    }

    public Map<String, FileSystemElement> getContent() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, Configuration.class, ContentAdder.class);
        return content;
    }

    public @Nullable FolderElement getParent() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class);
        return parent;
    }

    public void registerParent(FolderElement parent) {
        Utilities.ensureAuthorizedAccess(FolderElement.class, ContentAdder.class);
        if (this.parent != null) throw new IllegalStateException("The folder does already have a parent registered");
        this.parent = parent;
    }

    @Override
    public void ensureFields() {
        super.ensureFields();
        if (content == null) {
            content = new HashMap<>();
        }
        content.values().forEach(element -> {
            element.ensureFields();
            if (element instanceof FolderElement subFolder) subFolder.registerParent(this);
        });
    }

    @Override
    public void zeroOut() {
        super.zeroOut();
        content.values().forEach(FileSystemElement::zeroOut);
        content.clear();
        content = null;
    }
}
