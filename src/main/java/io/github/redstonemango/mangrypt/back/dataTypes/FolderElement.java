package io.github.redstonemango.mangrypt.back.dataTypes;

import io.github.redstonemango.mangoutils.MangoIO;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.Configuration;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public boolean exportTo(File file, boolean isRoot) {
        File tmpDir = isRoot ? MangoIO.getNextAvailableFile(new File(file.getParent(), file.getName() + "_tmp")) : file;

        if (tmpDir.exists()) {
            try {
                MangoIO.deleteDirectoryRecursively(tmpDir);
            }
            catch (IOException _) {
                return false;
            }
        }
        if (file.exists()) {
            try {
                MangoIO.deleteDirectoryRecursively(file);
            }
            catch (IOException _) {
                return false;
            }
        }

        AtomicBoolean success = new AtomicBoolean(tmpDir.mkdirs());
        if (success.get()) {
            content.values().forEach(element -> {
                String name = element.getName();
                if (element instanceof DataElement dataElement) name = name + dataElement.fileExtension();
                File sub = new File(tmpDir, name);
                if (!element.exportTo(sub, false)) {
                    success.set(false);
                }
            });

            if (isRoot) {
                try {
                    MangoIO.compressFile(tmpDir, file);
                    MangoIO.deleteDirectoryRecursively(tmpDir);
                } catch (IOException e) {
                    return false;
                }
            }
            return success.get();
        }
        return false;
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
