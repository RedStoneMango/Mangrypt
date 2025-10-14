package io.github.redstonemango.mangrypt.back.encryption;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.HKDFParameters;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public class MangryptV1Encryption {

    // AES-GCM Parameters
    private static final int MASTER_KEY_LENGTH = 32; // 256 bits
    private static final int AES_GCM_SALT_LENGTH = 16;
    private static final int AES_GCM_IV_LENGTH = 12;
    private static final int AES_GCM_TAG_LENGTH = 128;

    // Argon2 parameters for keys
    private static final int ARGON2_KEY_ITERATIONS = 3;
    private static final int ARGON2_KEY_MEMORY_KB = 65536;  // 64MB
    private static final int ARGON2_KEY_PARALLELISM = 2;

    // Argon2 parameters for password hashing
    private static final int ARGON2_PASSWORD_SALT_LENGTH = 16;
    private static final int ARGON2_PASSWORD_HASH_LENGTH = 32;
    private static final int ARGON2_PASSWORD_ITERATIONS = 3;
    private static final int ARGON2_PASSWORD_MEMORY = 65536; // 64MB
    private static final int ARGON2_PASSWORD_PARALLELISM = 1;

    public static final String DOMAIN_SEPARATOR = "mangrypt-vault-v1";

    public static byte[] generateRandomSalt() {
        byte[] salt = new byte[AES_GCM_SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static byte[] combinePasswords(char[] passwordA, char[] passwordB) throws IOException {
        byte[] bytesA = null;
        byte[] bytesB = null;
        try {
            bytesA = charsToBytes(passwordA);
            bytesB = charsToBytes(passwordB);
            return concat(bytesA, bytesB);
        } finally {
            if (bytesA != null) Arrays.fill(bytesA, (byte) 0);
            if (bytesB != null) Arrays.fill(bytesB, (byte) 0);
        }
    }

    public static MasterData setupMasterKey(char[] passwordA, char[] passwordB) throws Exception {
        Objects.requireNonNull(passwordA);
        Objects.requireNonNull(passwordB);

        byte[] masterSalt = generateRandomSalt();
        SecretKey masterKey = deriveMasterKey(passwordA, passwordB, masterSalt);
        return new MasterData(masterKey, masterSalt);
    }

    private static SecretKey deriveMasterKey(char[] passwordA, char[] passwordB, byte[] masterSalt) throws Exception {
        byte[] combined = null;
        byte[] outputKey = new byte[MASTER_KEY_LENGTH];
        try {
            combined = combinePasswords(passwordA, passwordB);
            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(argon2Params(masterSalt, true));
            generator.generateBytes(combined, outputKey);
            return new SecretKeySpec(Arrays.copyOf(outputKey, outputKey.length), "AES");
        } finally {
            if (combined != null) Arrays.fill(combined, (byte) 0);
            Arrays.fill(outputKey, (byte) 0);
        }
    }

    private static SecretKey deriveEncryptionKey(SecretKey masterKey, byte[] perEncryptSalt) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(masterKey.getEncoded(), perEncryptSalt, null));
        byte[] okm = new byte[MASTER_KEY_LENGTH];
        hkdf.generateBytes(okm, 0, okm.length);
        return new SecretKeySpec(okm, "AES");
    }

    public static byte[] encrypt(MasterData masterData, byte[] plaintext) throws Exception {
        Objects.requireNonNull(masterData);
        Objects.requireNonNull(plaintext);

        if (masterData.masterSalt.length != AES_GCM_SALT_LENGTH) {
            throw new IllegalArgumentException("Invalid master salt length");
        }

        byte[] perEncryptSalt = generateRandomSalt();
        SecretKey encryptionKey = deriveEncryptionKey(masterData.masterKey, perEncryptSalt);

        byte[] iv = new byte[AES_GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(AES_GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec);
        cipher.updateAAD(concat(DOMAIN_SEPARATOR.getBytes(StandardCharsets.UTF_8), masterData.masterSalt, perEncryptSalt, iv));

        byte[] encryptedOutput = new byte[plaintext.length + 16]; // 16 bytes for GCM tag
        int offset = 0;
        int chunkSize = 64 * 1024;

        for (int i = 0; i < plaintext.length; i += chunkSize) {
            int len = Math.min(chunkSize, plaintext.length - i);
            int outLen = cipher.update(plaintext, i, len, encryptedOutput, offset);
            offset += outLen;
        }
        int finalLen = cipher.doFinal(encryptedOutput, offset);

        byte[] ciphertext = Arrays.copyOf(encryptedOutput, offset + finalLen);

        return concat(masterData.masterSalt, perEncryptSalt, iv, ciphertext);
    }

    public static BiResult<byte[], MasterData> decrypt(char[] passwordA, char[] passwordB, byte[] encrypted) throws Exception {
        Objects.requireNonNull(passwordA);
        Objects.requireNonNull(passwordB);
        Objects.requireNonNull(encrypted);

        if (encrypted.length < AES_GCM_SALT_LENGTH * 2 + AES_GCM_IV_LENGTH + 1) {
            throw new IllegalArgumentException("Invalid encrypted data length");
        }

        byte[] masterSalt = Arrays.copyOfRange(encrypted, 0, AES_GCM_SALT_LENGTH);
        byte[] perEncryptSalt = Arrays.copyOfRange(encrypted, AES_GCM_SALT_LENGTH, AES_GCM_SALT_LENGTH * 2);
        byte[] iv = Arrays.copyOfRange(encrypted, AES_GCM_SALT_LENGTH * 2, AES_GCM_SALT_LENGTH * 2 + AES_GCM_IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(encrypted, AES_GCM_SALT_LENGTH * 2 + AES_GCM_IV_LENGTH, encrypted.length);

        SecretKey masterKey = deriveMasterKey(passwordA, passwordB, masterSalt);
        SecretKey encryptionKey = deriveEncryptionKey(masterKey, perEncryptSalt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(AES_GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec);
        cipher.updateAAD(concat(DOMAIN_SEPARATOR.getBytes(StandardCharsets.UTF_8), masterSalt, perEncryptSalt, iv));

        byte[] decryptedOutput = new byte[ciphertext.length]; // plaintext will be <= ciphertext
        int offset = 0;
        int chunkSize = 64 * 1024;

        for (int i = 0; i < ciphertext.length; i += chunkSize) {
            int len = Math.min(chunkSize, ciphertext.length - i);
            int outLen = cipher.update(ciphertext, i, len, decryptedOutput, offset);
            offset += outLen;
        }
        int finalLen = cipher.doFinal(decryptedOutput, offset);
        byte[] plaintext = Arrays.copyOf(decryptedOutput, offset + finalLen);

        return new BiResult<>(plaintext, new MasterData(masterKey, masterSalt));
    }

    private static Argon2Parameters argon2Params(byte[] salt, boolean forMasterKey) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt);

        if (forMasterKey) {
            builder.withParallelism(ARGON2_KEY_PARALLELISM)
                    .withMemoryAsKB(ARGON2_KEY_MEMORY_KB)
                    .withIterations(ARGON2_KEY_ITERATIONS);
        } else {
            builder.withParallelism(ARGON2_PASSWORD_PARALLELISM)
                    .withMemoryAsKB(ARGON2_PASSWORD_MEMORY)
                    .withIterations(ARGON2_PASSWORD_ITERATIONS);
        }

        return builder.build();
    }

    private static byte[] concat(byte[]... arrays) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (byte[] arr : arrays) {
                out.write(arr);
            }
            return out.toByteArray();
        }
    }

    private static byte[] charsToBytes(char[] chars) {
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0);
        return bytes;
    }

    public static String hash(char[] password) {
        Objects.requireNonNull(password);

        byte[] salt = new byte[ARGON2_PASSWORD_SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(argon2Params(salt, false));

        byte[] hash = new byte[ARGON2_PASSWORD_HASH_LENGTH];
        byte[] passwordBytes = charsToBytes(password);
        generator.generateBytes(passwordBytes, hash);

        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        String encodedHash = Base64.getEncoder().encodeToString(hash);

        return String.format("$argon2id$%s$%s", encodedSalt, encodedHash);
    }

    public static boolean verifyHash(String hash, char[] password) {
        Objects.requireNonNull(hash);
        Objects.requireNonNull(password);

        try {
            String[] parts = hash.split("\\$");
            if (parts.length != 4) return false;

            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(argon2Params(salt, false));

            byte[] passwordBytes = charsToBytes(password);
            byte[] computedHash = new byte[expectedHash.length];
            generator.generateBytes(passwordBytes, computedHash);

            return constantTimeArrayEquals(expectedHash, computedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean constantTimeArrayEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    public record MasterData(SecretKey masterKey, byte[] masterSalt) {}
    public record BiResult<T, U>(T valueA, U valueB) {}
}
