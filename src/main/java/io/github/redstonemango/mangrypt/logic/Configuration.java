package io.github.redstonemango.mangrypt.logic;

import com.google.gson.annotations.Expose;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.controller.AuthController;
import io.github.redstonemango.mangrypt.graphic.controller.SecuritySetupController;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

public class Configuration {

    static final StackWalker WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private SecretKey passphrase;
    private byte[] passphraseSalt;
    private SecretKey password;
    private byte[] passwordSalt;
    @Expose
    private String hash;
    @Expose
    private List<Folder> folders;

    public SecretKey passphrase() {
        boolean trustedCaller = WALKER.walk(frames ->
                frames.skip(1).anyMatch(frame -> {
                    Class<?> caller = frame.getDeclaringClass();
                    return caller.equals(ConfigIO.class)
                            && caller.getClassLoader().equals(ConfigIO.class.getClassLoader());
                })
        );
        if (!trustedCaller) {
            throw new SecurityException("Unauthorized (reflected?) access to passphrase()");
        }

        return passphrase;
    }
    public byte[] passphraseSalt() {
        return passphraseSalt;
    }
    public void updatePassphrase(char[] passphrase) throws Exception {
        boolean trustedCaller = WALKER.walk(frames ->
                frames.skip(1).anyMatch(frame -> {
                    Class<?> caller = frame.getDeclaringClass();
                    return (caller.equals(ConfigIO.class)
                            && caller.getClassLoader().equals(ConfigIO.class.getClassLoader()))
                            ||
                            (caller.equals(SecuritySetupController.class)
                                    && caller.getClassLoader().equals(SecuritySetupController.class.getClassLoader()));
                })
        );
        if (!trustedCaller) {
            throw new SecurityException("Unauthorized (reflected?) access to updatePassphrase(String)");
        }

        passphraseSalt = CypherEncryption.generateRandomSalt();
        this.passphrase = CypherEncryption.deriveKey(passphrase, passphraseSalt);
    }
    public void updatePassphrase(SecretKey key, byte[] salt) throws Exception {
        boolean trustedCaller = WALKER.walk(frames ->
                frames.skip(1).anyMatch(frame -> {
                    Class<?> caller = frame.getDeclaringClass();
                    return (caller.equals(ConfigIO.class)
                            && caller.getClassLoader().equals(ConfigIO.class.getClassLoader()))
                            ||
                            (caller.equals(SecuritySetupController.class)
                                    && caller.getClassLoader().equals(SecuritySetupController.class.getClassLoader()));
                })
        );
        if (!trustedCaller) {
            throw new SecurityException("Unauthorized (reflected?) access to updatePassphrase(String)");
        }

        passphraseSalt = salt;
        this.passphrase = key;
    }

    protected SecretKey password() {
        boolean trustedCaller = WALKER.walk(frames ->
                frames.skip(1).anyMatch(frame -> {
                    Class<?> caller = frame.getDeclaringClass();
                    return caller.equals(SecureData.class)
                            && caller.getClassLoader().equals(SecureData.class.getClassLoader());
                })
        );
        if (!trustedCaller) {
            throw new SecurityException("Unauthorized (reflected?) access to password()");
        }

        return password;
    }

    protected byte[] passwordSalt() {
        return passphraseSalt;
    }
    public void updatePassword(char[] password) throws Exception {
        boolean trustedCaller = WALKER.walk(frames ->
                frames.skip(1).anyMatch(frame -> {
                    Class<?> caller = frame.getDeclaringClass();
                    return (caller.equals(ConfigIO.class)
                            && caller.getClassLoader().equals(ConfigIO.class.getClassLoader()))
                            ||
                            (caller.equals(SecuritySetupController.class)
                                    && caller.getClassLoader().equals(SecuritySetupController.class.getClassLoader()));
                })
        );
        if (!trustedCaller) {
            throw new SecurityException("Unauthorized (reflected?) access to updatePassphrase(String)");
        }

        hash = Hasher.hash(password);
        passwordSalt = CypherEncryption.generateRandomSalt();
        this.password = CypherEncryption.deriveKey(password, passwordSalt);
    }
    public void updatePassword(SecretKey key, byte[] salt, char[] pwd) throws Exception {
        boolean trustedCaller = WALKER.walk(frames ->
                frames.skip(1).anyMatch(frame -> {
                    Class<?> caller = frame.getDeclaringClass();
                    return (caller.equals(ConfigIO.class)
                            && caller.getClassLoader().equals(ConfigIO.class.getClassLoader()))
                            ||
                            (caller.equals(SecuritySetupController.class)
                                    && caller.getClassLoader().equals(SecuritySetupController.class.getClassLoader()));
                })
        );
        if (!trustedCaller) {
            throw new SecurityException("Unauthorized (reflected?) access to updatePassphrase(String)");
        }

        hash = Hasher.hash(pwd);
        passwordSalt = salt;
        this.password = key;
    }

    public void cleanup() {
        boolean trustedCaller = WALKER.walk(frames ->
                frames.skip(1).anyMatch(frame -> {
                    Class<?> caller = frame.getDeclaringClass();
                    return caller.equals(Mangrypt.class)
                            && caller.getClassLoader().equals(Mangrypt.class.getClassLoader());
                })
        );
        if (!trustedCaller) {
            throw new SecurityException("Unauthorized (reflected?) access to cleanup()");
        }

        passphrase = null;
        password = null;
        if (folders != null) folders.forEach(Folder::cleanup);
        folders = null;

    }

    public boolean verifyPassword(char[] password) throws Exception {
        boolean trustedCaller = WALKER.walk(frames ->
                frames.skip(1).anyMatch(frame -> {
                    Class<?> caller = frame.getDeclaringClass();
                    return (caller.equals(AuthController.class)
                            && caller.getClassLoader().equals(AuthController.class.getClassLoader()));
                })
        );
        if (!trustedCaller) {
            throw new SecurityException("Unauthorized (reflected?) access to verifyPassword(char[])");
        }

        return Hasher.verifyHash(password, hash);
    }

    public List<Folder> getFolders() {
        return folders;
    }

    public void ensureFields() {
        if (hash == null) {
            hash = "";
        }
        if (folders == null) {
            folders = new ArrayList<>();
        }
        else {
            folders.forEach(Folder::ensureFields);
        }
    }

    public static class Folder {

        @Expose
        private String name;
        @Expose
        private String description;
        @Expose
        private List<SecureData.Encrypted> data;


        public List<SecureData.Encrypted> getEncryptedData() {
            return data;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void ensureFields() {
            if (name == null || name.isBlank()) {
                name = "UNNAMED VAULT";
            }
            if (description == null) {
                description = "";
            }
            if (data == null) {
                data = new ArrayList<>();
            }
            else {
                data.forEach(SecureData.Encrypted::ensureFields);
            }
        }

        public void cleanup() {
            boolean trustedCaller = WALKER.walk(frames ->
                    frames.skip(1).anyMatch(frame -> {
                        Class<?> caller = frame.getDeclaringClass();
                        return caller.equals(Configuration.class)
                                && caller.getClassLoader().equals(Configuration.class.getClassLoader());
                    })
            );
            if (!trustedCaller) {
                throw new SecurityException("Unauthorized (reflected?) access to cleanup()");
            }

            if (data != null) data.forEach(SecureData.Encrypted::cleanup);
            data = null;
        }
    }

}
