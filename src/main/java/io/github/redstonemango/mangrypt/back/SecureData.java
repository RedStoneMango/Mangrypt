package io.github.redstonemango.mangrypt.back;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import io.github.redstonemango.mangrypt.front.DataView;
import io.github.redstonemango.mangrypt.front.controller.DataListController;
import io.github.redstonemango.mangrypt.front.controller.SecuritySetupController;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKey;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

public class SecureData {

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_MEDIA = 2;

    @Expose
    private int type;

    /* --- Text Data --- */
    @Expose
    private char[] text;

    /* --- Image, Media Data --- */
    @Expose
    private byte[] binaryBytes;

    /* --- Media Data --- */
    @Expose
    private String mimeType;

    static SecureData newTextData() {
        return new SecureData(TYPE_TEXT);
    }

    static SecureData newImageData(byte[] imageBytes) {
        SecureData data = new SecureData(TYPE_IMAGE);
        data.binaryBytes = imageBytes;
        return data;
    }
    static SecureData newMediaData(byte[] imageBytes, String mimeType) {
        SecureData data = new SecureData(TYPE_MEDIA);
        data.binaryBytes = imageBytes;
        data.mimeType = mimeType;
        return data;
    }

    private SecureData(int type) {
        this.type = type;
        ensureFields();
    }

    public void ensureFields() {
        if (type < 0 || type > 2) {
            type = TYPE_TEXT;
        }
        switch (type) {
            case TYPE_TEXT -> {
                if (text == null) text = new char[0];
            }
            case TYPE_IMAGE, TYPE_MEDIA -> {
                if (binaryBytes == null) binaryBytes = new byte[0];
                if (mimeType == null || mimeType.isBlank()) mimeType = "audio/mpeg"; // Fallback, but should theoretically never happen
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
    public byte[] binaryBytes() {
        Utilities.ensureAuthorizedAccess(DataView.class);
        return binaryBytes;
    }
    public String mimeType() {
        Utilities.ensureAuthorizedAccess(DataView.class);
        return mimeType;
    }
    public void zeroOut() {
        Utilities.ensureAuthorizedAccess(SecureData.class);

        if (text != null) Arrays.fill(text, '\0');
        if (binaryBytes != null) Arrays.fill(binaryBytes, (byte) 0);
        if (mimeType != null) mimeType = null; // Even tough, this is not a sensitive object, it can't hurt to encourage GC
    }
    public boolean requiresSave() {
        return type == TYPE_TEXT;
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
        @Expose
        private String fileExtension;
        private String newContent;

        private @Nullable SecureData tmpData;
        private byte[] tmpBytes;
        private @Nullable CharBuffer tmpCharBuffer;
        private @Nullable ByteBuffer tmpByteBuffer;

        public void ensureFields() {
            if (type < 0 || type > 2) {
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
            if (fileExtension == null) {
                fileExtension = "";
            }
        }

        private Encrypted(int type, String fileExtension) {
            this.type = type;
            this.fileExtension = fileExtension;
            ensureFields();
        }

        public static Encrypted newEncryptedTextData(String name) throws Exception {
            Encrypted encrypted = new Encrypted(TYPE_TEXT, "");
            encrypted.setName(name);
            encrypted.store(newTextData());
            return encrypted;
        }
        public static Encrypted newEncryptedImageData(String name, byte[] imageBytes, String fileExtension) throws Exception {
            Encrypted encrypted = new Encrypted(TYPE_IMAGE, fileExtension);
            encrypted.setName(name);
            encrypted.store(newImageData(imageBytes));
            return encrypted;
        }
        public static Encrypted newEncryptedMediaData(String name, byte[] videoBytes, String fileExtension) throws Exception {
            Encrypted encrypted = new Encrypted(TYPE_MEDIA, fileExtension);
            encrypted.setName(name);
            encrypted.store(newMediaData(videoBytes, Utilities.getSupportedMimeType(fileExtension)));
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

            tmpData = decrypt();
            String json = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(tmpData);
            newContent = CypherEncryption.encryptToString(json, key, salt);
            tmpData.zeroOut();
            tmpData = null;
        }

        public void zeroTmpData() {
            if (tmpData != null) {
                tmpData.zeroOut();
                tmpData = null;
            }
            if (tmpBytes != null) {
                Arrays.fill(tmpBytes, (byte) 0);
                tmpBytes = null;
            }
            if (tmpCharBuffer != null) {
                tmpCharBuffer.clear();
                while (tmpCharBuffer.hasRemaining()) {
                    tmpCharBuffer.put('\0');
                }
                tmpCharBuffer = null;
            }
            if (tmpByteBuffer != null) {
                tmpByteBuffer.clear();
                while (tmpByteBuffer.hasRemaining()) {
                    tmpByteBuffer.put((byte) 0);
                }
                tmpByteBuffer = null;
            }
        }

        public void finalizePasswordUpdate() {
            Utilities.ensureAuthorizedAccess(SecuritySetupController.class);

            if (newContent == null) {
                throw new SecurityException("finalizePasswordUpdate() incorrectly called out-of-context");
            }

            content = newContent;
            newContent = null;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public void export(File targetDestination) throws Exception {
            tmpData = decrypt();
            targetDestination.getParentFile().mkdirs();
            targetDestination.createNewFile();
            switch (type) {
                case TYPE_TEXT -> { // Do not convert to immutable java.lang.String but do workaround using char-buffer and byte-buffer
                    tmpCharBuffer = CharBuffer.wrap(tmpData.text);
                    tmpByteBuffer = StandardCharsets.UTF_8.encode(tmpCharBuffer);
                    tmpBytes = new byte[tmpByteBuffer.remaining()];
                    tmpByteBuffer.get(tmpBytes);
                }
                case TYPE_IMAGE, TYPE_MEDIA -> tmpBytes = tmpData.binaryBytes;
                default -> throw new Exception("Invalid data type");
            }
            Files.write(targetDestination.toPath(), tmpBytes);
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
                case TYPE_MEDIA -> "media";
                default -> "";
            } + ".png")));
        }

        public void cleanup() {
            Utilities.ensureAuthorizedAccess(Configuration.class);
        }
    }
}
