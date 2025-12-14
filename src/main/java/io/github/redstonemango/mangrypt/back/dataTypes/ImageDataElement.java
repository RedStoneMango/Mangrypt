package io.github.redstonemango.mangrypt.back.dataTypes;

import javafx.scene.image.Image;

import java.util.Arrays;
import java.util.Objects;

public class ImageDataElement extends DataElement {

    public static Image buildIconImage() {
        return new Image(Objects.requireNonNull(ImageDataElement.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/data-type-icon/image.png")));
    }

    @Override
    public DataElement symlinkedVersion(String symlinkName) {
        ImageDataElement element = new ImageDataElement();
        element.name = symlinkName;
        element.bytesWrapper = bytesWrapper;
        return element;
    }

    @Override
    FileSystemElement deepCopy(FolderElement parent) {
        ImageDataElement copy = new ImageDataElement();
        copy.name = this.name;
        copy.description = this.description;
        copy.parent = parent;
        copy.fileExtension = fileExtension;
        copy.bytesWrapper = new byte[][]{Arrays.copyOf(bytesWrapper[0], bytesWrapper[0].length)};

        return copy;
    }
}
