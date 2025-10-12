package io.github.redstonemango.mangrypt.back.dataTypes;

import javafx.scene.image.Image;

import java.util.Objects;

public class ImageDataElement extends DataElement {

    public static Image buildIconImage() {
        return new Image(Objects.requireNonNull(ImageDataElement.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/data-type-icon/image.png")));
    }
}
