
package xaos.ui.components;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import xaos.ui.rendering.UiTextMetrics;

/**
 * A button representing an external boolean setting.
 *
 * The setting is read from a supplied state provider.
 * Clicking executes the supplied action.
 *
 * The component does not store a separate copy
 * of the setting value.
 */
public final class UiToggleButton extends UiButton {

    private final String label;
    private final BooleanSupplier state;
    private final Runnable onToggle;

    public UiToggleButton(
            String label,
            UiTextMetrics textMetrics,
            BooleanSupplier state,
            Runnable onToggle) {

        super(
                Objects.requireNonNull(label, "label"),
                textMetrics);

        this.label = label;

        this.state = Objects.requireNonNull(
                state, "state");

        this.onToggle = Objects.requireNonNull(
                onToggle, "onToggle");

        setOnClick(() -> {
            this.onToggle.run();
            refresh();
        });

        refresh();
    }

    /**
     * Reads the actual current setting value.
     */
    public boolean checked() {
        return state.getAsBoolean();
    }

    /**
     * Synchronises the visible button with
     * the current underlying setting.
     *
     * This also handles changes made through
     * keyboard shortcuts or elsewhere in the game.
     */
    public void refresh() {

        boolean checked = checked();

        setText(
                label + ": " + (checked ? "ON" : "OFF"));

        setSelected(checked);
    }

    public String label() {
        return label;
    }
}
