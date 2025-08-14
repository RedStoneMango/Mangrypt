package io.github.redstonemango.mangrypt.logic;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import io.github.redstonemango.mangrypt.graphic.DataView;
import io.github.redstonemango.mangrypt.graphic.controller.DataListController;
import io.github.redstonemango.mangrypt.graphic.controller.SecuritySetupController;
import javafx.scene.image.Image;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Objects;

public class SecureData {

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;

    @Expose
    private int type;

    /* --- Text Data --- */
    @Expose
    private char[] text;

    /* --- Image Data --- */
    @Expose
    private byte[] imageBytes;

    static SecureData newTextData() {
        return new SecureData(TYPE_TEXT);
    }

    private SecureData(int type) {
        this.type = type;
        ensureFields();
    }

    public void ensureFields() {
        if (type < 0 || type > 1) {
            type = TYPE_TEXT;
        }

        switch (type) {
            case TYPE_TEXT -> {
                if (text == null) text = new char[0];
            }
            case TYPE_IMAGE -> {
                if (imageBytes == null) imageBytes = new byte[0];
            }
        }
    }
    public char[] text() {
        Utilities.ensureAuthorizedAccess(DataView.class);
        return text;
    }
    public void text(char[] c) {
        Utilities.ensureAuthorizedAccess(DataView.class);
        text = c;
    }
    public byte[] imageBytes() {
        Utilities.ensureAuthorizedAccess(DataView.class);
        return imageBytes;
    }
    public void imageBytes(byte[] b) {
        Utilities.ensureAuthorizedAccess(DataView.class);
        imageBytes = b;
    }
    public void zeroOut() {
        Utilities.ensureAuthorizedAccess(SecureData.class);

        Arrays.fill(text, '\0');
    }





    public static class Encrypted {
        @Expose
        private String name;
        @Expose
        private String description;
        @Expose
        private String content;
        @Expose
        private int type;
        private String newContent;

        public void ensureFields() {
            if (type < 0 || type > 1) {
                type = TYPE_TEXT;
            }
            if (name == null || name.isBlank()) {
                name = "UNNAMED DATASET";
            }
            if (description == null) {
                description = "";
            }
            if (content == null) {
                content = "";
            }
        }

        private Encrypted(int type) {
            this.type = type;
            ensureFields();
        }

        public static Encrypted newEncryptedTextData(String name) throws Exception {
            Encrypted encrypted = new Encrypted(TYPE_TEXT);
            encrypted.setName(name);
            encrypted.store(newTextData());
            return encrypted;
        }

        public SecureData decrypt() throws Exception {
            Utilities.ensureAuthorizedAccess(Encrypted.class, DataListController.class);

            SecretKey key = ConfigIO.getConfig().password();
            String json = CypherEncryption.decryptFromString(content, key);
            SecureData data = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, SecureData.class);
            data.ensureFields();
            return data;
        }

        public void store(SecureData data) throws Exception {
            Utilities.ensureAuthorizedAccess(Encrypted.class);

            String json = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(data);
            content = CypherEncryption.encryptToString(json, ConfigIO.getConfig().password(), ConfigIO.getConfig().passwordSalt());
        }

        public void updatePassword(SecretKey key, byte[] salt) throws Exception {
            Utilities.ensureAuthorizedAccess(SecuritySetupController.class);

            SecureData data = decrypt();
            String json = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(data);
            newContent = CypherEncryption.encryptToString(json, key, salt);
            data.zeroOut();
        }

        public void finalizePasswordUpdate() {
            Utilities.ensureAuthorizedAccess(SecuritySetupController.class);

            if (newContent == null) {
                throw new SecurityException("finalizePasswordUpdate() incorrectly called out-of-context");
            }

            content = newContent;
            newContent = null;
        }

        public byte[] extractSalt() {
            return CypherEncryption.extractSaltFromString(content);
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getType() {
            return type;
        }

        public Image getIcon() {
            return buildIconImage(type);
        }

        public static Image buildIconImage(int type) {
            return new Image(Objects.requireNonNull(SecureData.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/data-type-icon/" + switch (type) {
                case TYPE_TEXT -> "text";
                case TYPE_IMAGE -> "image";
                default -> "";
            } + ".png")));
        }

        public void cleanup() {
            Utilities.ensureAuthorizedAccess(Configuration.class);
        }
    }
}
