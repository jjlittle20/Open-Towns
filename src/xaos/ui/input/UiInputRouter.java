
package xaos.ui.input;

import java.util.List;
import java.util.Objects;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiScrollBar;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiRoot;
import xaos.ui.core.UiWindowPlacement;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;

/**
 * Routes mouse interactions through the UI component tree.
 *
 * Frontmost elements receive input first.
 *
 * This class is independent of LWJGL and the game engine.
 * Coordinates use logical UI units.
 */
public final class UiInputRouter {

    private final UiRoot root;
    private final UiFocusManager focusManager;

    private UiElement pressedTarget;
    private UiButton hoveredButton;
    // ------------------------------------------------------------
    // Window dragging
    // ------------------------------------------------------------

    private UiWindow draggingWindow;

    private int dragOffsetX;
    private int dragOffsetY;
    // ------------------------------------------------------------
    // Window resizing
    // ------------------------------------------------------------

    private UiWindow resizingWindow;

    private int resizeStartX;
    private int resizeStartY;

    private int resizeStartWidth;
    private int resizeStartHeight;
    // ------------------------------------------------------------
    // Scrollbar dragging
    // ------------------------------------------------------------

    private UiScrollBar draggingScrollBar;
    private int scrollThumbGrabY;

    public UiInputRouter(UiRoot root) {

        this.root = Objects.requireNonNull(
                root, "root");

        this.focusManager = new UiFocusManager(root);
    }

    /**
     * Returns the keyboard focus manager
     * associated with this input router.
     */
    public UiFocusManager focusManager() {
        return focusManager;
    }

    // ------------------------------------------------------------
    // Mouse movement
    // ------------------------------------------------------------

    /**
     * Updates hover and pressed states.
     *
     * Returns true if the pointer is over an interactive
     * UI region or an existing UI press is being captured.
     */

    public boolean mouseMoved(int x, int y) {

        // Continue dragging a captured scrollbar thumb.
        if (draggingScrollBar != null) {

            draggingScrollBar.scrollToPointer(
                    y,
                    scrollThumbGrabY);

            root.updateLayout(root.viewportSize());

            updateHovered(null);
            return true;
        }

        // Continue a captured resize operation.
        if (resizingWindow != null) {
            moveResizedWindow(x, y);
            updateHovered(null);
            return true;
        }

        // Continue a captured window drag.
        if (draggingWindow != null) {
            moveDraggedWindow(x, y);
            updateHovered(null);
            return true;
        }

        UiElement target = hitTest(x, y);

        updateHovered(target);

        if (pressedTarget instanceof UiButton button) {
            button.setPressed(target == button);
        }

        return target != null || pressedTarget != null;
    }

    // ------------------------------------------------------------
    // Mouse wheel
    // ------------------------------------------------------------

    /**
     * Routes wheel events to the deepest scrollable container
     * under the cursor.
     *
     * Positive wheel deltas scroll up.
     * Negative wheel deltas scroll down.
     *
     * Returns true when the event belongs to the new UI,
     * even if a scroll container has reached its limit.
     */
    public boolean mouseWheelMoved(
            int x, int y, int wheelDelta) {

        if (wheelDelta == 0) {
            return false;
        }

        // Do not let wheel events pass through while
        // a window is being dragged or resized.
        if (draggingWindow != null
                || resizingWindow != null
                || draggingScrollBar != null) {

            return true;
        }

        UiElement target = wheelTargetAt(
                root.content(), x, y);

        if (target == null) {
            return false;
        }

        // LWJGL wheels usually report 120 units per notch.
        // Support smaller nonzero deltas as well.
        long notches = Math.max(
                1L,
                Math.abs((long) wheelDelta) / 120L);

        int distance = (int) (Math.min(20L, notches) * 40L);

        int amount = wheelDelta > 0
                ? -distance
                : distance;

        // Try the closest scroll container first.
        //
        // If it cannot scroll further, continue upwards
        // to see whether an enclosing container can.
        for (UiElement current = target; current != null; current = current.parent()) {

            if (current instanceof UiScrollContainer scroll
                    && scroll.scrollBy(amount)) {

                // Update the content positions immediately.
                root.updateLayout(root.viewportSize());

                return true;
            }
        }

        // The event occurred over the new UI.
        // Never pass it through to the game world,
        // even if nothing could scroll.
        return true;
    }

