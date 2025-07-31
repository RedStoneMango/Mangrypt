package io.github.redstonemango.mangrypt.logic;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import javax.crypto.SecretKey;

public abstract class SecureData {

    public SecureData() {

    }

    public void ensureFields() {

    }

    public static class Encrypted {
        @Expose
        private Metadata metadata;
        @Expose
        private String content;

        public void ensureFields() {
            if (metadata == null) {
                metadata = new Metadata();
            }
            else {
                metadata.ensureFields();
            }
            if (content == null) {
                content = "";
            }
        }

        public SecureData decrypt() throws Exception {
            boolean trustedCaller = Configuration.WALKER.walk(frames ->
                    frames.skip(1).anyMatch(frame -> {
                        Class<?> caller = frame.getDeclaringClass();
                        return false;
                    })
            );
            if (!trustedCaller) {
                throw new SecurityException("Unauthorized (reflected?) access to decrypt()");
            }

            SecretKey key = ConfigIO.getConfig().password();
            String json = CypherEncryption.decryptFromString(content, key);
            SecureData data = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, SecureData.class);
            data.ensureFields();
            return data;
        }

        public void cleanup() {
            boolean trustedCaller = Configuration.WALKER.walk(frames ->
                    frames.skip(1).anyMatch(frame -> {
                        Class<?> caller = frame.getDeclaringClass();
                        return caller.equals(Configuration.class)
                                && caller.getClassLoader().equals(Configuration.class.getClassLoader());
                    })
            );
            if (!trustedCaller) {
                throw new SecurityException("Unauthorized (reflected?) access to cleanup()");
            }

            metadata = null;
        }
    }

    public static class Metadata {
        @Expose
        private String name;
        @Expose
        private String description;

        public void ensureFields() {
            if (name == null || name.isBlank()) {
                name = "UNNAMED DATA";
            }
            if (description == null) {
                description = "";
            }
        }
    }
}
