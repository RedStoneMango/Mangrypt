package io.github.redstonemango.mangrypt.back;

import io.github.redstonemango.mangoutils.tuple.Tuple3;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.dataTypes.*;
import io.github.redstonemango.mangrypt.front.IOLoaderNode;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.util.Set;
import java.util.function.Consumer;

public class ContentAdder {

    public static void addFolder(FolderElement parentFolder, Consumer<FileSystemElement> callback, Set<String> existingNames) {
        nameInputDialog(name -> {
            FolderElement newFolder = new FolderElement();
            newFolder.ensureFields(name, parentFolder);
            parentFolder.getContent().put(name, newFolder);
            callback.accept(newFolder);
            ConfigIO.markShouldSave();
        }, existingNames);
    }

    public static void addTextElement(FolderElement parentFolder, Consumer<FileSystemElement> callback, Set<String> existingNames) {
        nameInputDialog(name -> {
            TextDataElement element = new TextDataElement();
            element.ensureFields(name, parentFolder);
            element.fileExtension(".txt");
            parentFolder.getContent().put(name, element);
            callback.accept(element);
            ConfigIO.markShouldSave();
        }, existingNames);
    }

    public static void addImageElement(FolderElement parentFolder, Consumer<FileSystemElement> callback, Set<String> existingNames) {
        nameInputDialog(name -> {
            File userHome = new File(System.getProperty("user.home"));
            Tuple3<IOLoaderNode, StackPane, StackPane> assets = IOLoaderNode.open(
                    "Select image file",
                    userHome,
                    data -> {
                        Mangrypt.getBase().setSecondLayerRoot(null);

                        ImageDataElement element = new ImageDataElement();
                        element.bytes(data.getFirst());
                        element.fileExtension(data.getSecond());
                        element.ensureFields(name, parentFolder);
                        parentFolder.getContent().put(name, element);
                        callback.accept(element);
                        ConfigIO.markShouldSave();
                    },
                    ex -> {
                        Mangrypt.getBase().showErrorAlert(String.valueOf(ex));
                        throw new RuntimeException("Error accessing image", ex);
                    },
                    () -> Mangrypt.getBase().setSecondLayerRoot(null),
                    "jpeg", "jpg", "png", "bmp", "gif");
            Utilities.registerClosableOverlay(assets.getSecond(), () -> Mangrypt.getBase().setSecondLayerRoot(null), assets.getThird());
            Mangrypt.getBase().setSecondLayerRoot(assets.getSecond());
            Platform.runLater(() -> assets.getFirst().requestFocus());
        }, existingNames);
    }

    public static void addMediaElement(FolderElement parentFolder, Consumer<FileSystemElement> callback, Set<String> existingNames) {
        nameInputDialog(name -> {
            File userHome = new File(System.getProperty("user.home"));
            Tuple3<IOLoaderNode, StackPane, StackPane> assets = IOLoaderNode.open(
                    "Select video or audio file",
                    userHome,
                    data -> {
                        Mangrypt.getBase().setSecondLayerRoot(null);

                        MediaDataElement element = new MediaDataElement();
                        element.bytes(data.getFirst());
                        element.fileExtension(data.getSecond());
                        element.ensureFields(name, parentFolder);
                        parentFolder.getContent().put(name, element);
                        callback.accept(element);
                        ConfigIO.markShouldSave();
                    },
                    ex -> {
                        Mangrypt.getBase().showErrorAlert(String.valueOf(ex));
                        throw new RuntimeException("Error accessing media", ex);
                    },
                    () -> Mangrypt.getBase().setSecondLayerRoot(null),
                    "mp4", "mp3", "aac", "wav");
            Utilities.registerClosableOverlay(assets.getSecond(), () -> Mangrypt.getBase().setSecondLayerRoot(null), assets.getThird());
            Mangrypt.getBase().setSecondLayerRoot(assets.getSecond());
            Platform.runLater(() -> assets.getFirst().requestFocus());
        }, existingNames);
    }

    private static void nameInputDialog(Consumer<String> action, Set<String> existingNames) {
        Mangrypt.getBase().showInputDialog(
                "Please set a name for the new dataset",
                "Name",
                "",
                true,
                name -> {
                    if (name.isBlank() || name.equals(".")) return false;
                    return !existingNames.contains(name);
                },
                name -> {
                    if (existingNames.contains(name)) return "Such an element already exists";
                    if (name.startsWith(".")) return "Elements starting with . are hidden";
                    return null;
                },
                action
        );
    }
}
