package io.github.redstonemango.mangrypt.back;

import io.github.redstonemango.mangrypt.back.dataTypes.FileSystemElement;
import io.github.redstonemango.mangrypt.back.dataTypes.FolderElement;
import io.github.redstonemango.mangrypt.back.encryption.VersionedEncryptionHandler;
import io.github.redstonemango.mangrypt.front.controller.AuthenticationController;

import javax.crypto.SecretKey;

public class Configuration {

    private transient SecretKey masterKey;
    private transient byte[] masterSalt;
    private String hash;
    private FolderElement rootFolder;

    public SecretKey masterKey() {
        Utilities.ensureAuthorizedAccess(VersionedEncryptionHandler.class);
        return masterKey;
    }
    public byte[] masterSalt() {
        return masterSalt;
    }
    public void updateMasterKey(SecretKey key, byte[] salt) {
        Utilities.ensureAuthorizedAccess(VersionedEncryptionHandler.class);
        this.masterKey = key;
        this.masterSalt = salt;
    }
    public void definePassword(String hash) {
        Utilities.ensureAuthorizedAccess(ConfigIO.class);
        this.hash = hash;
    }

    public void cleanup() {
        Utilities.ensureAuthorizedAccess(ConfigIO.class);

        masterKey = null;
        if (rootFolder != null) rootFolder.getContent().forEach(FileSystemElement::zeroOut);
        rootFolder = null;
    }

    public boolean verifyPassword(char[] password) {
        Utilities.ensureAuthorizedAccess(AuthenticationController.class);

        return VersionedEncryptionHandler.verifyHash(ConfigIO.VERSION, hash, password);
    }

    public FolderElement getRootFolder() {
        return rootFolder;
    }

    public void ensureFields() {
        if (hash == null) {
            hash = "";
        }
        if (rootFolder == null) {
            rootFolder = new FolderElement();
        }
        rootFolder.ensureFields();

        // Master key & salt have to be set separately
    }
}
