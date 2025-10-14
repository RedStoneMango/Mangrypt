package io.github.redstonemango.mangrypt.back.encryption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.nio.charset.StandardCharsets;
import javax.crypto.AEADBadTagException;

import static org.junit.jupiter.api.Assertions.*;

public class MangryptV1EncryptionTest {

    static Stream<Arguments> passwordPairs() {
        return Stream.of(
                Arguments.of("passA", "passB"),
                Arguments.of("", "nonempty"),
                Arguments.of("nonempty", ""),
                Arguments.of("!@#$$%ˆ", "^&*()_+"),
                Arguments.of("1234567890", "0987654321"),
                Arguments.of("パスワードA", "パスワードB"),  // Unicode passwords
                Arguments.of("a", "b"), // Short
                Arguments.of("Very long password ".repeat(50).trim(), "Lorem ipsum dolor sit amet ".repeat(50).trim())
        );
    }

    static Stream<Arguments> passwordAndPlaintextCombinations() {
        return Stream.of(
                Arguments.of("passA", "passB", "Normal text"),
                Arguments.of("short", "longer", ""),
                Arguments.of("unicodeA", "unicodeB", "🔥🔒🔑 Emoji input 🔐🔥"),
                Arguments.of("alpha", "beta", "Very long input ".repeat(50).trim()),
                Arguments.of("", "nonempty", "Edge case test"),
                Arguments.of("123", "321", "0123456789"),
                Arguments.of("aaaaaa", "bbbbbb", "Simple low entropy input"),
                Arguments.of("null\u0000char", "test", "with\u0000null")
        );
    }


    @ParameterizedTest
    @MethodSource({"passwordAndPlaintextCombinations"})
    @DisplayName("Encrypt -> Decrypt should restore original data")
    void testEncryptDecrypt(String passwordAStr, String passwordBStr, String inputText) throws Exception {
        char[] passwordA = passwordAStr.toCharArray();
        char[] passwordB = passwordBStr.toCharArray();
        byte[] input = inputText.getBytes(StandardCharsets.UTF_8);

        MangryptV1Encryption.MasterData master = MangryptV1Encryption.setupMasterKey(passwordA, passwordB);
        byte[] encrypted = MangryptV1Encryption.encrypt(master, input);

        MangryptV1Encryption.BiResult<byte[], MangryptV1Encryption.MasterData> result =
                MangryptV1Encryption.decrypt(passwordA, passwordB, encrypted);

        assertArrayEquals(input, result.valueA(), "Decrypted data should match original input");
    }

    @ParameterizedTest
    @MethodSource("passwordPairs")
    @DisplayName("Encryption should be non-deterministic with AES-GCM")
    void testEncryptionIsNonDeterministic(String passwordAStr, String passwordBStr) throws Exception {
        char[] passwordA = passwordAStr.toCharArray();
        char[] passwordB = passwordBStr.toCharArray();
        byte[] data = "Fixed message".getBytes(StandardCharsets.UTF_8);

        MangryptV1Encryption.MasterData master = MangryptV1Encryption.setupMasterKey(passwordA, passwordB);
        byte[] encrypted1 = MangryptV1Encryption.encrypt(master, data);
        byte[] encrypted2 = MangryptV1Encryption.encrypt(master, data);

        assertFalse(Arrays.equals(encrypted1, encrypted2), "AES-GCM should produce different ciphertexts");
    }

    @Test
    @DisplayName("Decrypted data can be re-encrypted and re-decrypted accurately")
    void testReEncryptionRoundTrip() throws Exception {
        char[] passwordA = "A".toCharArray();
        char[] passwordB = "B".toCharArray();
        byte[] data = "Re-encryption test".getBytes(StandardCharsets.UTF_8);

        MangryptV1Encryption.MasterData master = MangryptV1Encryption.setupMasterKey(passwordA, passwordB);
        byte[] encrypted1 = MangryptV1Encryption.encrypt(master, data);
        byte[] decrypted1 = MangryptV1Encryption.decrypt(passwordA, passwordB, encrypted1).valueA();

        byte[] encrypted2 = MangryptV1Encryption.encrypt(master, decrypted1);
        byte[] decrypted2 = MangryptV1Encryption.decrypt(passwordA, passwordB, encrypted2).valueA();

        assertArrayEquals(data, decrypted2, "Re-encrypted data should match original after second decryption");
    }

    @Test
    @DisplayName("Binary data encrypt/decrypt roundtrip should succeed")
    void testBinaryDataRoundTrip() throws Exception {
        byte[] binary = new byte[512];
        new SecureRandom().nextBytes(binary);

        char[] passwordA = "binA".toCharArray();
        char[] passwordB = "binB".toCharArray();

        MangryptV1Encryption.MasterData master = MangryptV1Encryption.setupMasterKey(passwordA, passwordB);
        byte[] encrypted = MangryptV1Encryption.encrypt(master, binary);
        byte[] decrypted = MangryptV1Encryption.decrypt(passwordA, passwordB, encrypted).valueA();

        assertArrayEquals(binary, decrypted, "Binary data should decrypt correctly");
    }

