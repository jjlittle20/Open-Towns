
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntFunction;

/**
 * Connects a numeric slider to an external setting.
 *
 * onChange updates the live value.
 * onCommit persists the completed change.
 *
 * Does not depend on rendering, input or game logic.
 */

public record UiSliderBinding(
        int minimum,
        int maximum,
        int step,
        IntSupplier value,
        IntConsumer onChange,
        Runnable onCommit,
        IntFunction<String> formatter) {

    /**
     * Preserves the existing six-argument constructor.
     *
     * Settings without special formatting simply
     * display their numeric value.
     */
    public UiSliderBinding(
            int minimum,
            int maximum,
            int step,
            IntSupplier value,
            IntConsumer onChange,
            Runnable onCommit) {

        this(
                minimum,
                maximum,
                step,
                value,
                onChange,
                onCommit,
                Integer::toString);
    }

    public UiSliderBinding {

        if (minimum >= maximum) {
            throw new IllegalArgumentException(
                    "Slider minimum must be below maximum");
        }

        if (step <= 0) {
            throw new IllegalArgumentException(
                    "Slider step must be positive");
        }

        if ((maximum - minimum) % step != 0) {
            throw new IllegalArgumentException(
                    "Slider range must be divisible by step");
        }

        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(onChange, "onChange");
        Objects.requireNonNull(onCommit, "onCommit");
        Objects.requireNonNull(formatter, "formatter");
    }

    public int currentValue() {
        return value.getAsInt();
    }

    public int setValue(int requested) {

        int clamped = Math.max(
                minimum,
                Math.min(maximum, requested));

        int steps = Math.round(
                (clamped - minimum) / (float) step);

        int snapped = minimum + steps * step;

        if (snapped != currentValue()) {
            onChange.accept(snapped);
        }

        return currentValue();
    }

    public String formattedValue() {
        return Objects.requireNonNull(
                formatter.apply(currentValue()),
                "Slider formatted value");
    }

    public void commit() {
        onCommit.run();
    }
}
