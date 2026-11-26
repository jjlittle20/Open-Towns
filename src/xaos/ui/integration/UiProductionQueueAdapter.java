
package xaos.ui.integration;

import java.util.Objects;

/**
 * Translates production-panel interactions into gameplay operations.
 *
 * The UI does not need to know how production queues are managed.
 */
public final class UiProductionQueueAdapter {

    public enum Action {
        ADD_REGULAR,
        REMOVE_REGULAR,
        INCREASE_AUTOMATED,
        DECREASE_AUTOMATED
    }

    /**
     * Implemented by the gameplay integration layer.
     */
    public interface Backend {

        void execute(Action action);

        void tutorial(Action action);
    }

    /**
     * Number of operations represented by one click.
     *
     * Control takes priority over Shift.
     */
    public static int repetitions(
            boolean shiftHeld,
            boolean controlHeld) {

        if (controlHeld) {
            return 100;
        }

        if (shiftHeld) {
            return 5;
        }

        return 1;
    }

    /**
     * Executes one production-panel interaction.
     */
    public void apply(
            Action action,
            boolean shiftHeld,
            boolean controlHeld,
            Backend backend) {

        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(backend, "backend");

        int count = repetitions(shiftHeld, controlHeld);

        // First operation.
        backend.execute(action);

        // Legacy tutorial notification happens once per click,
        // immediately after the first operation.
        backend.tutorial(action);

        // Additional operations for modifier keys.
        for (int i = 1; i < count; i++) {
            backend.execute(action);
        }
    }
}
