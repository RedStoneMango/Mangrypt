package io.github.redstonemango.mangrypt.logic;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.controller.SecuritySetupController;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    private static final StackWalker WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static class L1Enc {

        @Expose(serialize = false, deserialize = false)
        private char[] passphrase;
        @Expose(serialize = false, deserialize = false)
        private L2Enc layer2;
        @Expose
        private String content;

        protected char[] passphrase() {
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

        public void updatePassphrase(char[] passphrase) {
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

            this.passphrase = passphrase;
        }

        public L2Enc getLayer2() {
            if (layer2 == null) throw new UnsupportedOperationException("Layer 2 is not decrypted yet");
            return layer2;
        }

        public void encryptLayer2() throws Exception {
            String json = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(layer2);
            content = CypherEncryption.encryptToString(json, layer2.password());
        }

        public boolean decryptLayer2(char[] password) {
            try {
                String json = CypherEncryption.decryptFromString(content, password);
                if (json == null) {
                    return false;
                }
                layer2 = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, Configuration.L2Enc.class);
                layer2.updatePassword(password);
                return true;
            } catch (Exception e) {
                Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                throw new RuntimeException("Error decrypting and processing layer 2 in the storage file", e);
            }
        }

        public boolean isLayer2Decrypted() {
            return layer2 != null;
        }

        public void ensureFields() {
            if (layer2 == null) {
                layer2 = new L2Enc();
            }
            layer2.ensureFields();
        }
    }

    public static class L2Enc {

        @Expose(serialize = false, deserialize = false)
        private char[] password;
        @Expose
        private String hash;
        @Expose
        private List<Folder> folders;

        private char[] password() {
            boolean trustedCaller = WALKER.walk(frames ->
                    frames.skip(1).anyMatch(frame -> {
                        Class<?> caller = frame.getDeclaringClass();
                        return caller.equals(Configuration.class)
                                && caller.getClassLoader().equals(Configuration.class.getClassLoader());
                    })
            );
            if (!trustedCaller) {
                throw new SecurityException("Unauthorized (reflected?) access to updatePassphrase(String)");
            }

            return password;
        }

        public boolean verifyPassword(char[] password) throws Exception {
            return Hasher.verifyHash(password, hash);
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
            this.password = password;
        }

        public List<Folder> getFolders() {
            return folders;
        }

        public void ensureFields() {
            // Do not check for the hash to exist: If it doesn't, the data can be regarded as corrupted.
            if (folders == null) {
                folders = new ArrayList<>();
            }
            else {
                folders.forEach(Folder::ensureFields);
            }
        }
    }

    public static class Folder {

        @Expose
        private String name;
        @Expose
        private String description;
        @Expose
        private List<SecureData> data;


        public List<SecureData> getData() {
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
                name = "UNNAMED FOLDER";
            }
            if (description == null) {
                description = "";
            }
            if (data == null) {
                data = new ArrayList<>();
            }
            else {
                data.forEach(SecureData::ensureFields);
            }
        }

    }

}
