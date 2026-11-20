
package xaos.ui.components;

import java.util.Objects;
import java.util.function.Supplier;

import xaos.ui.rendering.UiTextMetrics;

/**
 * A button that advances an external setting
 * through a sequence of permitted values.
 *
 * The underlying game or mod owns the state,
 * command and wraparound behaviour.
 */
public final class UiCycleButton extends UiButton {

    private final String label;
    private final Supplier<String> value;
    private final Runnable onAdvance;

    public UiCycleButton(
            String label,
            UiTextMetrics textMetrics,
            Supplier<String> value,
            Runnable onAdvance) {

        super(
                Objects.requireNonNull(label, "label"),
                textMetrics);

        this.label = label;
        this.value = Objects.requireNonNull(
                value, "value");
        this.onAdvance = Objects.requireNonNull(
                onAdvance, "onAdvance");

        setOnClick(() -> {
            this.onAdvance.run();
            refresh();
        });

        refresh();
    }

    public String label() {
        return label;
    }

    /**
     * Reads the current setting value.
     */
    public String currentValue() {
        return Objects.requireNonNull(
                value.get(),
                "Cycle value cannot be null");
    }

    /**
     * Synchronises the visible text with
     * the current underlying setting.
     */
    public void refresh() {
        setText(label + ": " + currentValue());
    }
}
