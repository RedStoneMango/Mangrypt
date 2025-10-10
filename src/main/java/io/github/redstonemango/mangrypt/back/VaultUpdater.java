package io.github.redstonemango.mangrypt.back;

/**
 * As of now, this class does not have a use case for there has not been another version than <code>1</code>.
 * <i>Luckily...</i>
 */
public class VaultUpdater {
    /**
     * As of now, this class does not have a use case for there has not been another version than <code>1</code>.
     * <i>Luckily...</i>
     */
    public static byte[] updateVault(byte[] oldSyntax, int oldVersion) {
        switch (oldVersion) {
            // Write update back here. The back should decrypt using the decryption back used in 'oldVersion' then encrypt the data with today's encryption back and finally return these updated bytes (without versioning appended; just the raw bytes)
            // Mind to also recursively update all data inside the vault, not just Layer 1
        }
        return new byte[0];
    }
}
