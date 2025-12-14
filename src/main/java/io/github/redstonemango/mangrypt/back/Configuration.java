package io.github.redstonemango.mangrypt.back;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import io.github.redstonemango.mangrypt.back.dataTypes.FolderElement;
import io.github.redstonemango.mangrypt.back.encryption.VersionedEncryptionHandler;
import io.github.redstonemango.mangrypt.front.controller.AuthenticationController;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import io.github.redstonemango.mangrypt.front.controller.OverlayController;

import javax.crypto.SecretKey;

public class Configuration {

    private transient SecretKey masterKey;
    private transient byte[] masterSalt;
    private transient String hash;
    @Tag(1) private FolderElement rootFolder;
    @Tag(7) private boolean renderDataBG;
    @Tag(8) private boolean descriptionOnDownload;
    @Tag(9) private boolean wraparoundNavigation;
    @Tag(10) private boolean showHidden;

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

    public boolean renderDataBG() {
        return renderDataBG;
    }
    public void renderDataBG(boolean renderDataBG) {
        this.renderDataBG = renderDataBG;
    }
    public boolean descriptionOnDownload() {
        return descriptionOnDownload;
    }
    public void descriptionOnDownload(boolean descriptionOnDownload) {
        this.descriptionOnDownload = descriptionOnDownload;
    }

    public void cleanup() {
        Utilities.ensureAuthorizedAccess(ConfigIO.class);

        masterKey = null;
        if (rootFolder != null) rootFolder.zeroOut();
        rootFolder = null;
    }

    public boolean verifyPassword(char[] password) {
        Utilities.ensureAuthorizedAccess(AuthenticationController.class, OverlayController.class);

        return VersionedEncryptionHandler.verifyHash(ConfigIO.VERSION, hash, password);
    }

    public FolderElement getRootFolder() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, FolderElement.class);
        return rootFolder;
    }

    public void ensureFields() {
        if (hash == null) {
            hash = "";
        }
        if (rootFolder == null) {
            rootFolder = new FolderElement();
        }
        rootFolder.ensureFields("Root", null);

        // Master key & salt have to be set separately
    }

    public boolean isWraparoundNavigation() {
        return wraparoundNavigation;
    }

    public void setWraparoundNavigation(boolean wraparoundNavigation) {
        this.wraparoundNavigation = wraparoundNavigation;
    }

    public boolean isShowHidden() {
        return showHidden;
    }

    public void setShowHidden(boolean showHidden) {
        this.showHidden = showHidden;
    }
}
