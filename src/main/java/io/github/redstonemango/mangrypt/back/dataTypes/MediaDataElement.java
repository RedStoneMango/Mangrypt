package io.github.redstonemango.mangrypt.back.dataTypes;

import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.DataView;
import javafx.scene.image.Image;

import java.util.Arrays;
import java.util.Objects;

public class MediaDataElement extends DataElement {

    public String mimeType() {
        Utilities.ensureAuthorizedAccess(DataView.class);
        return Utilities.getSupportedMediaMimeType(fileExtension);
    }

    @Override
    public DataElement symlinkedVersion(String symlinkName) {
        MediaDataElement element = new MediaDataElement();
        element.name = symlinkName;
        element.bytesWrapper = bytesWrapper;
        return element;
    }

    public static Image buildIconImage() {
        return new Image(Objects.requireNonNull(MediaDataElement.class.getResourceAsStream("/io/github/redstonemango/mangrypt/image/data-type-icon/media.png")));
    }

    @Override
    FileSystemElement deepCopy(FolderElement parent) {
        MediaDataElement copy = new MediaDataElement();
        copy.name = name;
        copy.description = description;
        copy.parent = parent;
        copy.fileExtension = fileExtension;
        copy.bytesWrapper = new byte[][]{Arrays.copyOf(bytesWrapper[0], bytesWrapper[0].length)};

        return copy;
    }
}
