package io.github.redstonemango.mangrypt.logic;

import com.google.gson.annotations.Expose;
import io.github.redstonemango.mangrypt.graphic.controller.AuthController;
import io.github.redstonemango.mangrypt.graphic.controller.SecuritySetupController;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Configuration {

    private SecretKey passphrase;
    private byte[] passphraseSalt;
    private SecretKey password;
    private byte[] passwordSalt;
    @Expose
    private String hash;
    @Expose
    private List<Folder> folders;

    public SecretKey passphrase() {
        Utilities.ensureAuthorizedAccess(ConfigIO.class);
        return passphrase;
    }
    public byte[] passphraseSalt() {
        return passphraseSalt;
    }
    public void updatePassphrase(char[] passphrase) throws Exception {
        Utilities.ensureAuthorizedAccess(ConfigIO.class);

        passphraseSalt = CypherEncryption.generateRandomSalt();
        this.passphrase = CypherEncryption.deriveKey(passphrase, passphraseSalt);
    }
    public void updatePassphrase(SecretKey key, byte[] salt) {
        Utilities.ensureAuthorizedAccess(SecuritySetupController.class);

        passphraseSalt = salt;
        this.passphrase = key;
    }

    protected SecretKey password() {
        Utilities.ensureAuthorizedAccess(SecureData.Encrypted.class);
        return password;
    }

    protected byte[] passwordSalt() {
        return passwordSalt;
    }
    public void updatePassword(char[] password) throws Exception {
        Utilities.ensureAuthorizedAccess(SecuritySetupController.class);

        hash = Hasher.hash(password);
        passwordSalt = CypherEncryption.generateRandomSalt();
        this.password = CypherEncryption.deriveKey(password, passwordSalt);
    }
    public void updatePassword(SecretKey key, byte[] salt, char[] pwd) throws Exception {
        Utilities.ensureAuthorizedAccess(SecuritySetupController.class, AuthController.class);

        hash = Hasher.hash(pwd);
        passwordSalt = salt;
        this.password = key;
    }

    public void cleanup() {
        Utilities.ensureAuthorizedAccess(ConfigIO.class);

        passphrase = null;
        password = null;
        if (folders != null) folders.forEach(Folder::cleanup);
        folders = null;

    }

    public boolean verifyPassword(char[] password) throws Exception {
        Utilities.ensureAuthorizedAccess(AuthController.class);

        return Hasher.verifyHash(password, hash);
    }

    public List<Folder> getFolders() {
        return folders;
    }

    public void ensureFields() {
        if (hash == null) {
            hash = "";
        }
        if (folders == null) {
            folders = new ArrayList<>();
        }
        else {
            folders.forEach(Folder::ensureFields);
        }
    }

    public static class Folder {

        @Expose
        private String name;
        @Expose
        private String description;
        @Expose
        private List<SecureData.Encrypted> data;

        public Folder(String name) {
            this.name = name;
            this.description = "";
            ensureFields();
        }

        public List<SecureData.Encrypted> getEncryptedData() {
            return data;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void ensureFields() {
            if (name == null || name.isBlank()) {
                name = "UNNAMED VAULT";
            }
            if (description == null) {
                description = "";
            }
            if (data == null) {
                data = new ArrayList<>();
            }
            else {
                data.forEach(SecureData.Encrypted::ensureFields);
            }
        }

        public void cleanup() {
            Utilities.ensureAuthorizedAccess(Configuration.class);

            if (data != null) data.forEach(SecureData.Encrypted::cleanup);
            data = null;
        }
    }

}
