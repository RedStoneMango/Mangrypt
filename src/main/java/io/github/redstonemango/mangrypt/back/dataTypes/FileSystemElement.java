package io.github.redstonemango.mangrypt.back.dataTypes;

import io.github.redstonemango.mangrypt.back.Configuration;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.DataView;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;

public abstract class FileSystemElement {

    private String name;
    private @Nullable String description;

    public @Nullable String getDescription() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class);
        return description;
    }

    public void setDescription(@Nullable String description) {
        Utilities.ensureAuthorizedAccess(FileSystemController.class);
        this.description = description;
    }

    public String getName() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, DataView.class);
        return name;
    }

    public void setName(String name) {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, ContentAdder.class);
        this.name = name;
    }

    public @Nullable Image runIconImageBuild() {
        return switch (this) {
            case FolderElement _ -> FolderElement.buildIconImage();
            case TextDataElement _ -> TextDataElement.buildIconImage();
            case ImageDataElement _ -> ImageDataElement.buildIconImage();
            case MediaDataElement _ -> MediaDataElement.buildIconImage();
            default -> null;
        };
    }

    public void ensureFields() {
        if (name == null || name.isBlank() || name.equals(".")) {
            name = "UNNAMED ENTITY";
        }
        if (description != null && description.isBlank()) {
            description = null; // If existing description is blank, trat it as unexisting
        }
    }

    public void zeroOut() {
        Utilities.ensureAuthorizedAccess(Configuration.class, DataView.class);
        name = null;
        description = null;
    }
}
