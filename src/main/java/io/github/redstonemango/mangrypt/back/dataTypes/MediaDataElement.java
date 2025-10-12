package io.github.redstonemango.mangrypt.back.dataTypes;

import io.github.redstonemango.mangrypt.back.Utilities;
import javafx.scene.image.Image;

import java.util.Objects;

public class MediaDataElement extends DataElement {

    public String mimeType() {
        Utilities.ensureAuthorizedAccess();
        return Utilities.getSupportedMimeType(fileExtension);
    }

    public static Image buildIconImage() {
        return new Image(Objects.requireNonNull(MediaDataElement.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/data-type-icon/media.png")));
    }
}
