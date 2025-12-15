package io.github.redstonemango.mangrypt.back.dataTypes;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import io.github.redstonemango.mangoutils.MangoIO;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class SymlinkElement extends FileSystemElement {

    @Tag(11) private String targetPath;

    public String getTargetPath() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class);
        return targetPath;
    }

    public void targetPath(String targetPath) {
        Utilities.ensureAuthorizedAccess(ContentAdder.class);
        this.targetPath = targetPath;
    }

    public FileSystemElement resolveTargetElement() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class);
        FileSystemElement target = FileSystemElement.fromPath(targetPath);
        if (!target.buildPath().equals(targetPath)) return null; // Might be the case if target was deleted and detection logic falls back to parent folder
        return FileSystemElement.fromPath(targetPath);
    }

    @Override
    public Image runIconImageBuild() {
        FileSystemElement target = resolveTargetElement();
        if (target == null) return null;
        return target.runIconImageBuild();
    }

    @Override
    public void ensureFields(String name, FolderElement parent) {
        Utilities.ensureAuthorizedAccess(ContentAdder.class, FolderElement.class);
        super.ensureFields(name, parent);
        if (targetPath == null) {
            targetPath = "";
        }
    }

    @Override
    public void zeroOut() {
        super.zeroOut();
        targetPath = null;
    }

    @Override
    public boolean exportTo(File file, boolean isRoot) {
        FileSystemElement target = resolveTargetElement();
        if (target != null) {
            target.exportTo(file, isRoot);
        }
        return true;
    }

    @Override
    FileSystemElement deepCopy(@Nullable FolderElement parent) {
        SymlinkElement copy = new SymlinkElement();
        copy.name = name;
        copy.description = description;
        copy.parent = parent;
        copy.targetPath = targetPath;

        return copy;
    }
}
