package io.github.redstonemango.mangrypt.back.encryption;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.Configuration;
import io.github.redstonemango.mangrypt.back.SimplifiedKryo;
import io.github.redstonemango.mangrypt.back.dataTypes.*;
import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VersionedEncryptionHandler {

    public static final SimplifiedKryo v1_KRYO = new SimplifiedKryo(
            Configuration.class, SecretKey.class, byte[].class, String.class, FolderElement.class, Map.class,
            FileSystemElement.class, TextDataElement.class, ImageDataElement.class, MediaDataElement.class,
            HashMap.class);

    public static @Nullable Configuration decrypt(int version, byte[] encrypted, char[] passphrase, char[] password) throws Exception {
        switch (version) {
            case 1 -> {
                MangryptV1Encryption.BiResult<byte[], MangryptV1Encryption.MasterData> decryptRes =
                        MangryptV1Encryption.decrypt(passphrase, password, encrypted);
                byte[] decrypted = decryptRes.valueA();
                MangryptV1Encryption.MasterData master = decryptRes.valueB();

                Configuration config = v1_KRYO.deserialize(decrypted, Configuration.class);
                config.updateMasterKey(master.masterKey(), master.masterSalt());
                config.definePassword(hash(1, password));
                return config;
            }
            default -> {
                showVersionError(version);
                return null;
            }
        }
    }

    public static byte @Nullable [] encrypt(int version, Configuration config) throws Exception {
        switch (version) {
            case 1 -> {
                byte[] serialized = v1_KRYO.serialize(config);
                MangryptV1Encryption.MasterData master = new MangryptV1Encryption.MasterData(config.masterKey(), config.masterSalt());
                return MangryptV1Encryption.encrypt(master, serialized); // Encrypt and store to config file
            }
            default -> {
                showVersionError(version);
                return null;
            }
        }
    }

    public static void setPasswords(int version, char[] passphrase, char[] password, Configuration configuration) throws Exception {
        switch (version) {
            case 1 -> {
                MangryptV1Encryption.MasterData master = MangryptV1Encryption.setupMasterKey(passphrase, password);
                configuration.updateMasterKey(master.masterKey(), master.masterSalt());
            }
            default -> {
                showVersionError(version);
            }
        }
    }

    public static @Nullable String hash(int version, char[] chars) throws Exception {
        switch (version) {
            case 1 -> {
                return MangryptV1Encryption.hash(chars);
            }
            default -> {
                showVersionError(version);
                return null;
            }
        }
    }

    public static boolean verifyHash(int version, String hash, char[] chars) {
        switch (version) {
            case 1 -> {
                return MangryptV1Encryption.verifyHash(hash, chars);
            }
            default -> {
                showVersionError(version);
                return false;
            }
        }
    }


    private static void showVersionError(int version) {
        Mangrypt.getBase().showErrorAlert("The vault has an unsupported version: " + version);
        System.err.println("Vault with unsupported version: v" + version);
    }
}
