package io.github.redstonemango.mangrypt.logic;

import com.google.gson.GsonBuilder;
import io.github.redstonemango.mangrypt.Mangrypt;
import javafx.fxml.FXMLLoader;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

public class ConfigIO {

    private static final File STORAGE_FILE;

    static {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String userHome = System.getProperty("user.home");

        if (os.contains("win")) {
            STORAGE_FILE = new File(userHome, "AppData/Local/mangrypt/storage.mgrt");
        } else if (os.contains("mac")) {
            STORAGE_FILE = new File(userHome, "Library/Application Support/mangrypt/storage.mgrt");
        } else {
            // Assume Linux or other Unix-like system
            String configHome = System.getenv("XDG_CONFIG_HOME");
            if (configHome == null || configHome.isEmpty()) {
                configHome = userHome + "/.config";
            }
            STORAGE_FILE = new File(new File(configHome, "mangrypt"), "storage.mgrt");
        }
    }

    private static Configuration config;
    private static boolean shouldSave = false;

    public static void cleanup() {
        if (config != null) config.cleanup();
        config = null;
    }

    public static boolean shouldSave() {
        return shouldSave;
    }
    public static void markShouldSave() {
        shouldSave = true;
    }

    public static void save() {
        if (!STORAGE_FILE.exists()) {
            try {
                STORAGE_FILE.getParentFile().mkdirs();
                STORAGE_FILE.createNewFile();
            }
            catch (IOException e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error creating storage file", e);
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

        try {
            Files.write(STORAGE_FILE.toPath(), encrypted);
        }
        catch (IOException e) {
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error writing to storage file", e);
        }
    }

    public static boolean decryptConfig(char[] passphrase) throws Exception {
        if (!STORAGE_FILE.exists()) {
            Mangrypt.getBase().showErrorAlert("Storage file does not exist");
            throw new RuntimeException("Storage file does not exist");
        }

        byte[] encrypted = Files.readAllBytes(STORAGE_FILE.toPath());

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
        if (STORAGE_FILE.exists()) {
            try {
                FXMLLoader loader = new FXMLLoader(ConfigIO.class.getResource("/io/github/redstonemango/mangrypt/fxml/passphrase-input.fxml"));
                Mangrypt.getBase().setSceneRoot(loader.load());
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
            Mangrypt.getBase().setSceneRoot(loader.load());
        }
        catch (IOException e) {
            throw new RuntimeException(e); // I love happy compilers
        }
    }

    public static Configuration getConfig() {
        if (config == null) throw new UnsupportedOperationException("Configs is not decrypted yet");
        return config;
    }

    public static boolean isConfigDecrypted() {
        return config != null;
    }

    public static File getStorageFile() {
        return STORAGE_FILE;
    }
}
