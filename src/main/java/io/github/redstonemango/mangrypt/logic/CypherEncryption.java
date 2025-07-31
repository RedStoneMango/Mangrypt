package io.github.redstonemango.mangrypt.logic;

import org.jetbrains.annotations.Nullable;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

public class CypherEncryption {

    private static final int KEY_SIZE = 128; // bits
    private static final int IV_SIZE = 12; // 12 bytes is standard for GCM
    private static final int TAG_LENGTH = 128; // bits
    private static final int ITERATIONS = 65536;
    private static final int SALT_LENGTH = 16;

    public static String encryptToString(String input, char[] password) throws Exception {
        return Base64.getEncoder().encodeToString(encrypt(input, password));
    }

    public static String encryptToString(String input, SecretKey key, byte[] salt) throws Exception {
        return Base64.getEncoder().encodeToString(encrypt(input, key, salt));
    }

    public static byte[] encrypt(String input, char[] password) throws Exception {
        byte[] salt = generateRandomBytes(SALT_LENGTH);
        byte[] iv = generateRandomBytes(IV_SIZE);

        SecretKey key = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] encryptedBytes = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));

        // Combine salt + IV + ciphertext
        byte[] combined = new byte[salt.length + iv.length + encryptedBytes.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, salt.length + iv.length, encryptedBytes.length);

        return combined;
    }

    public static byte[] encrypt(String input, SecretKey key, byte[] salt) throws Exception {
        byte[] iv = generateRandomBytes(IV_SIZE);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] encryptedBytes = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));

        // Combine salt + IV + ciphertext
        byte[] combined = new byte[salt.length + iv.length + encryptedBytes.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, salt.length + iv.length, encryptedBytes.length);

        return combined;
    }

    public static @Nullable String decryptFromString(String base64CypherText, char[] password) throws Exception {
        return decrypt(Base64.getDecoder().decode(base64CypherText), password);
    }

    public static @Nullable String decryptFromString(String base64CypherText, SecretKey key) throws Exception {
        return decrypt(Base64.getDecoder().decode(base64CypherText), key);
    }

    public static @Nullable String decrypt(byte[] cypherBytes, char[] password) throws Exception {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_SIZE];
            byte[] ciphertext = new byte[cypherBytes.length - SALT_LENGTH - IV_SIZE];

            System.arraycopy(cypherBytes, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(cypherBytes, SALT_LENGTH, iv, 0, IV_SIZE);
            System.arraycopy(cypherBytes, SALT_LENGTH + IV_SIZE, ciphertext, 0, ciphertext.length);

            SecretKey key = deriveKey(password, salt);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            return null;
        }
    }

    public static @Nullable String decrypt(byte[] cypherBytes, SecretKey key) throws Exception {
        try {
            byte[] iv = new byte[IV_SIZE];
            byte[] ciphertext = new byte[cypherBytes.length - IV_SIZE];

            System.arraycopy(cypherBytes, 0, iv, 0, IV_SIZE);
            System.arraycopy(cypherBytes, IV_SIZE, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            return null;
        }
    }

    public static byte[] generateRandomSalt() {
        return generateRandomBytes(SALT_LENGTH);
    }

    public static SecretKey extractAndDeriveKey(byte[] cypherBytes, char[] password) throws Exception {
        byte[] salt = Arrays.copyOfRange(cypherBytes, 0, SALT_LENGTH);
        return deriveKey(password, salt);
    }

    public static byte[] extractPayload(byte[] cypherBytes) {
        return Arrays.copyOfRange(cypherBytes, SALT_LENGTH, cypherBytes.length);
    }

    public static SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] generateRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }
}
