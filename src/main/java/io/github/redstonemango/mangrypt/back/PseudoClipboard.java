package io.github.redstonemango.mangrypt.back;

import io.github.redstonemango.mangrypt.back.dataTypes.FileSystemElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PseudoClipboard {
    private List<FileSystemElement> content = new ArrayList<>();

    public void copy(List<FileSystemElement> objects) {
        List<FileSystemElement> copy = new ArrayList<>();
        objects.forEach(element -> copy.add(element.deepCopy()));

        content = copy;
    }

    public Optional<List<FileSystemElement>> paste() {
        List<FileSystemElement> copy = new ArrayList<>();
        content.forEach(element -> copy.add(element.deepCopy())); // Deep-copy again to prevent same pointers

        if (copy.isEmpty()) return Optional.empty();
        return Optional.of(copy);
    }

    public void clear() {
        content = null;
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }
}