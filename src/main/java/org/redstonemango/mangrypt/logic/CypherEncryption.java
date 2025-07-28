package org.redstonemango.mangrypt.logic;

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
import java.util.Base64;

public class CypherEncryption {

    private static final int KEY_SIZE = 128; // bits
    private static final int IV_SIZE = 12; // 12 bytes is standard for GCM
    private static final int TAG_LENGTH = 128; // bits
    private static final int ITERATIONS = 65536;
    private static final int SALT_LENGTH = 16;

    public static String encryptToString(String input, String password) throws Exception {
        return Base64.getEncoder().encodeToString(encrypt(input, password));
    }

    public static byte[] encrypt(String input, String password) throws Exception {
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

    public static @Nullable String decryptFromString(String base64CypherText, String password) throws Exception {
        return decrypt(Base64.getDecoder().decode(base64CypherText), password);
    }

    public static @Nullable String decrypt(byte[] cypherBytes, String password) throws Exception {
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
        }
        catch (AEADBadTagException e) {
            return null;
        }
    }

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_SIZE);
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