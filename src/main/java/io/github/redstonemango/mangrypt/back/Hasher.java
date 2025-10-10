package io.github.redstonemango.mangrypt.back;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Technically duplicate of {@link io.github.redstonemango.mangoutils.Hasher} but I like it more to exist here due to better control and more transparency.
 */
public class Hasher {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int SALT_LENGTH = 16; // 128-bit salt
    private static final int KEY_LENGTH = 256; // bits
    private static final int ITERATIONS = 60_000;

    public static String hash(char[] password) throws Exception {
        byte[] salt = generateSalt();
        byte[] hash = pbkdf2Hash(password, salt, ITERATIONS);

        // Combine salt + hash as Base64 for storage
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        String encodedHash = Base64.getEncoder().encodeToString(hash);

        // Format: salt:iterations:hash
        return encodedSalt + ":" + ITERATIONS + ":" + encodedHash;
    }

    private static byte[] pbkdf2Hash(char[] input, byte[] salt, int iterations) throws Exception {
        KeySpec spec = new PBEKeySpec(input, salt, iterations, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    public static boolean verifyHash(char[] input, String storedHash) throws Exception {
        String[] parts = storedHash.split(":");
        if (parts.length != 3) return false;

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        int iterations = Integer.parseInt(parts[1]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

        byte[] actualHash = pbkdf2Hash(input, salt, iterations);

        return constantTimeEquals(expectedHash, actualHash);
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
