package io.github.redstonemango.mangrypt.back;

import io.github.redstonemango.mangoutils.OperatingSystem;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.encryption.VersionedEncryptionHandler;
import io.github.redstonemango.mangrypt.front.BaseView;
import io.github.redstonemango.mangrypt.front.controller.AuthenticationController;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import io.github.redstonemango.mangrypt.front.controller.SecuritySetupController;
import javafx.fxml.FXMLLoader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class ConfigIO {

    private static final File VAULT_DIRECTORY = OperatingSystem.loadCurrentOS().createAppConfigDir("mangrypt");
    public static final int VERSION = 1;

    private static File vaultFile;

    private static Configuration config;
    private static boolean shouldSave = false;

    public static void cleanup() {
        Utilities.ensureAuthorizedAccess(Mangrypt.class, SecuritySetupController.class, AuthenticationController.class, BaseView.class);

        if (config != null) config.cleanup();
        config = null;
        vaultFile = null;
        shouldSave = false;
    }

    public static boolean shouldSave() {
        return shouldSave;
    }
    public static boolean isVaultOpen() {
        return config != null && vaultFile != null;
    }
    public static void markShouldSave() {
        shouldSave = true;
    }

    public static synchronized boolean save() {
        if (vaultFile == null || config == null) return true; // Exit safely if there is no open vault

        Mangrypt.getBase().storeShowingData();

        if (!vaultFile.exists()) {
            try {
                Files.createDirectories(vaultFile.getParentFile().toPath());
                Files.createFile(vaultFile.toPath());
            }
            catch (IOException e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error creating vault", e);
            }
        }

        byte[] encrypted;
        try {
            encrypted = VersionedEncryptionHandler.encrypt(VERSION, config);
        } catch (Exception e) {
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error encrypting vault", e);
        }
        if (encrypted == null) {
            return false; // Version is unsupported. Error handling happens in VersionedEncryptionHandler
        }
        encrypted = addVersioning(encrypted, VERSION);

        try {
            Files.write(vaultFile.toPath(), encrypted);
        }
        catch (IOException e) {
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error writing to vault", e);
        }
        return true;
    }

    public static boolean decryptConfig(char[] passphrase, char[] password) throws Exception {
        if (!vaultFile.exists()) {
            Mangrypt.getBase().showErrorAlert("Vault file does not exist");
            return false;
        }

        byte[] encrypted = Files.readAllBytes(vaultFile.toPath());

        int[] versionWrapper = new int[1];
        encrypted = extractVersioning(encrypted, versionWrapper); // Move versioning from byte header to versionWrapper[0]

        config = VersionedEncryptionHandler.decrypt(versionWrapper[0], encrypted, passphrase, password);
        if (config == null) {
            return false; // Version is unsupported. Error handling happens in VersionedEncryptionHandler
        }
        config.ensureFields();
        return true;
    }

    public static void setPasswords(char[] passphrase, char[] password) throws Exception {
        Utilities.ensureAuthorizedAccess(SecuritySetupController.class);
        if (config == null) {
            throw new IllegalStateException("No configs exist to edit");
        }

        String hash = VersionedEncryptionHandler.hash(VERSION, password);
        if (hash == null) return;

        VersionedEncryptionHandler.setPasswords(VERSION, passphrase, password, config);
        config.definePassword(hash);
    }

    public static void authenticateUser() {
        if (vaultFile.exists()) {
            try {
                FXMLLoader loader = new FXMLLoader(ConfigIO.class.getResource("/io/github/redstonemango/mangrypt/fxml/authentication.fxml"));
                Mangrypt.getBase().setSecondLayerRoot(loader.load());
                return;
            }
            catch (IOException e) {
                throw new RuntimeException(e); // Let's make the compiler happy :-)
            }
        }

        config = new Configuration();
        config.ensureFields();

        try {
            FXMLLoader loader = new FXMLLoader(ConfigIO.class.getResource("/io/github/redstonemango/mangrypt/fxml/security-setup.fxml"));
            Mangrypt.getBase().setSecondLayerRoot(loader.load());
        }
        catch (IOException e) {
            throw new RuntimeException(e); // I love happy compilers
        }
    }

    public static byte[] addVersioning(byte[] unversionedBytes, int version) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + unversionedBytes.length);
        buffer.putInt(version);
        buffer.put(unversionedBytes);
        return buffer.array();
    }
    public static byte[] extractVersioning(byte[] versionedBytes, int[] versionWrapper) {
        if (versionedBytes.length < 4) {
            throw new IllegalArgumentException("'byte[] versionedBytes' too short to contain a version");
        }
        else if (versionWrapper.length != 1) {
            throw new IllegalArgumentException("'int[] versionWrapper' is meant to be a wrapper only. Found array of length " + versionWrapper.length + " instead");
        }

        ByteBuffer buffer = ByteBuffer.wrap(versionedBytes);
        int extractedVersion = buffer.getInt();
        versionWrapper[0] = extractedVersion;

        byte[] unversionedBytes = new byte[versionedBytes.length - 4];
        buffer.get(unversionedBytes);
        return unversionedBytes;
    }

    public static Configuration getConfig() {
        if (config == null) throw new UnsupportedOperationException("Configs are not decrypted yet");
        return config;
    }

    public static boolean isConfigDecrypted() {
        return config != null;
    }

    public static void useVault(File file) {
        if (vaultFile != null) {
            throw new IllegalStateException("A vault is already selected");
        }

        vaultFile = file;
    }
    public static boolean isVaultSelected() {
        return vaultFile != null;
    }
    public static File getVaultFile() {
        return vaultFile;
    }
    public static File getVaultDirectory() {
        return VAULT_DIRECTORY;
    }
}