    /**
     * Finds the frontmost element relevant to wheel input.
     *
     * Unlike button hit testing, this must recognise
     * scroll containers even when the cursor is over
     * an empty part of their viewport.
     *
     * Windows and buttons prevent scrolling through
     * overlapping UI into elements behind them.
     */
    private UiElement wheelTargetAt(
            UiElement element,
            int x,
            int y) {

        if (!element.visible()
                || !element.bounds().contains(x, y)) {
            return null;
        }

        if (element instanceof UiContainer container) {

            List<UiElement> children = container.children();

            // Search front to back.
            for (int i = children.size() - 1; i >= 0; i--) {

                UiElement target = wheelTargetAt(
                        children.get(i), x, y);

                if (target != null) {
                    return target;
                }
            }
        }

        // Scroll containers accept wheel input across
        // their entire viewport, including empty space.
        //
        // Buttons and windows block elements behind them.
        if (element instanceof UiScrollContainer
                || element instanceof UiButton
                || element instanceof UiWindow) {

            return element;
        }

        return null;
    }

    // ------------------------------------------------------------
    // Mouse press
    // ------------------------------------------------------------

    /**
     * Begins a mouse interaction.
     *
     * Returns true if the UI consumed the press.
     */

    public boolean mousePressed(int x, int y) {
        clearPressed();

        UiElement target = hitTest(x, y);

        pressedTarget = target;
        updateHovered(target);

        // ------------------------------------------------------------
        // Keyboard focus
        // ------------------------------------------------------------

        // Clicking a focusable control gives it keyboard focus.
        // Clicking a non-focusable region clears the previous focus.
        if (focusManager.canFocus(target)) {
            focusManager.requestFocus(target);
        } else {
            focusManager.clearFocus();
        }

        UiWindow window = findOwningWindow(target);

        boolean floatingWindow = window != null
                && window.parent() != null
                && window.parent().layoutStrategy() == UiAbsoluteLayout.INSTANCE;

        // Raise the owning window before handling the press.
        if (floatingWindow) {
            window.parent().bringToFront(window);
        }

        if (target instanceof UiScrollBar bar) {

            // --------------------------------------------------------
            // Scrollbar interaction
            // --------------------------------------------------------

            draggingScrollBar = bar;

            UiRect thumb = bar.thumbBounds();

            boolean onThumb = thumb.contains(x, y);

            // Preserve the grab position when clicking the thumb.
            // Clicking elsewhere on the track centres the thumb
            // around the pointer and begins dragging.
            scrollThumbGrabY = onThumb
                    ? y - thumb.y()
                    : thumb.height() / 2;

            if (!onThumb) {
                bar.scrollToPointer(y, scrollThumbGrabY);
                root.updateLayout(root.viewportSize());
            }

        } else if (floatingWindow) {

            if (target == window
                    && window.resizable()
                    && window.resizeHandleBounds().contains(x, y)) {

                // Existing resize behaviour.
                resizingWindow = window;

                resizeStartX = x;
                resizeStartY = y;

                resizeStartWidth = window.bounds().width();
                resizeStartHeight = window.bounds().height();

            } else if (target == window
                    && window.titleBar().bounds().contains(x, y)) {

                // Existing title-bar drag behaviour.
                draggingWindow = window;

                dragOffsetX = Math.subtractExact(
                        x, window.position().x());

                dragOffsetY = Math.subtractExact(
                        y, window.position().y());
            }
        }

        if (target instanceof UiButton button) {
            button.setPressed(true);
        }

        return target != null;
    }

    // ------------------------------------------------------------
    // Mouse release
    // ------------------------------------------------------------

    /**
     * Completes a mouse interaction.
     *
     * A button activates only if the press began on that
     * button and the release occurs over the same button.
     *
     * Returns true if the UI handled either end of
     * the interaction.
     */
    public boolean mouseReleased(int x, int y) {
        UiElement target = hitTest(x, y);
        UiElement originalTarget = pressedTarget;

        pressedTarget = null;

        // Finish an active scrollbar drag.
        if (draggingScrollBar != null) {

            draggingScrollBar.scrollToPointer(
                    y,
                    scrollThumbGrabY);

            root.updateLayout(root.viewportSize());

            clearPressed();

            updateHovered(hitTest(x, y));

            return true;
        }

        // Finish an active resize operation.
        if (resizingWindow != null) {

            moveResizedWindow(x, y);

            clearPressed();

            updateHovered(hitTest(x, y));

            return true;
        }

        // Finish an active window drag before processing buttons.
        if (draggingWindow != null) {

            moveDraggedWindow(x, y);

            clearPressed();

            updateHovered(hitTest(x, y));

            return true;
        }

        if (originalTarget instanceof UiButton button) {
            button.setPressed(false);
        }

        updateHovered(target);

        if (originalTarget instanceof UiButton button
                && target == button
                && button.enabled()) {

            button.activate();

            // The action may have hidden or removed a component.
            // Refresh hover state against the updated UI tree.
            updateHovered(hitTest(x, y));
        }

        return originalTarget != null || target != null;
    }

    // ------------------------------------------------------------
    // Cancellation
    // ------------------------------------------------------------

    /**
     * Cancels the current pointer interaction.
     *
     * Useful when the game loses focus or changes screens.
     */
    public void cancelPointer() {
        clearPressed();

        if (hoveredButton != null) {
            hoveredButton.setHovered(false);
            hoveredButton = null;
        }
    }

