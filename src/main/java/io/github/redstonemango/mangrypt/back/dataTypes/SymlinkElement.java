package io.github.redstonemango.mangrypt.back.dataTypes;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.DataView;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import org.jetbrains.annotations.Nullable;

import java.io.File;

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
        return FileSystemElement.fromPath(targetPath);
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
        return false; // TODO
    }

    @Override
    FileSystemElement deepCopy(@Nullable FolderElement parent) {
        return null; // TODO
    }
}
