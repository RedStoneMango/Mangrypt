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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class MangryptV1Encryption {

    // Parameters for AES-GCM
    private static final int MASTER_KEY_LENGTH = 32; // 256 bits
    private static final int AES_GCM_SALT_LENGTH = 16;       // bytes
    private static final int AES_GCM_IV_LENGTH = 12;         // bytes for AES-GCM
    private static final int AES_GCM_TAG_LENGTH = 128;       // bits
    // Argon2id parameters for key hashing
    private static final int ARGON2_KEY_ITERATIONS = 3;
    private static final int ARGON2_KEY_MEMORY_KB = 65536;  // 64MB
    private static final int ARGON2_KEY_PARALLELISM = 2;

    // Argon2id parameters for hassword hashing
    private static final int ARGON2_PASSWORD_SALT_LENGTH = 16;  // 128-bit salt
    private static final int ARGON2_PASSWORD_HASH_LENGTH = 32;  // 256-bit hash
    private static final int ARGON2_PASSWORD_ITERATIONS = 3;
    private static final int ARGON2_PASSWORD_MEMORY = 65536; // 64MB
    private static final int ARGON2_PASSWORD_PARALLELISM = 1;

    public static final String DOMAIN_SEPARATOR = "mangrypt-vault-v1";

    public static byte[] generateRandomSalt() {
        byte[] salt = new byte[AES_GCM_SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static byte[] combinePasswords(char[] passwordA, char[] passwordB) throws Exception {
        byte[] bytesA = new byte[0];
        byte[] bytesB = new byte[0];
        byte[] hashA = new byte[0];
        byte[] hashB = new byte[0];
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            bytesA = charsToBytes(passwordA);
            bytesB = charsToBytes(passwordB);

            hashA = digest.digest(bytesA);
            hashB = digest.digest(bytesB);

            return concat(hashA, hashB);
        }
        finally {
            Arrays.fill(bytesA, (byte) 0);
            Arrays.fill(bytesB, (byte) 0);
            Arrays.fill(hashA, (byte) 0);
            Arrays.fill(hashB, (byte) 0);
        }
    }

    public static MasterData setupMasterKey(char[] passwordA, char[] passwordB) throws Exception {
        byte[] masterSalt = generateRandomSalt();
        SecretKey masterKey = deriveMasterKey(passwordA, passwordB, masterSalt);
        return new MasterData(masterKey, masterSalt);
    }

    private static SecretKey deriveMasterKey(char[] passwordA, char[] passwordB, byte[] masterSalt) throws Exception {
        byte[] combined = null;
        byte[] outputKey = new byte[MASTER_KEY_LENGTH];

        try {
            combined = combinePasswords(passwordA, passwordB);

            Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withSalt(masterSalt)
                    .withParallelism(ARGON2_KEY_PARALLELISM)
                    .withMemoryAsKB(ARGON2_KEY_MEMORY_KB)
                    .withIterations(ARGON2_KEY_ITERATIONS);

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(builder.build());
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

    // masterSalt (16) | perEncryptSalt (16) | IV (12) | ciphertext + tag
    public static byte[] encrypt(MasterData masterData, byte[] plaintext) throws Exception {
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

        // AAD: include salts and IV to prevent tampering
        cipher.updateAAD(concat(DOMAIN_SEPARATOR.getBytes(StandardCharsets.UTF_8), masterData.masterSalt, perEncryptSalt, iv));

        byte[] ciphertext = cipher.doFinal(plaintext);

        return concat(masterData.masterSalt, perEncryptSalt, iv, ciphertext);
    }

    public static BiResult<byte[], MasterData> decrypt(char[] passwordA, char[] passwordB, byte[] encrypted) throws Exception {
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

        cipher.updateAAD(concat(DOMAIN_SEPARATOR.getBytes(StandardCharsets.UTF_8), masterSalt, perEncryptSalt, iv)); // Must match AAD from encryption

        byte[] decrypted = cipher.doFinal(ciphertext);
        return new BiResult<>(decrypted, new MasterData(masterKey, masterSalt));
    }

    private static byte[] concat(byte[]... arrays) {
        int total = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] arr : arrays) {
            System.arraycopy(arr, 0, result, pos, arr.length);
            pos += arr.length;
        }
        return result;
    }

    private static byte[] charsToBytes(char[] chars) throws CharacterCodingException {
        ByteBuffer byteBuffer = null;
        try {
            CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
            byteBuffer = encoder.encode(CharBuffer.wrap(chars));
            return Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        }
        finally {
            if (byteBuffer != null && byteBuffer.hasArray()) {
                Arrays.fill(byteBuffer.array(), (byte) 0);
            }
        }
    }

    // $argon2id$<salt>$<hash>
    public static String hash(char[] password) throws CharacterCodingException {
        byte[] salt = new byte[ARGON2_PASSWORD_SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withParallelism(ARGON2_PASSWORD_PARALLELISM)
                .withMemoryAsKB(ARGON2_PASSWORD_MEMORY)
                .withIterations(ARGON2_PASSWORD_ITERATIONS);

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());

        byte[] hash = new byte[ARGON2_PASSWORD_HASH_LENGTH];
        byte[] passwordBytes = charsToBytes(password);
        generator.generateBytes(passwordBytes, hash);

        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        String encodedHash = Base64.getEncoder().encodeToString(hash);

        return String.format("$argon2id$%s$%s", encodedSalt, encodedHash);

    }

    // $argon2id$<salt>$<hash>
    public static boolean verifyHash(String hash, char[] password) {
        try {
            String[] parts = hash.split("\\$");
            if (parts.length != 4) return false;

            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

            Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withSalt(salt)
                    .withParallelism(ARGON2_PASSWORD_PARALLELISM)
                    .withMemoryAsKB(ARGON2_PASSWORD_MEMORY)
                    .withIterations(ARGON2_PASSWORD_ITERATIONS);

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(builder.build());

            byte[] passwordBytes = charsToBytes(password);
            byte[] computedHash = new byte[expectedHash.length];
            generator.generateBytes(passwordBytes, computedHash);

            return constantTimeArrayEquals(expectedHash, computedHash);
        } catch (Exception e) {
            return false;
        }
    }

    // Prevent timing attacks
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