    private void clearPressed() {

        if (pressedTarget instanceof UiButton button) {
            button.setPressed(false);
        }

        pressedTarget = null;

        draggingWindow = null;
        resizingWindow = null;
        draggingScrollBar = null;
        scrollThumbGrabY = 0;
    }

    // ------------------------------------------------------------
    // Hover management
    // ------------------------------------------------------------

    private void updateHovered(UiElement target) {
        UiButton nextButton = target instanceof UiButton button
                ? button
                : null;

        if (hoveredButton != nextButton) {
            if (hoveredButton != null) {
                hoveredButton.setHovered(false);
            }

            hoveredButton = nextButton;
        }

        if (hoveredButton != null) {
            hoveredButton.setHovered(true);
        }
    }

    /**
     * Finds the window containing an element.
     *
     * Returns null for controls that are not inside a window.
     */
    private UiWindow findOwningWindow(UiElement element) {

        UiElement current = element;

        while (current != null) {

            if (current instanceof UiWindow window) {
                return window;
            }

            current = current.parent();
        }

        return null;
    }

    /**
     * Moves a dragged window while keeping it
     * accessible within the viewport.
     */
    private void moveDraggedWindow(int x, int y) {

        if (draggingWindow == null) {
            return;
        }

        int newX = Math.subtractExact(x, dragOffsetX);
        int newY = Math.subtractExact(y, dragOffsetY);

        draggingWindow.setPosition(
                new UiPoint(newX, newY));

        // Prevent dragging the window outside
        // the accessible viewport area.
        UiWindowPlacement.constrainToViewport(
                draggingWindow,
                root.viewportSize());

        root.updateLayout(root.viewportSize());
    }

    /**
     * Updates the size of the window currently being resized.
     *
     * Enforces the window's minimum dimensions and prevents
     * resizing beyond the viewport where possible.
     */
    private void moveResizedWindow(int x, int y) {

        if (resizingWindow == null) {
            return;
        }

        UiWindow window = resizingWindow;

        if (!window.isOpen() || !window.resizable()) {
            return;
        }

        UiSize viewport = root.viewportSize();
        UiPoint position = window.position();
        UiSize minimum = window.minimumSize();

        // Use long arithmetic to avoid overflow from
        // unusually large pointer coordinates.
        long requestedWidth = (long) resizeStartWidth
                + ((long) x - resizeStartX);

        long requestedHeight = (long) resizeStartHeight
                + ((long) y - resizeStartY);

        // Space between the window's position and the
        // right/bottom edges of the viewport.
        long availableWidth = (long) viewport.width() - position.x();

        long availableHeight = (long) viewport.height() - position.y();

        int width = clampDimension(
                requestedWidth,
                minimum.width(),
                availableWidth);

        int height = clampDimension(
                requestedHeight,
                minimum.height(),
                availableHeight);

        window.setUserSize(new UiSize(width, height));

        // Recalculate bounds immediately for rendering
        // and subsequent mouse hit testing.
        root.updateLayout(viewport);
    }

    /**
     * Keeps a requested dimension between its minimum
     * and the available viewport space.
     *
     * Minimum size wins when the viewport is too small.
     */
    private static int clampDimension(
            long requested,
            int minimum,
            long available) {

        long maximum = Math.max(
                (long) minimum, available);

        long constrained = Math.max(
                (long) minimum,
                Math.min(requested, maximum));

        return (int) Math.min(
                Integer.MAX_VALUE, constrained);
    }

    // ------------------------------------------------------------
    // Hit testing
    // ------------------------------------------------------------

    private UiElement hitTest(int x, int y) {
        return hitTest(root.content(), x, y);
    }

    /**
     * Finds the frontmost interactive element at a position.
     */
    private UiElement hitTest(
            UiElement element,
            int x,
            int y) {

        if (!element.visible()
                || !element.bounds().contains(x, y)) {
            return null;
        }

        // The window resize corner takes priority over
        // child controls, including scrollbars.
        if (element instanceof UiWindow window
                && window.resizable()
                && window.resizeHandleBounds().contains(x, y)) {

            return window;
        }

        // Search children in reverse drawing order.
        if (element instanceof UiContainer container) {
            List<UiElement> children = container.children();

            for (int i = children.size() - 1; i >= 0; i--) {
                UiElement target = hitTest(
                        children.get(i), x, y);

                if (target != null) {
                    return target;
                }
            }
        }

        // Buttons receive direct interaction.
        if (element instanceof UiButton
                || element instanceof UiScrollBar) {

            return element;
        }

        // A window blocks clicks from reaching
        // windows and game elements behind it.
        if (element instanceof UiWindow) {
            return element;
        }

        // Ordinary containers and labels do not
        // intercept mouse input themselves.
        return null;
    }
}
