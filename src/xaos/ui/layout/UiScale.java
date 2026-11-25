
package xaos.ui.layout;

import java.util.Objects;

/**
 * Converts base UI dimensions using a scale
 * factor, without depending on the legacy
 * Towns UIScaler.
 *
 * The game integration layer supplies the
 * scale factor; reusable UI components do
 * not need to know where it came from.
 */
public record UiScale(float factor) {

    public static final UiScale DEFAULT = new UiScale(1.0f);

    public UiScale {

        if (!Float.isFinite(factor)
                || factor <= 0f) {

            throw new IllegalArgumentException(
                    "UI scale must be positive and finite");
        }
    }

    /**
     * Scales one coordinate or dimension,
     * rounding to the nearest integer.
     *
     * Negative values are supported for
     * positions and offsets.
     */
    public int pixels(int value) {

        long scaled = Math.round(
                (double) value * factor);

        return Math.toIntExact(scaled);
    }

    public UiSize size(UiSize size) {

        Objects.requireNonNull(size, "size");

        return new UiSize(
                pixels(size.width()),
                pixels(size.height()));
    }

    public UiPoint point(UiPoint point) {

        Objects.requireNonNull(point, "point");

        return new UiPoint(
                pixels(point.x()),
                pixels(point.y()));
    }

    public UiInsets insets(UiInsets insets) {

        Objects.requireNonNull(insets, "insets");

        return new UiInsets(
                pixels(insets.top()),
                pixels(insets.right()),
                pixels(insets.bottom()),
                pixels(insets.left()));
    }
}
