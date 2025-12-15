package io.github.redstonemango.mangrypt.back.dataTypes;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import io.github.redstonemango.mangoutils.MangoIO;
import io.github.redstonemango.mangrypt.back.Configuration;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class FolderElement extends FileSystemElement {

    @Tag(4) private Map<String, FileSystemElement> content;

    public static Image buildIconImage() {
        return new Image(Objects.requireNonNull(FolderElement.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/data-type-icon/folder.png"))); // Dummy value for now
    }

    public Map<String, FileSystemElement> getContent() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, Configuration.class, ContentAdder.class);
        return content;
    }

    @Override
    public boolean exportTo(File file, boolean isRoot, Set<FileSystemElement> visited) {
        visited.add(this);
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
                if (!element.exportTo(sub, false, visited)) {
                    success.set(false);
                }
            });

            if (isRoot) {
                try {
                    MangoIO.compressFile(tmpDir, file);
                } catch (IOException e) {
                    success.set(false);
                }
                try {
                    MangoIO.deleteDirectoryRecursively(tmpDir);
                } catch (IOException e) {
                    success.set(false);
                }
            }
            return success.get();
        }
        return false;
    }

    public FolderElement symlinkedVersion(String symlinkName, FolderElement symlinkParent) {
        FolderElement element = new FolderElement();
        element.name = symlinkName;
        element.parent = symlinkParent;
        element.content = content;
        return element;
    }

    @Override
    public void ensureFields(String name, FolderElement parent) {
        Utilities.ensureAuthorizedAccess(ContentAdder.class, FolderElement.class);
        super.ensureFields(name, parent);
        if (content == null) {
            content = new HashMap<>();
        }
        content.forEach((elementName, element) -> element.ensureFields(elementName, this));
    }

    @Override
    public void zeroOut() {
        super.zeroOut();
        content.values().forEach(FileSystemElement::zeroOut);
        content.clear();
        content = null;
    }

    @Override
    FileSystemElement deepCopy(FolderElement parent) {
        FolderElement copy = new FolderElement();
        copy.name = name;
        copy.description = description;
        copy.parent = parent;
        copy.content = new HashMap<>();

        content.values().forEach(element -> {
            copy.getContent().put(element.getName(), element.deepCopy(this));
        });
        return copy;
    }
}
