package io.github.redstonemango.mangrypt.back;

import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.dataTypes.*;
import io.github.redstonemango.mangrypt.front.FileChooserNode;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.nio.file.Files;
import java.util.Set;
import java.util.function.Consumer;

public class ContentAdder {

    public static void addFolder(FolderElement parentFolder, Consumer<FileSystemElement> callback, Set<String> existingNames) {
        nameInputDialog(name -> {
            FolderElement newFolder = new FolderElement();
            newFolder.setName(name);
            newFolder.ensureFields();
            parentFolder.getContent().put(name, newFolder);
            callback.accept(newFolder);
            ConfigIO.markShouldSave();
        }, existingNames);
    }

    public static void addTextElement(FolderElement parentFolder, Consumer<FileSystemElement> callback, Set<String> existingNames) {
        nameInputDialog(name -> {
            TextDataElement element = new TextDataElement();
            element.setName(name);
            element.ensureFields();
            parentFolder.getContent().put(name, element);
            callback.accept(element);
            ConfigIO.markShouldSave();
        }, existingNames);
    }

    public static void addImageElement(FolderElement parentFolder, Consumer<FileSystemElement> callback, Set<String> existingNames) {
        nameInputDialog(name -> {
            File userHome = new File(System.getProperty("user.home"));
            FileChooserNode chooser = new FileChooserNode("Select image file", false, userHome,
                    selectedFile -> {
                        Mangrypt.getBase().setSecondLayerRoot(null);
                        byte[] bytes;
                        try {
                            bytes = Files.readAllBytes(selectedFile.toPath());
                        }
                        catch (Exception e) {
                            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                            throw new RuntimeException("Error reading image file", e);
                        }

                        String fileExtension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                        ImageDataElement element = new ImageDataElement();
                        element.setName(name);
                        element.bytes(bytes);
                        element.fileExtension(fileExtension);
                        element.ensureFields();
                        parentFolder.getContent().put(name, element);
                        callback.accept(element);
                        ConfigIO.markShouldSave();
                    },
                    () -> Mangrypt.getBase().setSecondLayerRoot(null), "jpeg", "jpg", "png", "bmp", "gif");
            StackPane background = new StackPane();
            StackPane root = new StackPane();
            chooser.prepareMangryptLayout(root, background);
            Utilities.registerClosableOverlay(root, () -> Mangrypt.getBase().setSecondLayerRoot(null), background);
            Mangrypt.getBase().setSecondLayerRoot(root);
            Platform.runLater(chooser::requestFocus);
        }, existingNames);
    }

    public static void addMediaElement(FolderElement parentFolder, Consumer<FileSystemElement> callback, Set<String> existingNames) {
        nameInputDialog(name -> {
            File userHome = new File(System.getProperty("user.home"));
            FileChooserNode chooser = new FileChooserNode("Select video or audio file", false, userHome,
                    selectedFile -> {
                        Mangrypt.getBase().setSecondLayerRoot(null);
                        byte[] bytes;
                        try {
                            bytes = Files.readAllBytes(selectedFile.toPath());
                        }
                        catch (Exception e) {
                            Mangrypt.getBase().showErrorAlert(String.valueOf(e));
                            throw new RuntimeException("Error reading media file", e);
                        }

                        String fileExtension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                        MediaDataElement element = new MediaDataElement();
                        element.setName(name);
                        element.bytes(bytes);
                        element.fileExtension(fileExtension);
                        element.ensureFields();
                        parentFolder.getContent().put(name, element);
                        callback.accept(element);
                        ConfigIO.markShouldSave();
                    },
                    () -> Mangrypt.getBase().setSecondLayerRoot(null), "mp4", "mp3", "aac", "wav");
            StackPane background = new StackPane();
            StackPane root = new StackPane();
            chooser.prepareMangryptLayout(root, background);
            Utilities.registerClosableOverlay(root, () -> Mangrypt.getBase().setSecondLayerRoot(null), background);
            Mangrypt.getBase().setSecondLayerRoot(root);
            Platform.runLater(chooser::requestFocus);
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
