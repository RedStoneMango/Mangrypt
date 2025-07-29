package io.github.redstonemango.mangrypt.logic;

import com.google.gson.annotations.Expose;

public abstract class SecureData {

    @Expose
    private String name;
    @Expose
    private String description;

    public SecureData(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void ensureFields() {
        if (name == null || name.isBlank()) {
            name = "UNNAMED CONTENT";
        }
        if (description == null) {
            description = "";
        }
    }

    public static class SecureTextData extends SecureData {

        @Expose
        private String text;

        public SecureTextData(String name, String description, String text) {
            super(name, description);
            this.text = text;
        }

        @Override
        public void ensureFields() {
            super.ensureFields();

            if (text == null) {
                text = "";
            }
        }
    }

}
