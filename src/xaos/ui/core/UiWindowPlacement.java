
package xaos.ui.core;

import java.util.Objects;
import xaos.ui.components.UiWindow;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiWindowAnchor;
import xaos.ui.layout.UiWindowDockMode;

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
     * Calculates the temporary size of a docked window.
     *
     * Returns null when docking is inactive.
     *
     * The unstretched dimension retains the
     * original user or preferred size.
     */
    public static UiSize dockedSize(
            UiWindow window,
            UiSize viewport) {

        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(viewport, "viewport");

        if (!window.isOpen()
                || window.anchor() == UiWindowAnchor.NONE
                || window.dockMode() == UiWindowDockMode.NONE
                || viewport.width() == 0
                || viewport.height() == 0) {

            return null;
        }

        UiWindowAnchor anchor = window.anchor();
        UiWindowDockMode mode = window.dockMode();

        UiInsets insets = window.anchorInsets();

        // Prefer the player's explicit dimensions.
        // Otherwise use preferred or measured size.
        UiSize natural = window.userSize()
                .orElse(window.preferredSize());

        int width = natural.width() > 0
                ? natural.width()
                : window.bounds().width();

        int height = natural.height() > 0
                ? natural.height()
                : window.bounds().height();

        switch (mode) {

            case FILL_HEIGHT -> {

                if (anchor != UiWindowAnchor.LEFT
                        && anchor != UiWindowAnchor.RIGHT) {

                    throw new IllegalStateException(
                            "FILL_HEIGHT requires LEFT or RIGHT anchor");
                }

                int availableHeight = Math.max(
                        0,
                        viewport.height() - insets.vertical());

                return new UiSize(
                        Math.max(
                                window.minimumSize().width(),
                                width),

                        Math.max(
                                window.minimumSize().height(),
                                availableHeight));
            }

            case FILL_WIDTH -> {

                if (anchor != UiWindowAnchor.TOP
                        && anchor != UiWindowAnchor.BOTTOM) {

                    throw new IllegalStateException(
                            "FILL_WIDTH requires TOP or BOTTOM anchor");
                }

                int availableWidth = Math.max(
                        0,
                        viewport.width() - insets.horizontal());

                return new UiSize(
                        Math.max(
                                window.minimumSize().width(),
                                availableWidth),

                        Math.max(
                                window.minimumSize().height(),
                                height));
            }

            case NONE -> {
                return null;
            }
        }

        throw new IllegalStateException(
                "Unsupported docking mode: " + mode);
    }

    /**
     * Positions an anchored window relative to
     * the current logical viewport.
     *
     * The window must already have been laid out.
     *
     * Anchoring does not change the window's size.
     *
     * @return true if its position changed
     */
    public static boolean anchorInViewport(
            UiWindow window,
            UiSize viewport) {

        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(viewport, "viewport");

        UiWindowAnchor anchor = window.anchor();

        if (anchor == UiWindowAnchor.NONE
                || !window.isOpen()
                || viewport.width() == 0
                || viewport.height() == 0) {

            return false;
        }

        UiRect bounds = window.bounds();

        if (bounds.width() == 0
                || bounds.height() == 0) {

            return false;
        }

        UiInsets margin = window.anchorInsets();

        // --------------------------------------------------------
        // Horizontal positions
        // --------------------------------------------------------

        int left = margin.left();

        int right = viewport.width()
                - bounds.width()
                - margin.right();

        int centerX = (viewport.width()
                - bounds.width()) / 2;

        // --------------------------------------------------------
        // Vertical positions
        // --------------------------------------------------------

        int top = margin.top();

        int bottom = viewport.height()
                - bounds.height()
                - margin.bottom();

        int centerY = (viewport.height()
                - bounds.height()) / 2;

        // --------------------------------------------------------
        // Resolve the anchor
        // --------------------------------------------------------

        int x = switch (anchor) {

            case TOP_LEFT, BOTTOM_LEFT, LEFT ->
                left;

            case TOP_RIGHT, BOTTOM_RIGHT, RIGHT ->
                right;

            case TOP, BOTTOM ->
                centerX;

            case NONE ->
                window.position().x();
        };

        int y = switch (anchor) {

            case TOP_LEFT, TOP_RIGHT, TOP ->
                top;

            case BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM ->
                bottom;

            case LEFT, RIGHT ->
                centerY;

            case NONE ->
                window.position().y();
        };

        // A full-height sidebar starts at the top inset,
        // rather than being vertically centred.
        if (window.dockMode() == UiWindowDockMode.FILL_HEIGHT) {

            y = margin.top();
        }

        // A full-width toolbar starts at the left inset,
        // rather than being horizontally centred.
        if (window.dockMode() == UiWindowDockMode.FILL_WIDTH) {

            x = margin.left();
        }

        // --------------------------------------------------------
        // Keep the anchored window visible
        // --------------------------------------------------------

        UiPoint position = new UiPoint(
                clamp(
                        x,
                        0,
                        Math.max(
                                0,
                                viewport.width() - bounds.width())),

                clamp(
                        y,
                        0,
                        Math.max(
                                0,
                                viewport.height() - bounds.height())));

        if (window.position().equals(position)) {
            return false;
        }

        window.setPosition(position);

        return true;
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
