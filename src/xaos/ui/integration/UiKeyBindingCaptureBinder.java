
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.BiConsumer;

import xaos.ui.components.UiKeyBindingRow;
import xaos.ui.input.UiInputRouter;

/**
 * Connects a key-binding row to physical-key capture.
 *
 * The assignment callback receives:
 * slot: 0 = primary, 1 = secondary
 * keyCode: physical keyboard code
 *
 * The caller owns assignment, duplicate handling
 * and persistence.
 */
public final class UiKeyBindingCaptureBinder {

    private UiKeyBindingCaptureBinder() {
    }

    public static void bind(
            UiKeyBindingRow row,
            UiInputRouter input,
            int escapeKeyCode,
            BiConsumer<Integer, Integer> assignKey,
            Runnable onBindingsChanged) {

        Objects.requireNonNull(row, "row");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(assignKey, "assignKey");
        Objects.requireNonNull(
                onBindingsChanged, "onBindingsChanged");

        row.setOnCaptureRequested(slot -> {

            // The input router consumes physical key
            // presses before normal UI and game actions.
            input.beginPhysicalKeyCapture(
                    escapeKeyCode,

                    // Capture succeeded.
                    keyCode -> {
                        try {
                            assignKey.accept(slot, keyCode);
                        } finally {
                            row.endCapture();

                            // Other rows may have changed
                            // because of duplicate handling.
                            onBindingsChanged.run();
                        }
                    },

                    // Escape, clicking elsewhere or
                    // closing the UI cancels capture.
                    row::endCapture);

            // Only show the prompt after the input
            // router successfully begins capture.
            row.beginCapture(slot);
        });
    }
}
