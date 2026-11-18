
package xaos.ui.rendering;

import java.util.Objects;
import xaos.ui.components.UiScrollBar;
import xaos.ui.layout.UiRect;

/**
 * Renders a vertical scrollbar.
 *
 * Uses backend-independent drawing operations.
 * Themes supply the thumb colour.
 */
public final class UiScrollBarPainter {

    private UiScrollBarPainter() {}

    public static void paint(
            UiScrollBar bar,
            UiRenderer renderer,
            UiColor thumbColor) {

        Objects.requireNonNull(bar, "bar");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(thumbColor, "thumbColor");

        UiRect track = bar.bounds();

        if (!bar.visible()
                || track.width() == 0
                || track.height() == 0) {

            return;
        }

        // Dark track behind the thumb.
        renderer.fillRect(
                track,
                UiColor.rgba(20, 20, 20, 180));

        // Thumb indicates scroll position.
        renderer.fillRect(
                bar.thumbBounds(),
                thumbColor);
    }
}
