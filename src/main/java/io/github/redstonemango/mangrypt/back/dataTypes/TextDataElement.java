package io.github.redstonemango.mangrypt.back.dataTypes;

import javafx.scene.image.Image;

import java.util.Arrays;
import java.util.Objects;

public class TextDataElement extends DataElement {

    public static Image buildIconImage() {
        return new Image(Objects.requireNonNull(TextDataElement.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/data-type-icon/text.png")));
    }

    @Override
    public DataElement symlinkedVersion(String symlinkName) {
        TextDataElement element = new TextDataElement();
        element.name = symlinkName;
        element.bytesWrapper = bytesWrapper;
        return element;
    }

    @Override
    FileSystemElement deepCopy(FolderElement parent) {
        TextDataElement copy = new TextDataElement();
        copy.name = name;
        copy.description = description;
        copy.parent = parent;
        copy.fileExtension = fileExtension;
        copy.bytesWrapper = new byte[][]{Arrays.copyOf(bytesWrapper[0], bytesWrapper[0].length)};

        return copy;
    }
}
