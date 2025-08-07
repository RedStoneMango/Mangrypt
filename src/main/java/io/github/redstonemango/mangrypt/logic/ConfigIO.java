package io.github.redstonemango.mangrypt.logic;

import com.google.gson.GsonBuilder;
import io.github.redstonemango.mangoutils.OperatingSystem;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.controller.AuthController;
import io.github.redstonemango.mangrypt.graphic.controller.FolderOverviewController;
import io.github.redstonemango.mangrypt.graphic.controller.SecuritySetupController;
import javafx.fxml.FXMLLoader;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Locale;

public class ConfigIO {

    private static final File VAULT_DIRECTORY = OperatingSystem.loadCurrentOS().createAppConfigDir("mangrypt");
    private static final int VERSION = 1;

    private static File vaultFile;

    private static Configuration config;
    private static boolean shouldSave = false;

    public static void cleanup() {
        Utilities.ensureAuthorizedAccess(Mangrypt.class, SecuritySetupController.class, AuthController.class, SharedLogicManager.class);

        if (config != null) config.cleanup();
        config = null;
        vaultFile = null;
        shouldSave = false;
    }

    public static boolean shouldSave() {
        return shouldSave;
    }
    public static void markShouldSave() {
        shouldSave = true;
    }

    public static void save() {
        if (!vaultFile.exists()) {
            try {
                vaultFile.getParentFile().mkdirs();
                vaultFile.createNewFile();
            }
            catch (IOException e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error creating vault file", e);
            }
        }

        byte[] encrypted;
        try {
            String json = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(config);
            encrypted = CypherEncryption.encrypt(json, config.passphrase(), config.passphraseSalt());
        } catch (Exception e) {
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException(e);
        }
        encrypted = addVersioning(encrypted, VERSION);

        try {
            Files.write(vaultFile.toPath(), encrypted);
        }
        catch (IOException e) {
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error writing to vault file", e);
        }
    }

    public static boolean decryptConfig(char[] passphrase) throws Exception {
        if (!vaultFile.exists()) {
            Mangrypt.getBase().showErrorAlert("Vault file does not exist");
            throw new RuntimeException("Vault file does not exist");
        }

        byte[] encrypted = Files.readAllBytes(vaultFile.toPath());

        int[] versionWrapper = new int[1];
        encrypted = extractVersioning(encrypted, versionWrapper);
        if (versionWrapper[0] != VERSION) {
            encrypted = VaultUpdater.updateVault(encrypted, versionWrapper[0]); // Update if the vault has an outdated version
        }

        SecretKey key = CypherEncryption.extractAndDeriveKey(encrypted, passphrase);
        byte[] payload = CypherEncryption.extractPayload(encrypted);
        String json = CypherEncryption.decrypt(payload, key);
        if (json == null) {
            return false;
        }
        config = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, Configuration.class);
        config.ensureFields();
        config.updatePassphrase(passphrase);
        return true;
    }

    public static void authenticateUserAndLoadConfig() {
        if (vaultFile.exists()) {
            try {
                FXMLLoader loader = new FXMLLoader(ConfigIO.class.getResource("/io/github/redstonemango/mangrypt/fxml/passphrase-input.fxml"));
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
