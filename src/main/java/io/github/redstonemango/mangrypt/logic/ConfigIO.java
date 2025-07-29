package io.github.redstonemango.mangrypt.logic;

import com.google.gson.GsonBuilder;
import io.github.redstonemango.mangrypt.Mangrypt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    private static Configuration.L1Enc layer1;

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
            layer1.encryptLayer2();
            String json = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(layer1);
            encrypted = CypherEncryption.encrypt(json, layer1.passphrase());
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

    public static boolean decryptLayerOne(String password) {
        if (!STORAGE_FILE.exists()) {
            try {
                STORAGE_FILE.getParentFile().mkdirs();
                STORAGE_FILE.createNewFile();
                // Run init logic
                return true;
            }
            catch (IOException e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error creating storage file", e);
            }
        }

        byte[] encrypted;
        try {
            encrypted = Files.readAllBytes(STORAGE_FILE.toPath());
        }
        catch (IOException e) {
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error reading storage file", e);
        }

        try {
            String json = CypherEncryption.decrypt(encrypted, password);
            if (json == null) {
                return false;
            }
            layer1 = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, Configuration.L1Enc.class);
            return true;
        }
        catch (Exception e) {
            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
            throw new RuntimeException("Error decrypting and processing layer 1 in the storage file", e);
        }
    }

    public static Configuration.L1Enc getLayer1() {
        if (layer1 == null) throw new UnsupportedOperationException("Layer 1 is not decrypted yet");
        return layer1;
    }

    public static boolean isLayer1Decrypted() {
        return layer1 != null;
    }

    public static File getStorageFile() {
        return STORAGE_FILE;
    }
}
