
package xaos.ui.components;

import java.util.Objects;
import xaos.ui.core.UiElement;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

/**
 * A single-line text label that automatically sizes itself
 * using the supplied font metrics.
 *
 * Text rendering and styling are handled separately.
 */
public class UiLabel extends UiElement {

    private String text;
    private final UiTextMetrics textMetrics;

    // ------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------

    public UiLabel(String text, UiTextMetrics textMetrics) {
        this.text = Objects.requireNonNull(text, "text");

        this.textMetrics = Objects.requireNonNull(
                textMetrics, "textMetrics");
    }

    // ------------------------------------------------------------
    // Text
    // ------------------------------------------------------------

    public String text() {
        return text;
    }

    public void setText(String text) {
        String newText = Objects.requireNonNull(text, "text");

        if (!this.text.equals(newText)) {
            this.text = newText;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Measurement
    // ------------------------------------------------------------

    @Override
    public UiSize measure(UiSize available) {
        Objects.requireNonNull(available, "available");

        if (!visible()) {
            return UiSize.ZERO;
        }

        UiSize textSize = Objects.requireNonNull(
                textMetrics.measure(text),
                "textMetrics result");

        int width = Math.max(
                minimumSize().width(),
                Math.min(
                        Math.max(
                                preferredSize().width(),
                                textSize.width()),
                        available.width()));

        int height = Math.max(
                minimumSize().height(),
                Math.min(
                        Math.max(
                                preferredSize().height(),
                                textSize.height()),
                        available.height()));

        return new UiSize(width, height);
    }
}