    @Test
    @DisplayName("setupMasterKey should produce different keys with same input (randomized salt)")
    void testMasterKeyRandomness() throws Exception {
        char[] passwordA = "passA".toCharArray();
        char[] passwordB = "passB".toCharArray();

        MangryptV1Encryption.MasterData m1 = MangryptV1Encryption.setupMasterKey(passwordA, passwordB);
        MangryptV1Encryption.MasterData m2 = MangryptV1Encryption.setupMasterKey(passwordA, passwordB);

        assertFalse(Arrays.equals(m1.masterKey().getEncoded(), m2.masterKey().getEncoded()), "Master keys should differ due to randomized salt");
    }

    @Test
    @DisplayName("Encryption/decryption should be thread-safe")
    void testThreadSafety() throws Exception {
        char[] passA = "pA".toCharArray();
        char[] passB = "pB".toCharArray();
        byte[] input = "Concurrent Data".getBytes(StandardCharsets.UTF_8);

        MangryptV1Encryption.MasterData master = MangryptV1Encryption.setupMasterKey(passA, passB);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            results.add(executor.submit(() -> {
                byte[] encrypted = MangryptV1Encryption.encrypt(master, input);
                byte[] decrypted = MangryptV1Encryption.decrypt(passA, passB, encrypted).valueA();
                return Arrays.equals(input, decrypted);
            }));
        }

        for (Future<Boolean> result : results) {
            assertTrue(result.get(), "Threaded encryption/decryption should be correct");
        }

