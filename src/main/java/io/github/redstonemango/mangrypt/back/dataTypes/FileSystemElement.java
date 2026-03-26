package io.github.redstonemango.mangrypt.back.dataTypes;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import io.github.redstonemango.mangoutils.tuple.Tuple2;
import io.github.redstonemango.mangrypt.back.ConfigIO;
import io.github.redstonemango.mangrypt.back.Configuration;
import io.github.redstonemango.mangrypt.back.ContentAdder;
import io.github.redstonemango.mangrypt.back.Utilities;
import io.github.redstonemango.mangrypt.front.DataView;
import io.github.redstonemango.mangrypt.front.controller.FileSystemController;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public abstract class FileSystemElement implements Comparable<FileSystemElement> {

    @Tag(2) String name;
    @Tag(3) @Nullable String description;
    transient @Nullable FolderElement parent;

    abstract boolean exportTo(File file, boolean isRoot, Set<FileSystemElement> visited);
    public boolean exportTo(File file) {
        return exportTo(file, true, new HashSet<>());
    }

    public @Nullable String getDescription() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class);
        return description;
    }

    public void setDescription(@Nullable String description) {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, ContentAdder.class);
        this.description = description;
    }

    public String getName() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, DataView.class, FileSystemElement.class);
        return name;
    }

    public void setName(String name) {
        Utilities.ensureAuthorizedAccess(FileSystemController.class);
        this.name = name;
    }

    public @Nullable FolderElement getParent() {
        Utilities.ensureAuthorizedAccess(FileSystemController.class, FileSystemElement.class);
        return parent;
    }

    public String buildPath() {
        FolderElement root = ConfigIO.getConfig().getRootFolder();
        if (this == root) return "/";

        LinkedList<String> pathParts = new LinkedList<>();

        FileSystemElement current = this;
        while (current != null && current != root) {
            pathParts.addFirst(current.getName());
            current = current.getParent();
        }

        return "/" + String.join("/", pathParts);
    }

    public @Nullable Image runIconImageBuild() {
        return switch (this) {
            case FolderElement _ -> FolderElement.buildIconImage();
            case TextDataElement _ -> TextDataElement.buildIconImage();
            case ImageDataElement _ -> ImageDataElement.buildIconImage();
            case MediaDataElement _ -> MediaDataElement.buildIconImage();
            default -> null;
        };
    }

    public void ensureFields(String name, @Nullable FolderElement parent) {
        Utilities.ensureAuthorizedAccess(DataElement.class, FolderElement.class, SymlinkElement.class);
        this.name = name;
        if (description != null && description.isBlank()) {
            description = null; // If existing description is blank, trat it as unexisting
        }
        this.parent = parent;
    }

    public void updateParent(FolderElement parent) {
        Utilities.ensureAuthorizedAccess(FileSystemController.class);
        this.parent = parent;
    }

    public void zeroOut() {
        Utilities.ensureAuthorizedAccess(Configuration.class, FileSystemController.class);
        name = null;
        description = null;
    }

    public static @NotNull FileSystemElement fromPath(String path) {
        FolderElement root = ConfigIO.getConfig().getRootFolder();
        path = path.trim().replaceAll("/{2,}", "/");

        FolderElement current = root;

        String[] parts = path.split("/");
        for (String part : parts) {
            if (part.isBlank()) continue;

            FileSystemElement subElement = current.getContent().get(part);
            if (subElement != null && subElement.buildPath().equals(path)) return subElement;
            switch (subElement) {
                case null -> {
                    return current; // Fall back to the closest folder
                }
                case FolderElement folder -> {
                    current = folder;
                }
                case SymlinkElement ln when ln.resolveTargetElement() instanceof FolderElement folder -> {
                    current = folder;
                }
                default -> {
                    return subElement;
                }
            }
        }
        return current;
    }

    abstract FileSystemElement deepCopy(@Nullable FolderElement parent);

    public FileSystemElement deepCopy() {
        return deepCopy(parent);
    }

    @Override
    public int compareTo(@NotNull FileSystemElement other) {
        String thisName = name;
        String otherName = other.name;

        var thisNum = extractNumber(thisName);
        var otherNum = extractNumber(otherName);

        if (thisNum != null && otherNum != null
                && thisNum.getFirst() == otherNum.getFirst()) {
            return new BigInteger(thisNum.getSecond()).compareTo(new BigInteger(otherNum.getSecond()));
        }
        return thisName.compareTo(otherName);
    }

    /**
     * @return A tuple containing the extracted number as a string in the 2nd field
     * and an indicator where the number was found ({@code true} if the number is
     * the whole string or in the beginning, {@code false} if it is in the end)
     * or returns {@code null} if no number was found
     */
    public static @Nullable Tuple2<Boolean, String> extractNumber(String str) {
        int len = str.length();

        // Check start
        char first = str.charAt(0);
        if (first >= '0' && first <= '9') {
            int i = 0;
            while (i < len) {
                char c = str.charAt(i);
                if (c < '0' || c > '9') break;
                i++;
            }
            return new Tuple2<>(Boolean.TRUE, str.substring(0, i));
        }

        // Check end
        char last = str.charAt(len - 1);
        if (last >= '0' && last <= '9') {
            int i = len - 1;
            while (i >= 0) {
                char c = str.charAt(i);
                if (c < '0' || c > '9') break;
                i--;
            }
            return new Tuple2<>(Boolean.FALSE, str.substring(i + 1));
        }

        return null;
    }

}
