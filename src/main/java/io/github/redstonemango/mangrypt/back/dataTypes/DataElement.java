package io.github.redstonemango.mangrypt.back.dataTypes;

import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.DataView;

import java.util.Arrays;

public abstract class DataElement extends FileSystemElement {

    byte[] bytes;
    String fileExtension;

    public byte[] bytes() {
        Utilities.ensureAuthorizedAccess(DataView.class);
        return bytes;
    }

    public void bytes(byte[] bytes) {
        Utilities.ensureAuthorizedAccess(ContentAdder.class, DataView.class);
        this.bytes = bytes;
    }

    public String fileExtensions() {
        return fileExtension;
    }

    public void fileExtension(String fileExtension) {
        Utilities.ensureAuthorizedAccess(ContentAdder.class);
        this.fileExtension = fileExtension;
    }

    @Override
    public void ensureFields() {
        super.ensureFields();
        if (bytes == null) {
            bytes = new byte[0]; // Dummy init
        }
        if (fileExtension == null) {
            fileExtension = "";
        }
    }

    @Override
    public void zeroOut() {
        super.zeroOut();
        Arrays.fill(bytes, (byte) 0);
    }
}
