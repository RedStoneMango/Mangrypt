package io.github.redstonemango.mangrypt.back.dataTypes;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import io.github.redstonemango.mangoutils.MangoIO;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.DataView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public abstract class DataElement extends FileSystemElement {

    @Tag(5) @Deprecated(forRemoval = true) byte[] bytes;
    @Tag(6) String fileExtension;
    @Tag(12) byte[][] bytesWrapper;

    public byte[] bytes() {
        Utilities.ensureAuthorizedAccess(DataView.class);
        return bytesWrapper[0];
    }

    public void bytes(byte[] bytes) {
        Utilities.ensureAuthorizedAccess(ContentAdder.class, DataView.class);
        this.bytesWrapper[0] = bytes;
    }

    public String fileExtension() {
        return fileExtension;
    }

    public void fileExtension(String fileExtension) {
        Utilities.ensureAuthorizedAccess(ContentAdder.class);
        this.fileExtension = fileExtension;
    }

    @Override
    public boolean exportTo(File file, boolean isRoot) {
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
            Files.write(file.toPath(), bytes);
        } catch (IOException _) {
            return false;
        }
        return true;
    }

    @Override
    public void ensureFields(String name, FolderElement parent) {
        Utilities.ensureAuthorizedAccess(ContentAdder.class, FolderElement.class);
        super.ensureFields(name, parent);
        if (bytes != null) {
            bytesWrapper = new byte[][]{bytes}; // Copy to wrapper
            bytes = null;
        }
        if (bytesWrapper == null) {
            bytesWrapper = new byte[][]{new byte[0]}; // Dummy init
        }
        if (fileExtension == null) {
            fileExtension = "";
        }
    }

    @Override
    public void zeroOut() {
        super.zeroOut();
        Arrays.fill(bytesWrapper[0], (byte) 0);
        bytesWrapper[0] = null;
        bytesWrapper = null;
    }

    public abstract DataElement symlinkedVersion(String symlinkName);
}
