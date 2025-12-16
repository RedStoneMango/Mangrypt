package io.github.redstonemango.mangrypt.back.dataTypes;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import io.github.redstonemango.mangoutils.MangoIO;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.PathCompletion;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Set;

public class SymlinkElement extends FileSystemElement {

    @Tag(11) private String targetPath;

    public String getTargetPath() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, FileSystemElement.class);
        return targetPath;
    }

    public void targetPath(String targetPath) {
        Utilities.ensureAuthorizedAccess(ContentAdder.class, FileSystemController.class);
        this.targetPath = targetPath;
    }

    public FileSystemElement resolveTargetElement() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, PathCompletion.class, FileSystemElement.class);
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

    public static Image buildIconImage() {
        return new Image(Objects.requireNonNull(MediaDataElement.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/data-type-icon/symlink.png")));
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
    public boolean exportTo(File file, boolean isRoot, Set<FileSystemElement> visited) {
        FileSystemElement target = resolveTargetElement();
        if (target != null) {
            if (target instanceof FolderElement && visited.contains(target)) {
                if (file.exists()) {
                    try {
                        MangoIO.deleteDirectoryRecursively(file);
                    } catch (IOException _) {
                        return false;
                    }
                }

                try {
                    if (!file.createNewFile()) {
                        return false;
                    }
                } catch (IOException _) {
                    return false;
                }

                try {
                    Files.writeString(file.toPath(), "This is a link to '" + targetPath + "'.\n" +
                            "This file was automatically generated to prevent directory recursion during the export process.");
                } catch (IOException _) {
                    return false;
                }
                return true;
            }

            return target.exportTo(file, isRoot, visited);
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
