package io.github.redstonemango.mangrypt.front;

import io.github.redstonemango.mangoutils.function.TriConsumer;
import io.github.redstonemango.mangoutils.tuple.Tuple3;
import io.github.redstonemango.mangrypt.Mangrypt;
import io.github.redstonemango.mangrypt.back.ConfigIO;
import io.github.redstonemango.mangrypt.back.Utilities;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class IOLoaderNode extends VBox {

    private final TreeView<File> fileView;
    private final TextField filePathField;
    private Button doneButton;
    private ContextMenu pathCompletionMenu;
    private ListView<String> pathCompletionList;
    private boolean ignorePathChange = false;

    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://)?(www\\.)?((([\\w-]+\\.)+[a-zA-Z]{2,63})|(\\d{1,3}(\\.\\d{1,3}){3}))(:\\d{1,5})?(/[\\w\\-._~%!$&'()*+,;=:@/]*)*(\\?[\\w\\-._~%!$&'()*+,;=:@/?]*)?(#[\\w\\-._~%!$&'()*+,;=:@/?]*)?$");

    public static Tuple3<IOLoaderNode, StackPane, StackPane> save(String title, File initialDirectory, Consumer<File> onAction, Runnable onCancel, String... allowedExtensions) {
        IOLoaderNode node = new IOLoaderNode(title, true, initialDirectory, onAction, (_, _, _) -> {}, _ -> {}, onCancel, allowedExtensions);
        StackPane background = new StackPane();
        StackPane root = new StackPane();
        node.prepareMangryptLayout(root, background);
        return new Tuple3<>(node, root, background);
    }

    public static Tuple3<IOLoaderNode, StackPane, StackPane> open(String title, File initialDirectory, TriConsumer<byte[], String, @Nullable String> onAction, Consumer<Exception> onOpenException, Runnable onCancel, String... allowedExtensions) {
        IOLoaderNode node = new IOLoaderNode(title, false, initialDirectory, _ -> {}, onAction, onOpenException, onCancel, allowedExtensions);
        StackPane background = new StackPane();
        StackPane root = new StackPane();
        node.prepareMangryptLayout(root, background);
        return new Tuple3<>(node, root, background);
    }

    private IOLoaderNode(String title, boolean saveMode, File initialDirectory, Consumer<File> onSaveAction, TriConsumer<byte[], String, @Nullable String> onOpenAction, Consumer<Exception> onOpenException, Runnable onCancel, String... allowedExtensions) {
        List<String> extensions = normalizeAllowedExtensions(allowedExtensions);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("uncolored-label");
        titleLabel.setFont(Font.font("", FontWeight.NORMAL, FontPosture.REGULAR, 20));

        createPathPopup();

        filePathField = new TextField();
        filePathField.setFocusTraversable(false);
        filePathField.textProperty().addListener((_, _, _) -> {
            if (ignorePathChange) {
                ignorePathChange = false;
            } else {
                updatePathPopup(filePathField.getText());
            }

            if (saveMode && doneButton != null) {
                File file = fileFromString(filePathField.getText());
                doneButton.setDisable(!isValidFileExtension(file, extensions) || file.isDirectory());
            }
        });
        filePathField.focusedProperty().addListener((_, _, focused) -> {
            if (focused) {
                showPathPopup();
                updatePathPopup(filePathField.getText());
            } else pathCompletionMenu.hide();
        });
        EventHandler<KeyEvent> keyEvent = e -> {
            if (!(filePathField.isFocused() || pathCompletionMenu.isFocused())) return;

            if (e.getCode() == KeyCode.ENTER) {
                boolean focus = true;
                if (pathCompletionMenu.isShowing() && pathCompletionList.getSelectionModel().getSelectedItem() != null) {
                    String pre = filePathField.getText().substring(0, filePathField.getText().lastIndexOf("/") + 1);
                    ignorePathChange = true;
                    filePathField.setText(pre + pathCompletionList.getSelectionModel().getSelectedItem());
                    filePathField.positionCaret(filePathField.getText().length());
                    showPathPopup();
                    updatePathPopup(filePathField.getText());

                    focus = false;
                }

                select(filePathField.getText(), focus);
                Platform.runLater(() -> updatePathPopup(filePathField.getText()));
            } else if ((e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.UP) && !pathCompletionList.getItems().isEmpty()) {
                int size = pathCompletionList.getItems().size();
                int currentIndex = pathCompletionList.getSelectionModel().getSelectedIndex();
                int direction = e.getCode() == KeyCode.DOWN ? 1 : -1;
                int i = (currentIndex + direction + size) % size;
                pathCompletionList.getSelectionModel().select(i);
                pathCompletionList.scrollTo(i);
                e.consume();
            }
        };
        pathCompletionMenu.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent);
        filePathField.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent);


        Path workingDirRoot = Path.of(System.getProperty("user.dir")).getRoot();
        TreeItem<File> rootItem = createNode(workingDirRoot == null ? new File(System.getProperty("user.home")) : workingDirRoot.toFile());
        fileView = new TreeView<>(rootItem);
        fileView.setShowRoot(true);
        VBox.setVgrow(fileView, Priority.ALWAYS);
        fileView.getSelectionModel().select(rootItem);

        fileView.setCellFactory(_ -> {
            final long[] lastClick = {-1};
            TreeCell<File> cell = new TreeCell<>() {
                @Override
                protected void updateItem(File file, boolean empty) {
                    super.updateItem(file, empty);
                    if (empty || file == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(file.getName().isEmpty() ? file.getAbsolutePath() : file.getName());
                        boolean valid = isValidFileExtension(file, extensions) || file.isDirectory();
                        pseudoClassStateChanged(PseudoClass.getPseudoClass("invalid-content"), !valid);
                    }
                }
            };
            cell.setOnMouseClicked(_ -> {
                if (cell.getItem() == null) return;
                if (!cell.getItem().isFile()) return;

                if (System.currentTimeMillis() - lastClick[0] < 250) {
                    doneButton.fire();
                }
                lastClick[0] = System.currentTimeMillis();
            });
            return cell;
        });
        fileView.getSelectionModel().selectedItemProperty().addListener((_, _, item) -> {
            if (item == null) {
                doneButton.setDisable(true);
                return;
            }

            doneButton.setDisable(!isValidFileExtension(item.getValue(), extensions) || item.getValue().isDirectory());
            ignorePathChange = true;
            filePathField.setText(item.getValue().getAbsolutePath() + (item.getValue().isDirectory() ? "/" : ""));
            filePathField.positionCaret(filePathField.getText().length());
        });
        fileView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) doneButton.fire();
        });
        VBox fileChooserBox = new VBox(10, filePathField, fileView);
        fileChooserBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(fileChooserBox, Priority.ALWAYS);

        VBox webLoadBox = new VBox(10);
        webLoadBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(webLoadBox, Priority.ALWAYS);

        Font urlInfoFont = Font.font("", FontWeight.NORMAL, FontPosture.REGULAR, 15);
        Label urlInfoLabel1 = new Label("Please enter a valid web URL to a resource");
        urlInfoLabel1.setWrapText(true);
        urlInfoLabel1.setTextAlignment(TextAlignment.CENTER);
        urlInfoLabel1.setFont(urlInfoFont);
        urlInfoLabel1.getStyleClass().add("uncolored-label");
        Label urlInfoLabel2 = new Label("This url will be accessed once to store the data locally. After this call, " +
                "no more web requests will be made");
        urlInfoLabel2.setWrapText(true);
        urlInfoLabel2.setTextAlignment(TextAlignment.CENTER);
        urlInfoLabel2.setFont(urlInfoFont);
        urlInfoLabel2.getStyleClass().add("uncolored-label");
        Label urlInfoLabel3 = new Label("!! Third-party resources might contain malicious data. Only load resources " +
                "you trust. Consider using TSL encryption (https URLs) for a more secure connection !!");
        urlInfoLabel3.setTextFill(Color.DARKGOLDENROD);
        urlInfoLabel3.setWrapText(true);
        urlInfoLabel3.setTextAlignment(TextAlignment.CENTER);
        urlInfoLabel3.setFont(urlInfoFont);
        VBox.setMargin(urlInfoLabel3, new Insets(0, 0, 20, 0));

        Label urlInvalidLabel = new Label("The supplied web URL is not valid");
        urlInvalidLabel.setTextFill(Color.RED);
        urlInvalidLabel.setText("The supplied web URL is not valid");
        VBox.setMargin(urlInvalidLabel, new Insets(-5, 0, 0, 0));

        TextField urlField = new TextField();
        urlField.setPrefWidth(Double.MAX_VALUE);
        urlField.textProperty().addListener((_, _, urlString) -> {
            boolean valid = URL_PATTERN.matcher(urlString).matches();
            doneButton.setDisable(!valid);
            urlInvalidLabel.setVisible(!valid);
        });
        urlField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) doneButton.fire();
        });
        urlField.prefWidthProperty().bind(webLoadBox.widthProperty());
        webLoadBox.getChildren().addAll(urlInfoLabel1, urlInfoLabel2, urlInfoLabel3, urlField, urlInvalidLabel);
        VBox.setMargin(webLoadBox, new Insets(0, 50, 0, 50));

        doneButton = new Button(saveMode ? "Save" : "Select");
        doneButton.setPrefWidth(80);
        doneButton.setDisable(true);
        doneButton.setOnAction(_ -> {
            if (Mangrypt.getBase().isObscuring()) return;

            if (saveMode) {
                File file = fileFromString(filePathField.getText());
                if (isValidFileExtension(file, extensions) && !file.isDirectory()) onSaveAction.accept(file);
                return;
            }

            if (getChildren().contains(fileChooserBox)) {
                if (fileView.getSelectionModel().getSelectedItem() == null) return;
                File file = fileView.getSelectionModel().getSelectedItem().getValue();
                byte[] bytes = new byte[0];
                try {
                    bytes = Files.readAllBytes(file.toPath());
                } catch (IOException e) {
                    onOpenException.accept(e);
                }
                if (isValidFileExtension(file, extensions) && !file.isDirectory()) {
                    String fileExtension = file.getName().substring(file.getName().lastIndexOf("."));
                    onOpenAction.accept(bytes, fileExtension, null);
                }
            }
            else {
                loadFromWeb(urlField.getText(), onOpenAction, onOpenException, extensions);
            }
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(80);
        cancelButton.setOnAction(_ -> onCancel.run());


        CheckBox toggleUrlDescriptionBox = new CheckBox("Store URL In Description");
        toggleUrlDescriptionBox.setFocusTraversable(false);
        toggleUrlDescriptionBox.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        toggleUrlDescriptionBox.setSelected(ConfigIO.getConfig().descriptionOnDownload());
        toggleUrlDescriptionBox.selectedProperty().addListener((_, _, isSelected) -> {
                ConfigIO.getConfig().descriptionOnDownload(isSelected);
                ConfigIO.markShouldSave();
        });

        HBox.setMargin(toggleUrlDescriptionBox, new Insets(4, 0, 0, 0));

        HBox controlsBox = new HBox();

        Button typeChangeButton = new Button("From Web URL");
        typeChangeButton.setPrefWidth(110);
        typeChangeButton.setOnAction(_ -> {
            if (getChildren().contains(webLoadBox)) {
                getChildren().remove(webLoadBox);
                getChildren().add(1, fileChooserBox);
                controlsBox.getChildren().remove(toggleUrlDescriptionBox);
                fileView.requestFocus();
                typeChangeButton.setText("From Web URL");

                TreeItem<File> selectedItem = fileView.getSelectionModel().getSelectedItem();
                if (selectedItem == null) {
                    doneButton.setDisable(true);
                    return;
                }
                doneButton.setDisable(!isValidFileExtension(selectedItem.getValue(), extensions) || selectedItem.getValue().isDirectory());
            }
            else {
                getChildren().remove(fileChooserBox);
                getChildren().add(1, webLoadBox);
                controlsBox.getChildren().add(2, toggleUrlDescriptionBox);
                urlField.requestFocus();
                typeChangeButton.setText("From Local File");

                boolean valid = URL_PATTERN.matcher(urlField.getText()).matches();
                doneButton.setDisable(!valid);
            }
        });
        Region spacingRegion = new Region();
        HBox.setHgrow(spacingRegion, Priority.ALWAYS);

        controlsBox.setSpacing(10);
        controlsBox.getChildren().addAll(spacingRegion, cancelButton, doneButton);
        if (!saveMode) controlsBox.getChildren().addFirst(typeChangeButton);
        controlsBox.setPadding(new Insets(0, 10, 0, 10));

        getChildren().addAll(titleLabel, fileChooserBox, controlsBox);
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(10, 0, 10, 0));

        // Select initial dir
        select(initialDirectory.getAbsolutePath(), true);
        ignorePathChange = true;
        filePathField.setText(initialDirectory.getAbsolutePath());
    }

    private void loadFromWeb(String rawUrl, TriConsumer<byte[], String, @Nullable String> onDownloaded, Consumer<Exception> onException, List<String> extensions) {
        String url = !(rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) ? "https://" + rawUrl : rawUrl;
        Runnable download = () -> {
            boolean handleExceptionInternally = true;
            try {
                URL remoteUrl = new URI(url).toURL();
                HttpURLConnection connection = (HttpURLConnection) remoteUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept-Encoding", "identity");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                            responseCode == 307 || responseCode == 308) {
                        Mangrypt.getBase().showWarningAlert("Received an HTTP " + responseCode + " error while " +
                                "downloading.\nYou might be able to fix this by explicitly setting the correct http" +
                                "-protocol (http:// or https://)");
                        return;
                    }
                    handleExceptionInternally = false;
                    onException.accept(new RuntimeException("Failed to download file. HTTP response code: " + responseCode));
                    return;
                }

                String contentType = connection.getContentType();
                String guessedExtension = Utilities.getSupportedExtension(contentType);

                if (!extensions.contains(guessedExtension.toLowerCase()) && !extensions.isEmpty()) {
                    Mangrypt.getBase().showWarningAlert("The file under the specified URL is not of an allowed type:\n\n" +
                            "Does not allow '" + contentType + "' in this context");
                    connection.disconnect();
                    return;
                }

                try (InputStream rawInput = connection.getInputStream();
                     BufferedInputStream inputStream = new BufferedInputStream(rawInput);
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    byte[] fileBytes = outputStream.toByteArray();
                    handleExceptionInternally = false;
                    onDownloaded.accept(fileBytes, guessedExtension, rawUrl);
                    handleExceptionInternally = true;
                }
                finally {
                    connection.disconnect();
                }

            }
            catch (Exception e) {
                if (handleExceptionInternally) onException.accept(e);
            }
        };

        if (!isTLSEnabled(url)) {
            Mangrypt.getBase().showConfirmationDialog(
                    "Unencrypted connection",
                    "The URL does not provide an encrypted TLS connection. Do you still want to continue?",
                    download);
        } else {
            download.run();
        }
    }

    public static boolean isTLSEnabled(String urlString) {
        try {
            URI uri = new URI(urlString);

            String host = uri.getHost();
            int port = (uri.getPort() == -1) ? 443 : uri.getPort();

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
                socket.setSoTimeout(5000);
                socket.startHandshake();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void prepareMangryptLayout(StackPane root, StackPane backgroundBox) {
        setPadding(new Insets(5));

        backgroundBox.getChildren().add(this);
        backgroundBox.setPadding(new Insets(20));
        backgroundBox.setBackground(
                new Background(new BackgroundFill(Color.DARKGREEN, new CornerRadii(20), null))
        );

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(backgroundBox.widthProperty());
        clip.heightProperty().bind(backgroundBox.heightProperty());
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        backgroundBox.setClip(clip);

        root.getChildren().add(backgroundBox);
        root.setPickOnBounds(true);
        root.setStyle("-fx-background-color: transparent;");
        StackPane.setMargin(backgroundBox, new Insets(50));

        pathCompletionList.getStyleClass().remove("list-view");
        pathCompletionList.getStyleClass().add("popup-list");
        pathCompletionMenu.getItems().getFirst().getStyleClass().remove("menu-item");
        pathCompletionMenu.getItems().getFirst().getStyleClass().add("popup-menu-root");
    }

    private void select(String path, boolean requestFocus) {
        File f = fileFromString(path);
        TreeItem<File> item = findItemByFile(f);
        if (item == null) return;
        if (item.getValue().isDirectory() && !item.isExpanded()) item.setExpanded(true);
        fileView.getSelectionModel().select(item);
        fileView.scrollTo(fileView.getSelectionModel().getSelectedIndex());
        if (requestFocus) fileView.requestFocus();
    }

    private TreeItem<File> createNode(final File file) {
        return new TreeItem<>(file) {
            private boolean isLeaf;
            private boolean isFirstTimeChildren = true;
            private boolean isFirstTimeLeaf = true;

            @Override
            public boolean isLeaf() {
                if (isFirstTimeLeaf) {
                    isFirstTimeLeaf = false;
                    isLeaf = file.isFile();
                }
                return isLeaf;
            }

            @Override
            public ObservableList<TreeItem<File>> getChildren() {
                if (isFirstTimeChildren) {
                    isFirstTimeChildren = false;
                    super.getChildren().setAll(buildChildren(this));
                }
                return super.getChildren();
            }

            private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> treeItem) {
                File f = treeItem.getValue();
                if (f != null && f.isDirectory()) {
                    File[] files = f.listFiles();
                    if (files != null) {
                        ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();

                        for (File childFile : files) {
                            children.add(createNode(childFile));
                        }
                        return children;
                    }
                }
                return FXCollections.emptyObservableList();
            }
        };
    }

    private static boolean isValidFileExtension(File file, List<String> extensions) {
        return extensions.stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .anyMatch(ext -> file.getName().toLowerCase(Locale.ROOT).endsWith(ext)) || extensions.isEmpty();
    }

    private List<String> normalizeAllowedExtensions(String... allowedExtensions) {
        List<String> normalized = new ArrayList<>();
        for (String s : allowedExtensions) {
            if (s.startsWith(".") || s.isBlank()) normalized.add(s);
            else if (s.startsWith("*.")) normalized.add(s.substring("*".length()));
            else normalized.add("." + s);
        }
        return normalized;
    }

    private TreeItem<File> findItemByFile(File file) {
        TreeItem<File> current = fileView.getRoot();
        if (current == null) return null;

        Path targetPath = file.toPath().toAbsolutePath();

        for (Path pathPart : targetPath) {
            boolean found = false;

            if (!current.isExpanded()) {
                current.setExpanded(true);
            }

            ObservableList<TreeItem<File>> children = current.getChildren();

            for (TreeItem<File> child : children) {
                if (child.getValue().toPath().getFileName().equals(pathPart)) {
                    current = child;
                    found = true;
                    break;
                }
            }

            if (!found) {
                break;
            }
        }
        return current;
    }

    private File fileFromString(String s) {
        String root = fileView.getRoot().getValue().getAbsolutePath();
        return new File(s.isBlank() ? root : (s.startsWith(root) ? s : root + s));
    }

    private void updatePathPopup(String path) {
        pathCompletionList.getItems().clear();
        TreeItem<File> currentPos = findItemByFile(fileFromString(path));
        if (currentPos == null) {
            pathCompletionMenu.hide();
            return;
        }
        String filter = fileFromString(path).exists() ? "" : path.substring(path.lastIndexOf("/") + 1);
        File[] options = currentPos.getValue().listFiles((_, name) -> name.contains(filter));
        if (options == null) {
            pathCompletionMenu.hide();
            return;
        }


        Font font = Font.font("", 12);
        double maxTextWidth = 0;
        for (File option : options) {
            String name = option.getName();
            if (option.isDirectory()) name = name + "/";
            pathCompletionList.getItems().add(name);
            Text text = new Text(name);
            text.setFont(font);
            double width = text.getLayoutBounds().getWidth();
            maxTextWidth = Math.max(maxTextWidth, width);
        }

        double rowHeight = 24.0;
        int visibleRows = Math.min(pathCompletionList.getItems().size(), 10);
        pathCompletionList.setPrefHeight(visibleRows * rowHeight);
        pathCompletionList.setMaxHeight(visibleRows * rowHeight);
        double extraPadding = 60.0;
        pathCompletionList.setPrefWidth(maxTextWidth + extraPadding);
        pathCompletionList.setMaxWidth(maxTextWidth + extraPadding);

        if (pathCompletionList.getItems().isEmpty()) {
            pathCompletionMenu.hide();
        }
        else {
            showPathPopup();
        }
    }

    private void showPathPopup() {
        Point2D fieldPos = filePathField.localToScreen(0, 0);
        if (fieldPos == null) return;
        pathCompletionMenu.show(filePathField, fieldPos.getX(), fieldPos.getY() + filePathField.getHeight());
    }

    private void createPathPopup() {
        pathCompletionList = new ListView<>();
        pathCompletionList.setPrefHeight(0);
        pathCompletionList.setMaxHeight(0);
        pathCompletionList.setCellFactory(_ -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String s, boolean b) {
                    super.updateItem(s, b);
                    if (s == null || b) {
                        setText(null);
                        setGraphic(null);
                    }
                    else {
                        setText(s);
                    }
                }
            };
            cell.setOnMouseEntered(_ -> cell.getListView().getSelectionModel().select(cell.getItem()));
            return cell;
        });

        CustomMenuItem scrollableItem = new CustomMenuItem(pathCompletionList, false);
        pathCompletionMenu = new ContextMenu(scrollableItem);
    }
}
