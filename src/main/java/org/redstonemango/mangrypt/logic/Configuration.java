package org.redstonemango.mangrypt.logic;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.redstonemango.mangrypt.Mangrypt;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class Configuration {

    public static class L1Enc {

        @Expose(serialize = false, deserialize = false)
        private String plainPassphrase;
        @Expose(serialize = false, deserialize = false)
        private L2Enc layer2;
        @Expose
        private String content;

        protected String passphrase() {
            return plainPassphrase;
        }

        public void updatePassphrase(String passphrase) {
            plainPassphrase = passphrase;
        }

        public L2Enc getLayer2() {
            if (layer2 == null) throw new UnsupportedOperationException("Layer 2 is not decrypted yet");
            return layer2;
        }

        public void encryptLayer2() throws Exception {
            String json = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(layer2);
            content = CypherEncryption.encryptToString(json, layer2.password());
        }

        public boolean decryptLayer2(String password) {
            try {
                String json = CypherEncryption.decryptFromString(content, password);
                if (json == null) {
                    return false;
                }
                layer2 = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, Configuration.L2Enc.class);
                return true;
            } catch (Exception e) {
                Mangrypt.getBase().showErrorAlert("Decrypt and process storage file (Layer 2)", String.valueOf(e));
                throw new RuntimeException("Error decrypting and processing layer 2 in the storage file", e);
            }
        }

        public boolean isLayer2Decrypted() {
            return layer2 != null;
        }
    }

    public static class L2Enc {

        @Expose(serialize = false, deserialize = false)
        private String plainPassword;
        @Expose
        private String passwordHash;
        @Expose
        private List<Folder> folders;

        protected String password() {
            return plainPassword;
        }

        public boolean verifyPassword(String password) throws Exception {
            return Hasher.verifyHash(passwordHash, password);
        }

        public void updatePassword(String password) throws Exception {
            passwordHash = Hasher.hashString(password);
            plainPassword = password;
        }

        public List<Folder> getFolders() {
            return folders;
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

    }

}
