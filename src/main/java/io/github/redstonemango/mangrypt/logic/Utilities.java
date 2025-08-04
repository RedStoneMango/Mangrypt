package io.github.redstonemango.mangrypt.logic;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Utilities {

    public static final StackWalker WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static void ensureAuthorizedAccess(Class<?>... authorizedClasses) throws SecurityException {
        String methodName = WALKER.walk(frames ->
                frames.skip(1).findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse("unknownMethod"));

        List<Class<?>> classes = Arrays.asList(authorizedClasses);

        boolean trustedCaller = WALKER.walk(frames ->
                frames.skip(1).anyMatch(frame -> {
                    Class<?> caller = frame.getDeclaringClass();
                    if (!classes.contains(caller)) {
                        return false;
                    }
                    int index = classes.indexOf(caller);
                    return classes.get(index).getClassLoader().equals(caller.getClassLoader());
                })
        );

        if (!trustedCaller) {
            throw new SecurityException("Unauthorized access to method '" + methodName + "'");
        }
    }

    public static <T> void applyCustomNodeCellFactory(ListView<T> listView, Function<T, Node> nodeFunction) {
        applyCustomNodeCellFactory(listView, nodeFunction, _ -> {});
    }

    public static <T> void applyCustomNodeCellFactory(ListView<T> listView, Function<T, Node> nodeFunction, Consumer<T> onDoubleClick) {
        listView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<T> call(ListView<T> lv) {
                return new ListCell<>() {

                    private long lastClick = -1;

                    @Override
                    protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setGraphic(nodeFunction.apply(item));
                            setPadding(new Insets(0));
                            setOnMouseClicked(_ -> {
                                if (System.currentTimeMillis() - lastClick <= 250) onDoubleClick.accept(getItem());
                                lastClick = System.currentTimeMillis();
                            });
                        }
                    }
                };
            }
        });
    }

}
