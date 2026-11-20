
package xaos.ui.core;

import java.util.Objects;
import xaos.ui.components.UiWindow;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;

/**
 * Viewport-aware positioning for floating UI windows.
 *
 * Keeps normal windows fully visible and ensures
 * oversized windows retain an accessible title bar
 * wherever the viewport permits.
 */
public final class UiWindowPlacement {

    private UiWindowPlacement() {
    }

    /**
     * Adjusts a window's requested position to keep it
     * accessible within the supplied viewport.
     *
     * The window must already have been laid out.
     *
     * @return true if its position changed
     */
    public static boolean constrainToViewport(
            UiWindow window,
            UiSize viewport) {

        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(viewport, "viewport");

        if (!window.isOpen()
                || viewport.width() == 0
                || viewport.height() == 0) {
            return false;
        }

        UiRect bounds = window.bounds();

        if (bounds.width() == 0 || bounds.height() == 0) {
            return false;
        }

        // --------------------------------------------------------
        // Horizontal boundaries
        // --------------------------------------------------------

        int minX;
        int maxX;

        if (bounds.width() <= viewport.width()) {
            // Keep the entire window visible.
            minX = 0;
            maxX = viewport.width() - bounds.width();
        } else {
            // Oversized windows may extend beyond the viewport.
            minX = viewport.width() - bounds.width();
            maxX = 0;
        }

        // --------------------------------------------------------
        // Vertical boundaries
        // --------------------------------------------------------

        int maxY;

        if (bounds.height() <= viewport.height()) {
            maxY = viewport.height() - bounds.height();
        } else {
            // Keep the complete title bar visible where possible.
            UiRect title = window.titleBar().bounds();

            int titleOffset = Math.max(
                    0, title.y() - bounds.y());

            long availableY = (long) viewport.height()
                    - titleOffset
                    - title.height();

            maxY = (int) Math.max(0L, availableY);
        }

        // --------------------------------------------------------
        // Apply position
        // --------------------------------------------------------

        UiPoint current = window.position();

        UiPoint adjusted = new UiPoint(
                clamp(current.x(), minX, maxX),
                clamp(current.y(), 0, maxY));

        if (current.equals(adjusted)) {
            return false;
        }

        window.setPosition(adjusted);
        return true;
    }

    /**
     * Centres a window within the current viewport.
     *
     * If the window is larger than the viewport,
     * its position is clamped to the top-left so
     * its title bar remains accessible.
     *
     * @return true if the window moved
     */
    public static boolean centerInViewport(
            UiWindow window,
            UiSize viewport) {

        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(viewport, "viewport");

        if (!window.isOpen()
                || viewport.width() == 0
                || viewport.height() == 0) {
            return false;
        }

        UiRect bounds = window.bounds();

        if (bounds.width() == 0
                || bounds.height() == 0) {
            return false;
        }

        int x = Math.max(
                0,
                (viewport.width() - bounds.width()) / 2);

        int y = Math.max(
                0,
                (viewport.height() - bounds.height()) / 2);

        UiPoint position = new UiPoint(x, y);

        if (window.position().equals(position)) {
            return false;
        }

        window.setPosition(position);
        return true;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
