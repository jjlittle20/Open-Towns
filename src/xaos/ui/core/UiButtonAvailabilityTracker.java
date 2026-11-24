
package xaos.ui.core;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import xaos.ui.components.UiButton;

/**
 * Refreshes button availability without rebuilding
 * or replacing UI components.
 *
 * The caller determines what "available" means.
 *
 * Designed for use on the UI/game thread.
 */
public final class UiButtonAvailabilityTracker {

    private static final class Entry {

        final BooleanSupplier check;
        final Consumer<RuntimeException> onError;

        boolean errorReported;

        Entry(
                BooleanSupplier check,
                Consumer<RuntimeException> onError) {

            this.check = check;
            this.onError = onError;
        }
    }

    private final Map<UiButton, Entry> entries = new IdentityHashMap<>();

    /**
     * Registers a button and checks its initial state.
     */
    public void track(
            UiButton button,
            BooleanSupplier check,
            Consumer<RuntimeException> onError) {

        Objects.requireNonNull(button, "button");
        Objects.requireNonNull(check, "check");
        Objects.requireNonNull(onError, "onError");

        Entry entry = new Entry(check, onError);

        entries.put(button, entry);

        update(button, entry);
    }

    /**
     * Rechecks every registered button.
     *
     * Returns how many buttons changed state.
     */
    public int refresh() {

        int changed = 0;

        for (var tracked : entries.entrySet()) {

            if (update(
                    tracked.getKey(),
                    tracked.getValue())) {

                changed++;
            }
        }

        return changed;
    }

    /**
     * Removes registrations for an old menu.
     */
    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    private static boolean update(
            UiButton button,
            Entry entry) {

        boolean available;

        try {

            available = entry.check.getAsBoolean();

            // A successful check means a future
            // failure can be reported again.
            entry.errorReported = false;

        } catch (RuntimeException exception) {

            available = false;

            // Prevent an incorrectly implemented
            // mod check from flooding the error log
            // every frame.
            if (!entry.errorReported) {

                entry.errorReported = true;

                entry.onError.accept(exception);
            }
        }

        if (button.enabled() == available) {
            return false;
        }

        button.setEnabled(available);
        return true;
    }
}
