
package xaos.ui.rendering;

import java.util.Objects;
import xaos.ui.components.UiWindow;
import xaos.ui.layout.UiRect;

/**
 * Draws a small resize indicator in a window's
 * bottom-right corner.
 *
 * The indicator uses the active theme's colour.
 */
public final class UiResizeGripPainter {

    private UiResizeGripPainter() {
    }

    public static void paint(
            UiWindow window,
            UiRenderer renderer,
            UiColor color) {

        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(color, "color");

        if (!window.visible() || !window.resizable()) {
            return;
        }

        UiRect bounds = window.resizeHandleBounds();

        if (bounds.width() < 16 || bounds.height() < 16) {
            return;
        }

        int right = Math.addExact(
                bounds.x(), bounds.width());

        int bottom = Math.addExact(
                bounds.y(), bounds.height());

        // Six small squares form a diagonal resize grip.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column <= row; column++) {

                int x = right - 4 - column * 4;
                int y = bottom - 4 - (row - column) * 4;

                renderer.fillRect(
                        new UiRect(x, y, 2, 2),
                        color);
            }
        }
    }
}
