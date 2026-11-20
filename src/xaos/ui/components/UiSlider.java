
package xaos.ui.components;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

/**
 * A horizontal slider for an externally owned
 * integer setting.
 *
 * Supports:
 * - Configurable minimum, maximum and step
 * - Click and drag
 * - Live value updates
 * - Commit when interaction finishes
 * - External state refresh
 * - Keyboard activation through UiButton
 *
 * Contains no game-specific code.
 */
public final class UiSlider extends UiButton {

    private final String label;

    private final int minimum;
    private final int maximum;
    private final int step;

    private final IntSupplier value;
    private final IntConsumer onChange;
    private final Runnable onCommit;
    private final IntFunction<String> formatter;

    private boolean dragging;
    private boolean changedDuringDrag;

    public UiSlider(
            String label,
            UiTextMetrics textMetrics,
            int minimum,
            int maximum,
            int step,
            IntSupplier value,
            IntConsumer onChange,
            Runnable onCommit,
            IntFunction<String> formatter) {

        super(
                Objects.requireNonNull(label, "label"),
                textMetrics);

        if (minimum >= maximum
                || step <= 0
                || (maximum - minimum) % step != 0) {

            throw new IllegalArgumentException(
                    "Invalid slider range or step");
        }

        this.label = label;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;

        this.value = Objects.requireNonNull(value);
        this.onChange = Objects.requireNonNull(onChange);
        this.onCommit = Objects.requireNonNull(onCommit);
        this.formatter = Objects.requireNonNull(formatter);

        setMinimumSize(new UiSize(160, 48));
        setPreferredSize(new UiSize(300, 52));

        // Keyboard Enter/Space activation advances
        // by one step. Mouse dragging is handled
        // separately by UiInputRouter.
        setOnClick(this::stepForward);

        refresh();
    }

    public String label() {
        return label;
    }

    public int minimum() {
        return minimum;
    }

    public int maximum() {
        return maximum;
    }

    public int step() {
        return step;
    }

    public int currentValue() {
        return value.getAsInt();
    }

    public boolean dragging() {
        return dragging;
    }

    /**
     * Updates the displayed value from external state.
     */
    public void refresh() {
        setText(
                label + ": "
                        + formatter.apply(currentValue()));
    }

    // --------------------------------------------------------
    // Geometry
    // --------------------------------------------------------

    public UiRect trackBounds() {

        UiRect b = bounds();

        int padding = 16;

        return new UiRect(
                b.x() + padding,
                b.y() + b.height() - 15,
                Math.max(1, b.width() - padding * 2),
                6);
    }

    public UiRect thumbBounds() {

        UiRect track = trackBounds();

        int range = maximum - minimum;

        int current = Math.max(
                minimum,
                Math.min(maximum, currentValue()));

        int travel = Math.max(0, track.width() - 1);

        int offset = (int) Math.round(
                (current - minimum)
                        * (double) travel / range);

        int centreX = track.x() + offset;

        return new UiRect(
                centreX - 5,
                track.y() - 4,
                11,
                14);
    }

    // --------------------------------------------------------
    // Pointer interaction
    // --------------------------------------------------------

    public void beginDrag(int mouseX) {

        if (!enabled() || !visible()) {
            return;
        }

        dragging = true;
        changedDuringDrag = false;

        updateFromMouse(mouseX);
    }

    public void dragTo(int mouseX) {

        if (dragging) {
            updateFromMouse(mouseX);
        }
    }

    public void endDrag(int mouseX) {

        if (!dragging) {
            return;
        }

        updateFromMouse(mouseX);

        finishDrag();
    }

    /**
     * Cancels capture but preserves and commits any
     * live value change already made.
     *
     * Important when the player Alt+Tabs away.
     */
    public void cancelDrag() {
        finishDrag();
    }

    private void finishDrag() {

        if (!dragging) {
            return;
        }

        dragging = false;

        if (changedDuringDrag) {
            changedDuringDrag = false;
            onCommit.run();
        }
    }

    private void updateFromMouse(int mouseX) {

        UiRect track = trackBounds();

        int travel = Math.max(1, track.width() - 1);

        long offset = Math.max(
                0L,
                Math.min(
                        (long) travel,
                        (long) mouseX - track.x()));

        int totalSteps = (maximum - minimum) / step;

        int selectedStep = (int) Math.round(
                offset * (double) totalSteps / travel);

        setRequestedValue(
                minimum + selectedStep * step);
    }

    // --------------------------------------------------------
    // Value adjustment
    // --------------------------------------------------------

    public void stepForward() {
        adjustBySteps(1);
    }

    public void stepBackward() {
        adjustBySteps(-1);
    }

    public void adjustBySteps(int steps) {

        if (!enabled()) {
            return;
        }

        int previous = currentValue();

        long requested = (long) previous
                + (long) steps * step;

        setRequestedValue(
                (int) Math.max(
                        minimum,
                        Math.min(maximum, requested)));

        if (previous != currentValue()) {
            onCommit.run();
        }
    }

    private void setRequestedValue(int requested) {

        int clamped = Math.max(
                minimum,
                Math.min(maximum, requested));

        int snappedSteps = Math.round(
                (clamped - minimum) / (float) step);

        int snapped = minimum + snappedSteps * step;

        if (snapped != currentValue()) {

            onChange.accept(snapped);

            if (dragging) {
                changedDuringDrag = true;
            }
        }

        refresh();
    }
}