        executor.shutdown();
    }


    @ParameterizedTest
    @ValueSource(strings = {"password", "p@ssw0rd!", "123456", "", "パスワード"})
    @DisplayName("Hashed password should be verified correctly")
    void testHashAndVerifyPassword(String passwordStr) {
        char[] password = passwordStr.toCharArray();

        String hash = MangryptV1Encryption.hash(password);
        boolean verified = MangryptV1Encryption.verifyHash(hash, password);

        assertTrue(verified, "Password should verify correctly against hash");
    }

    @Test
    @DisplayName("Slightly different passwords should produce different encryption keys")
    void testSimilarPasswordsDoNotCollide() throws Exception {
        byte[] input = "Sensitive Info".getBytes(StandardCharsets.UTF_8);

        char[] passwordA1 = "pass123".toCharArray();
        char[] passwordB1 = "passABC".toCharArray();
        char[] passwordA2 = "pass123".toCharArray();
        char[] passwordB2 = "passABD".toCharArray(); // Off by 1

        MangryptV1Encryption.MasterData master = MangryptV1Encryption.setupMasterKey(passwordA1, passwordB1);
        byte[] encrypted = MangryptV1Encryption.encrypt(master, input);

        assertThrows(AEADBadTagException.class, () -> {
            MangryptV1Encryption.decrypt(passwordA2, passwordB2, encrypted);
        }, "Slightly different passwords should not produce matching keys");
    }

    @ParameterizedTest
    @CsvSource({
            "password1,password2,passwordX,password2",
            "hello123,world456,hello123,wrong456",
            "a,b,c,d"
    })
    @DisplayName("Decryption should fail with wrong passwords")
    void testIncorrectPasswordFailsDecryption(
            String passA, String passB, String wrongA, String wrongB) throws Exception {

        byte[] plaintext = "SecretData".getBytes(StandardCharsets.UTF_8);

        MangryptV1Encryption.MasterData master = MangryptV1Encryption.setupMasterKey(passA.toCharArray(), passB.toCharArray());
        byte[] encrypted = MangryptV1Encryption.encrypt(master, plaintext);

        assertThrows(AEADBadTagException.class, () -> {
            MangryptV1Encryption.decrypt(wrongA.toCharArray(), wrongB.toCharArray(), encrypted);
        }, "Expected AEAD failure with wrong passwords");
    }

    @ParameterizedTest
    @MethodSource("passwordPairs")
    @DisplayName("Tampering any part of the ciphertext should fail decryption (IV, body, tag)")
    void testTamperingAllPartsFailsDecryption(String passwordAStr, String passwordBStr) throws Exception {
        char[] passwordA = passwordAStr.toCharArray();
        char[] passwordB = passwordBStr.toCharArray();
        byte[] data = "Sensitive payload to encrypt".getBytes(StandardCharsets.UTF_8);

        MangryptV1Encryption.MasterData master = MangryptV1Encryption.setupMasterKey(passwordA, passwordB);
        byte[] encrypted = MangryptV1Encryption.encrypt(master, data);

        for (int i = 0; i < encrypted.length; i += Math.max(1, encrypted.length / 3)) {
            byte[] tampered = Arrays.copyOf(encrypted, encrypted.length);
            tampered[i] ^= 0x01;

            assertThrows(AEADBadTagException.class, () -> {
                MangryptV1Encryption.decrypt(passwordA, passwordB, tampered);
            }, "Tampering at byte " + i + " should break authentication");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"somePassword", "otherPassword", "パスワード"})
    @DisplayName("Same password hash should be salted and unique")
    void testHashIsSalted(String passwordStr) {
        char[] password = passwordStr.toCharArray();

        String hash1 = MangryptV1Encryption.hash(password);
        String hash2 = MangryptV1Encryption.hash(password);

        assertNotEquals(hash1, hash2, "Hashes of the same password should differ (salted)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"test123", "anotherPassword"})
    @DisplayName("Invalid password should not match hash")
    void testInvalidHashReturnsFalse(String input) {
        char[] passwordA = (input + "_A").toCharArray();
        char[] passwordB = (input + "_B").toCharArray();

        String hash = MangryptV1Encryption.hash(passwordB);
        boolean result = MangryptV1Encryption.verifyHash(hash, passwordA);

        assertFalse(result, "Invalid hash verification should return false");
    }

    @Test
    @DisplayName("Modified hash string should not verify")
    void testModifiedHashFailsVerification() {
        char[] password = "secure123".toCharArray();
        String hash = MangryptV1Encryption.hash(password);

        String tampered = hash.substring(0, hash.length() - 1) + "X"; // Change last char
        assertFalse(MangryptV1Encryption.verifyHash(tampered, password), "Tampered hash should not verify");
    }

    @ParameterizedTest
    @ValueSource(strings = {"data1", "anotherData", ""})
    @DisplayName("Password order matters for decryption")
    void testPasswordOrderMatters(String input) throws Exception {
        char[] passwordA = "alpha".toCharArray();
        char[] passwordB = "beta".toCharArray();
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        MangryptV1Encryption.MasterData master = MangryptV1Encryption.setupMasterKey(passwordA, passwordB);
        byte[] encrypted = MangryptV1Encryption.encrypt(master, data);

        // Swap passwords
        assertThrows(AEADBadTagException.class, () -> {
            MangryptV1Encryption.decrypt(passwordB, passwordA, encrypted);
        }, "Decryption should fail if passwords are swapped");
    }

    @Test
    @DisplayName("Invalid master-salt length should throw IllegalArgumentException")
    void testEncryptionSaltLengthThrowsException() throws Exception {
        char[] passwordA = "alpha".toCharArray();
        char[] passwordB = "beta".toCharArray();
        byte[] data = "Sensitive".getBytes(StandardCharsets.UTF_8);

        MangryptV1Encryption.MasterData master = MangryptV1Encryption.setupMasterKey(passwordA, passwordB);
        MangryptV1Encryption.MasterData invalidMaster = new MangryptV1Encryption.MasterData(master.masterKey(),
                new byte[master.masterSalt().length - 2]);
        System.arraycopy(master.masterSalt(), 0, invalidMaster.masterSalt(), 0,
                invalidMaster.masterSalt().length); // Short master-salt length

        assertThrows(IllegalArgumentException.class, () -> {
            MangryptV1Encryption.encrypt(invalidMaster, data);
        }, "Encryption should throw IllegalArgumentException if master-salt length is invalid");
    }

    @Test
    @DisplayName("Invalid ciphertext length should throw IllegalArgumentException")
    void testCiphertextLengthThrowsException() {
        char[] passwordA = "alpha".toCharArray();
        char[] passwordB = "beta".toCharArray();
        byte[] data = new byte[6]; // Too short

        assertThrows(IllegalArgumentException.class, () -> {
            MangryptV1Encryption.decrypt(passwordA, passwordB, data);
        }, "Decryption should throw IllegalArgumentException if ciphertext length is invalid");
    }

    @Test
    @DisplayName("Null input for encryption should throw NullPointerException")
    void testEncryptionNullInputThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            MangryptV1Encryption.encrypt(null, new byte[0]);
        }, "Null input for encryption should throw NullPointerException");
    }

    @Test
    @DisplayName("Null input for decryption should throw NullPointerException")
    void testDecryptionKeyNullInputThrowsException() {
        char[] passwordA = "pA".toCharArray();
        char[] passwordB = "pB".toCharArray();

        assertThrows(NullPointerException.class, () -> {
            MangryptV1Encryption.decrypt(passwordA, passwordB, null);
        }, "Null input for decryption should throw NullPointerException");
    }

    @Test
    @DisplayName("Null input for setupMasterKey should throw NullPointerException")
    void testMasterKeyNullInputThrowsException() {
        char[] passwordA = "pA".toCharArray();

        assertThrows(NullPointerException.class, () -> {
            MangryptV1Encryption.setupMasterKey(passwordA, null);
        }, "Null input for setupMasterKey should throw NullPointerException");
    }

    @Test
    @DisplayName("Null input for hashing should throw NullPointerException")
    void testHashNullInputThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            MangryptV1Encryption.hash(null);
        }, "Null input for hashing should throw NullPointerException");
    }

    @Test
    @DisplayName("Null input for hash verification should throw NullPointerException")
    void testHashVerifyNullInputThrowsException() {
        char[] passwordA = "pA".toCharArray();

        assertThrows(NullPointerException.class, () -> {
            MangryptV1Encryption.verifyHash(null, passwordA);
        }, "Null input for hash verification should throw NullPointerException");
    }
}
