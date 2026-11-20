
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Connects a cycle selector to its external
 * display value and advancement action.
 */
public record UiCycleBinding(
        Supplier<String> value,
        Runnable action) {

    public UiCycleBinding {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(action, "action");
    }
}
