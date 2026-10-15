package xaos.internal.api.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UiActionRegistry {

    private static final Map<String, RegisteredAction> actions =
            new LinkedHashMap<>();

    private UiActionRegistry() {
    }

    public static void register(
            String id,
            String label,
            Runnable action
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Action ID cannot be empty"
            );
        }

        if (!id.contains(":")) {
            throw new IllegalArgumentException(
                    "Action ID must be namespaced, e.g. strip-mine:start"
            );
        }

        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException(
                    "Action label cannot be empty"
            );
        }

        if (action == null) {
            throw new IllegalArgumentException(
                    "Action callback cannot be null"
            );
        }

        actions.put(
                id,
                new RegisteredAction(
                        id,
                        label,
                        action
                )
        );
    }

    public static void unregister(String id) {
        if (id != null) {
            actions.remove(id);
        }
    }

    public static List<RegisteredAction> getActions() {
        return List.copyOf(actions.values());
    }

    public static boolean hasActions() {
        return !actions.isEmpty();
    }

    public static boolean execute(String id) {
        RegisteredAction action = actions.get(id);

        if (action == null) {
            return false;
        }

        action.callback().run();
        return true;
    }

    public record RegisteredAction(
            String id,
            String label,
            Runnable callback
    ) {
    }
}