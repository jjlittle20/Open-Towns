
package xaos.ui.rendering;

import java.util.Objects;
import xaos.ui.core.UiElement;
import xaos.ui.layout.UiRect;

/**
 * Draws a keyboard focus outline.
 *
 * This painter does not change element sizes
 * or participate in layout.
 */
public final class UiFocusPainter {

    private UiFocusPainter() {}

    public static void paint(
            UiElement element,
            UiRenderer renderer,
            UiColor color) {

        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(color, "color");

        UiRect bounds = element.bounds();

        if (!element.visible()
                || bounds.width() < 8
                || bounds.height() < 8) {
            return;
        }

        int left = bounds.x() + 2;
        int top = bounds.y() + 2;

        int width = bounds.width() - 4;
        int height = bounds.height() - 4;

        renderer.fillRect(
                new UiRect(left, top, width, 2),
                color);

        renderer.fillRect(
                new UiRect(
                        left,
                        top + height - 2,
                        width,
                        2),
                color);

        renderer.fillRect(
                new UiRect(left, top, 2, height),
                color);

        renderer.fillRect(
                new UiRect(
                        left + width - 2,
                        top,
                        2,
                        height),
                color);
    }
}
