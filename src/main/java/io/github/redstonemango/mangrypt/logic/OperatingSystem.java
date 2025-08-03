package io.github.redstonemango.mangrypt.logic;

import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A utility enum providing useful OS related features
 * @author RedStoneMango
 */
public enum OperatingSystem {

    /**
     * An open-source operating system
     */
    LINUX {
        /**
         * Creates the command to open a {@linkplain URI URI}
         * @param uri The {@linkplain URI URi} the command should open
         * @return An array of {@linkplain String Strings} forming the command
         */
        @Override
        String[] createUriOpenCommand(URI uri) {
            String string = uri.toString();
            if ("file".equals(uri.getScheme())) {
                string = string.replace("file:", "file://");
            }

            return new String[]{"xdg-open", string};
        }

        @Nullable AtomicReference<String> usedManagerCache = null;

        /**
         * Creates the command to browse a {@linkplain File File} in default file explorer
         * @param file The {@linkplain File File} the command should open
         * @return An array of {@linkplain String Strings} forming the command
         */
        @Override
        public String[] createFileBrowseCommand(File file) {
            String[] fileManagers = {"nautilus", "thuxnar", "dolphin", "caja", "io.elementary.files"};
            if (usedManagerCache == null) {
                usedManagerCache = new AtomicReference<>();
                for (String manager : fileManagers) {
                    if (isProcessExisting(manager)) {
                        usedManagerCache.set(manager);
                    }
                }
            }

            if (usedManagerCache.get() == null) return createUriOpenCommand(file.toURI());
            return new String[]{usedManagerCache.get(), "--select", file.getAbsolutePath()};
        }

        /**
         * Returns just the parameter passed in because the keyboard layout Linux uses is the default one.
         * <br/>
         * To find out about the use of this method, please refer to {@linkplain OperatingSystem#unifyKeyEvent(KeyEvent) unifyKeyEvent(KeyEvent)}
         * @param keyEvent The {@linkplain KeyEvent KeyEvent} to unify
         * @return The unified (unchanged) {@linkplain KeyEvent KeyEvent}
         */
        @Override
        public KeyEvent unifyKeyEvent(KeyEvent keyEvent) {
            return keyEvent;
        }


        /**
         * Returns just the parameter passed in because the keyboard layout Linux uses is the default one.
         * <br/>
         * To find out about the use of this method, please refer to {@linkplain OperatingSystem#unifyKeyEvent(javafx.scene.input.KeyEvent) unifyKeyEvent(KeyEvent)}
         * @param keyEvent The {@linkplain javafx.scene.input.KeyEvent KeyEvent} to unify
         * @return The unified (unchanged) {@linkplain javafx.scene.input.KeyEvent KeyEvent}
         */
        @Override
        public javafx.scene.input.KeyEvent unifyKeyEvent(javafx.scene.input.KeyEvent keyEvent) {
            return keyEvent;
        }

        /**
         * Creates a {@linkplain File} object for a folder with a given name. The folder will be located in the path Linux typically uses to use to save application configurations.
         * @param folderName The name of the folder.
         * @return The created {@linkplain File} object
         */
        @Override
        public File createAppConfigDir(String folderName) {
            String userHome = System.getProperty("user.home");
            String configHome = System.getenv("XDG_CONFIG_HOME");
            if (configHome == null || configHome.isEmpty()) {
                configHome = userHome + "/.config";
            }
            return new File(configHome, folderName);
        }

        boolean isProcessExisting(String processName) {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"which", processName});
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                return reader.readLine() != null;
            } catch (IOException ignore) {
                return false;
            }
        }
    },
    /**
     * An operating system developed by Microsoft
     */
    WINDOWS {
        /**
         * Creates the command to open a {@linkplain URI URI}
         * @param uri The {@linkplain URI URI} the command should open
         * @return An array of {@linkplain String Strings} forming the command
         */
        @Override
        String[] createUriOpenCommand(URI uri) {
            return new String[]{"start", uri.toString()};
        }

        /**
         * Creates the command to browse a {@linkplain File File} in default file explorer
         * @param file The {@linkplain File File} the command should open
         * @return An array of {@linkplain String Strings} forming the command
         */
        @Override
        String[] createFileBrowseCommand(File file) {
            return new String[]{"explorer.exe", "/select," + file.getAbsolutePath()};
        }

        /**
         * Returns just the parameter passed in because the keyboard layout Windows uses is the default one.
         * <br/>
         * To find out about the use of this method, please refer to {@linkplain OperatingSystem#unifyKeyEvent(KeyEvent) unifyKeyEvent(KeyEvent)}
         * @param keyEvent The {@linkplain KeyEvent KeyEvent} to unify
         * @return The unified (unchanged) {@linkplain KeyEvent KeyEvent}
         */
        @Override
        public KeyEvent unifyKeyEvent(KeyEvent keyEvent) {
            return keyEvent;
        }

        /**
         * Returns just the parameter passed in because the keyboard layout Windows uses is the default one.
         * <br/>
         * To find out about the use of this method, please refer to {@linkplain OperatingSystem#unifyKeyEvent(javafx.scene.input.KeyEvent) unifyKeyEvent(KeyEvent)}
         * @param keyEvent The {@linkplain javafx.scene.input.KeyEvent KeyEvent} to unify
         * @return The unified (unchanged) {@linkplain javafx.scene.input.KeyEvent KeyEvent}
         */
        @Override
        public javafx.scene.input.KeyEvent unifyKeyEvent(javafx.scene.input.KeyEvent keyEvent) {
            return keyEvent;
        }

        /**
         * Creates a {@linkplain File} object for a folder with a given name. The folder will be located in the path Windows typically uses to use to save application configurations.
         * @param folderName The name of the folder.
         * @return The created {@linkplain File} object
         */
        @Override
        public File createAppConfigDir(String folderName) {
            String userHome = System.getProperty("user.home");
            return new File(new File(userHome, "AppData/Local"), folderName);
        }


    },
    /**
     * An operating system developed by Apple
     */
    MAC {
        /**
         * Creates the command to open a {@linkplain URI URI}
         * @param uri The {@linkplain URI URI} the command should open
         * @return An array of {@linkplain String Strings} forming the command
         */
        @Override
        String[] createUriOpenCommand(URI uri) {
            return new String[]{"open", uri.toString()};
        }

        /**
         * Creates the command to browse a {@linkplain File File} in default file explorer
         * @param file The {@linkplain File File} the command should open
         * @return An array of {@linkplain String Strings} forming the command
         */
        @Override
        String[] createFileBrowseCommand(File file) {
            return new String[]{"open", "-R", file.getAbsolutePath()};
        }

        /**
         * Unifies the Mac keyboard modifiers from a {@linkplain KeyEvent KeyEvent} to the Windows default modifiers.
         * <br/>
         * This ensures {@linkplain KeyEvent#isControlDown() isControlDown()} will correctly check for Cmd key to be pressed.
         * @param keyEvent The {@linkplain KeyEvent KeyEvent} to unify
         * @return The unified {@linkplain KeyEvent KeyEvent}
         */
        @Override
        public KeyEvent unifyKeyEvent(KeyEvent keyEvent) {
            int modifiers = 0;
            if (keyEvent.isMetaDown()) modifiers = modifiers | KeyEvent.CTRL_DOWN_MASK;
            if (keyEvent.isAltDown() || keyEvent.isAltGraphDown())  modifiers = modifiers | KeyEvent.ALT_DOWN_MASK | KeyEvent.ALT_GRAPH_DOWN_MASK;
            if (keyEvent.isControlDown()) modifiers = modifiers | KeyEvent.META_DOWN_MASK;
            if (keyEvent.isShiftDown()) modifiers = modifiers | KeyEvent.SHIFT_DOWN_MASK;

            return new KeyEvent(
                    keyEvent.getComponent(),
                    keyEvent.getID(),
                    keyEvent.getWhen(),
                    modifiers,
                    keyEvent.getKeyCode(),
                    keyEvent.getKeyChar(),
                    keyEvent.getKeyLocation()
            );
        }


        /**
         * Unifies the Mac keyboard modifiers from a {@linkplain javafx.scene.input.KeyEvent KeyEvent} to the Windows default modifiers.
         * <br/>
         * This ensures {@linkplain javafx.scene.input.KeyEvent#isControlDown() isControlDown()} will correctly check for Cmd key to be pressed.
         * @param keyEvent The {@linkplain javafx.scene.input.KeyEvent KeyEvent} to unify
         * @return The unified {@linkplain javafx.scene.input.KeyEvent KeyEvent}
         */
        @Override
        public javafx.scene.input.KeyEvent unifyKeyEvent(javafx.scene.input.KeyEvent keyEvent) {
            return new javafx.scene.input.KeyEvent(
                    keyEvent.getSource(),
                    keyEvent.getTarget(),
                    keyEvent.getEventType(),
                    keyEvent.getCharacter(),
                    keyEvent.getText(),
                    keyEvent.getCode(),
                    keyEvent.isShiftDown(),
                    keyEvent.isMetaDown(),
                    keyEvent.isAltDown(),
                    keyEvent.isControlDown()
            );
        }

        /**
         * Creates a {@linkplain File} object for a folder with a given name. The folder will be located in the path Mac typically uses to use to save application configurations.
         * @param folderName The name of the folder.
         * @return The created {@linkplain File} object
         */
        @Override
        public File createAppConfigDir(String folderName) {
            String userHome = System.getProperty("user.home");
            return new File(new File(userHome, "Library/Application Support"), folderName);
        }
    },
    /**
     * An enum constant that can be used to express an unknown operating system (An OS that isn't {@linkplain #WINDOWS Windows}, {@linkplain #MAC MacOS}, or {@linkplain #LINUX Linux})
     * Its methods will always return {@linkplain #LINUX Linux's data} because they are the most commonly used ones by other OSs
     */
    UNKNOWN {
        /**
         * Guesses a command to open a {@linkplain URI uri} that most likely works on this OS
         * @param uri The {@linkplain URI uri} the command should open
         * @return An array of {@linkplain String strings} forming the command by the scheme Linux uses
         */
        @Override
        String[] createUriOpenCommand(URI uri) {
            return LINUX.createUriOpenCommand(uri);
        }

        /**
         * Guesses a command to browse a {@linkplain File file} in default file explorer that most likely works on this OS
         * @param file The {@linkplain File file} the command should open
         * @return An array of {@linkplain String strings} forming the command by the scheme Linux uses
         */
        @Override
        String[] createFileBrowseCommand(File file) {
            return LINUX.createFileBrowseCommand(file);
        }

        /**
         * Returns just the parameter passed in because the keyboard layout Linux uses is the default one.
         * <br/>
         * To find out about the use of this method, please refer to {@linkplain OperatingSystem#unifyKeyEvent(KeyEvent) unifyKeyEvent(KeyEvent)}
         * @param keyEvent The {@linkplain KeyEvent KeyEvent} to unify
         * @return The unified (unchanged) {@linkplain KeyEvent KeyEvent}
         */
        @Override
        public KeyEvent unifyKeyEvent(KeyEvent keyEvent) {
            return keyEvent;
        }

        /**
         * Returns just the parameter passed in because the keyboard layout Linux uses is the default one.
         * <br/>
         * To find out about the use of this method, please refer to {@linkplain OperatingSystem#unifyKeyEvent(javafx.scene.input.KeyEvent) unifyKeyEvent(KeyEvent)}
         * @param keyEvent The {@linkplain javafx.scene.input.KeyEvent KeyEvent} to unify
         * @return The unified (unchanged) {@linkplain javafx.scene.input.KeyEvent KeyEvent}
         */
        @Override
        public javafx.scene.input.KeyEvent unifyKeyEvent(javafx.scene.input.KeyEvent keyEvent) {
            return keyEvent;
        }

        /**
         * Creates a {@linkplain File} object for a folder with a given name. The folder will be located in the path the OS is most likely to use to save application configurations.
         * @param folderName The name of the folder.
         * @return The created {@linkplain File} object
         */
        @Override
        public File createAppConfigDir(String folderName) {
            return LINUX.createAppConfigDir(folderName);
        }
    };

    /**
     * Reads the value of the systems "os.name" property
     * @return The current OS's Name
     */
    public static String readCurrentOSName() {
        return System.getProperty("os.name");
    }

    /**
     * Reads the value of the systems "os.version" property
     * @return The current OS's version
     */
    public static String readCurrentOSVersion() {
        return System.getProperty("os.version");
    }

    /**
     * Reads the value of the systems "os.arch" property
     * @return The current OS's architecture
     */
    public static String readCurrentOSArch() {
        return System.getProperty("os.arch");
    }

    /**
     * Reads the user's system data and returns the {@linkplain OperatingSystem OS} used by them. Returns {@linkplain #UNKNOWN unknown OS} if the load's not successful
     * @return The user's OS or {@linkplain #UNKNOWN unknown OS} if method fails
     */
    public static OperatingSystem loadCurrentOS() {

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("nix") || os.contains("nux")) {
            return OperatingSystem.LINUX;
        }
        else if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        }
        else if (os.contains("mac")) {
            return OperatingSystem.MAC;
        }
        else {
            return OperatingSystem.UNKNOWN;
        }
    }

    /**
     * Reads the user's system data and checks whether {@linkplain #LINUX Linux OS} is used
     * @return A boolean that shows if {@linkplain #LINUX Linux OS} is used
     * @see #loadCurrentOS()
     */
    public static boolean isLinux() {
        return loadCurrentOS() == LINUX;
    }

    /**
     * Reads the user's system data and checks whether {@linkplain #WINDOWS Windows OS} is used
     * @return A boolean that shows if {@linkplain #WINDOWS Windows OS} is used
     * @see #loadCurrentOS()
     */
    public static boolean isWindows() {
        return loadCurrentOS() == WINDOWS;
    }

    /**
     * Reads the user's system data and checks whether {@linkplain #MAC MacOS} is used
     * @return A boolean that shows if {@linkplain #MAC MacOS} is used
     * @see #loadCurrentOS()
     */
    public static boolean isMac() {
        return loadCurrentOS() == MAC;
    }

    /**
     * Reads the user's system data and checks whether the OS is {@linkplain #UNKNOWN unknown}
     * @return A boolean that shows if the OS is {@linkplain #UNKNOWN unknown}
     * @see #loadCurrentOS()
     */
    public static boolean isUnknown() {
        return loadCurrentOS() == UNKNOWN;
    }

    /**
     * Opens a {@linkplain File File}/website in the user's preferred application by using the OS's data. A better alternative for {@linkplain java.awt.Desktop Desktop} class which has some issues with non-windows OSs.
     * <br/>
     * This does not show the file in file explorer but opens it directly using an editor application or the default web browser
     * @param file The file to open
     * @see #browse(File)
     */
    public void open(File file) {
        this.open(file.toURI());
    }

    /**
     * Opens a file/website based on a {@linkplain Path path} in the user's preferred application by using the OS's data. A better alternative for {@linkplain java.awt.Desktop Desktop} class which has some issues with non-windows OSs.
     * <br/>
     * This does not show the file in file explorer but opens it directly using an editor application
     * @param path The path of the file that should open
     * @see #browse(File)
     */
    public void open(Path path) {
        this.open(path.toUri());
    }

    /**
     * Opens a file/website based on an {@linkplain URI URI} (that gets resolved by a {@linkplain String String}) in the user's preferred application by using the OS's data. A better alternative for {@linkplain java.awt.Desktop Desktop} class which has some issues with non-windows OSs.
     * <br/>
     * This does not show the file in file explorer but opens it directly using an editor application or the default web browser
     * @param uri The {@linkplain String String} to be converted to a {@linkplain URI URI}
     * @see #browse(URI)
     */
    public void open(String uri) {
        try {
            this.open(URI.create(uri));
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Unable to create URI from string '%s': %s", uri, e.getMessage()));
        }
    }

    /**
     * Opens a file/website based on an {@linkplain URI URI} in the user's preferred application by using the OS's data. A better alternative for {@linkplain java.awt.Desktop Desktop} class which has some issues with non-windows OSs.
     * <br/>
     * This does not show the file in file explorer but opens it directly using an editor application or the default web browser
     * @param uri The {@linkplain URI URI} open
     * @see #browse(URI)
     */
    public void open(URI uri) {
        try {
            Process process = Runtime.getRuntime().exec(this.createUriOpenCommand(uri));
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
        } catch (IOException ignore) {}
    }

    /**
     * Shows a file based on a {@linkplain String String} representing its path in the file explorer by using the OS's data. A better alternative for {@linkplain java.awt.Desktop Desktop} class which has some issues with non-windows OSs.
     * <br/>
     * This does not open the file in an editor/viewer but shows it in the file explorer
     * @param path The {@linkplain String String} the file that should be browsed
     * @see #open(String)
     */
    public void browse(String path) {
        browse(new File(path));
    }

    /**
     * Shows a file based on a {@linkplain URI URI} in the file explorer by using the OS's data. A better alternative for {@linkplain java.awt.Desktop Desktop} class which has some issues with non-windows OSs.
     * <br/>
     * This does not open the file in an editor/viewer but shows it in the file explorer
     * @param uri The {@linkplain URI URI} to the file that should be browsed
     * @see #open(URI)
     */
    public void browse(URI uri) {
        if ("file".equals(uri.getScheme())) {
            browse(new File(uri));
        }
        else {
            throw new IllegalArgumentException("Cannot run browse method for a non-file URI");
        }
    }

    /**
     * Shows a {@linkplain File File} in the file explorer by using the OS's data. A better alternative for {@linkplain java.awt.Desktop Desktop} class which has some issues with non-windows OSs.
     * <br/>
     * This does not open the file in an editor/viewer but shows it in the file explorer
     * @param file The {@linkplain File File} that should be browsed
     * @see #open(File)
     */
    public void browse(File file) {
        try {
            Process process = Runtime.getRuntime().exec(this.createFileBrowseCommand(file));
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * An abstract method that's implementations create the OS required CLI command to open a {@linkplain URI uri}
     * @param uri The {@linkplain URI uri} the command should open
     * @return An array of {@linkplain String Strings} forming the command that can then be executed using a {@linkplain ProcessBuilder ProcessBuilder}
     */
    abstract String[] createUriOpenCommand(URI uri);

    /**
     * An abstract method that's implementations create the OS required CLI command to browse a {@linkplain File File}
     * @param file The {@linkplain File File} the command should browse
     * @return An array of {@linkplain String Strings} forming the command that can then be executed using a {@linkplain ProcessBuilder ProcessBuilder}
     */
    abstract String[] createFileBrowseCommand(File file);

    /**
     * An abstract method that's implementations convert a {@linkplain KeyEvent KeyEvent's} data to the Windows standards.
     * <br/>
     * This ensures that calling {@linkplain KeyEvent#isControlDown() isControlDown()} will actually check for pressing Cmd on {@linkplain #MAC MacOS} devices.
     * @param keyEvent The {@linkplain KeyEvent} to unify
     * @return The unified event
     */
    public abstract KeyEvent unifyKeyEvent(KeyEvent keyEvent);

    /**
     * An abstract method that's implementations convert a {@linkplain javafx.scene.input.KeyEvent KeyEvent's} data to the Windows standards.
     * <br/>
     * This ensures that calling {@linkplain javafx.scene.input.KeyEvent#isControlDown() isControlDown()} will actually check for pressing Cmd on {@linkplain #MAC MacOS} devices.
     * @param keyEvent The {@linkplain javafx.scene.input.KeyEvent KeyEvent} to unify
     * @return The unified event
     */
    public abstract javafx.scene.input.KeyEvent unifyKeyEvent(javafx.scene.input.KeyEvent keyEvent);

    /**
     * An abstract method that's implementations create a {@linkplain File} object for a folder with a given name. The folder will be located in the path the OS is typically using to save application configurations.
     * @param folderName The name of the folder.
     * @return The created {@linkplain File} object.
     */
    public abstract File createAppConfigDir(String folderName);
}
