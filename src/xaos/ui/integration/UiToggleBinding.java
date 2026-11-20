
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Connects an XML toggle item to an external
 * setting and its existing action.
 *
 * Internal compatibility binding.
 */
public record UiToggleBinding(
        BooleanSupplier state,
        Runnable action) {

    public UiToggleBinding {

        Objects.requireNonNull(
                state, "state");

        Objects.requireNonNull(
                action, "action");
    }
}
