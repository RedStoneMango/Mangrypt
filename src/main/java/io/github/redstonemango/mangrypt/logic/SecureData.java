package io.github.redstonemango.mangrypt.logic;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.graphic.controller.AuthController;
import io.github.redstonemango.mangrypt.graphic.controller.SecuritySetupController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class SecureData {

    public SecureData() {

    }

    public void ensureFields() {

    }

    public static class Encrypted {
        @Expose
        private Metadata metadata;
        @Expose
        private String content;
        private String newContent;

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
            byte[] payload = CypherEncryption.extractPayload(content.getBytes(StandardCharsets.UTF_8));
            String json = CypherEncryption.decrypt(payload, key);
            SecureData data = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, SecureData.class);
            data.ensureFields();
            return data;
        }

        public void updatePassword(SecretKey key, byte[] salt) throws Exception {
            boolean trustedCaller = Configuration.WALKER.walk(frames ->
                    frames.skip(1).anyMatch(frame -> {
                        Class<?> caller = frame.getDeclaringClass();
                        return caller.equals(SecuritySetupController.class)
                                && caller.getClassLoader().equals(SecuritySetupController.class.getClassLoader());
                    })
            );
            if (!trustedCaller) {
                throw new SecurityException("Unauthorized (reflected?) access to updatePassword(SecretKey, byte[])");
            }

            SecretKey oldKey = ConfigIO.getConfig().password();
            byte[] payload = CypherEncryption.extractPayload(content.getBytes(StandardCharsets.UTF_8));
            String json = CypherEncryption.decrypt(payload, oldKey);
            if (json == null) throw new SecurityException("Unable to update data password for the data could not be decrypted using the old password");
            newContent = CypherEncryption.encryptToString(json, key, salt);
        }

        public void finalizePasswordUpdate() {
            boolean trustedCaller = Configuration.WALKER.walk(frames ->
                    frames.skip(1).anyMatch(frame -> {
                        Class<?> caller = frame.getDeclaringClass();
                        return caller.equals(SecuritySetupController.class)
                                && caller.getClassLoader().equals(SecuritySetupController.class.getClassLoader());
                    })
            );
            if (!trustedCaller) {
                throw new SecurityException("Unauthorized (reflected?) access to finalizePasswordUpdate()");
            }
            else if (newContent == null) {
                throw new SecurityException("finalizePasswordUpdate() incorrectly called out-of-context");
            }

            content = newContent;
            newContent = null;
        }

        public void cleanup() {
            boolean trustedCaller = Configuration.WALKER.walk(frames ->
                    frames.skip(1).anyMatch(frame -> {
                        Class<?> caller = frame.getDeclaringClass();
                        return (caller.equals(Mangrypt.class)
                                && caller.getClassLoader().equals(Mangrypt.class.getClassLoader()))
                                ||
                                (caller.equals(SecuritySetupController.class)
                                        && caller.getClassLoader().equals(SecuritySetupController.class.getClassLoader()))
                                ||
                                (caller.equals(AuthController.class)
                                        && caller.getClassLoader().equals(AuthController.class.getClassLoader()));
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
