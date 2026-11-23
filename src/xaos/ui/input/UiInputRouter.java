
package xaos.ui.input;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

import xaos.ui.components.UiButton;
import xaos.ui.components.UiScrollBar;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiSlider;
import xaos.ui.components.UiTextField;
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
    private int pointerX;
    private int pointerY;
    private final UiRoot root;
    private final UiFocusManager focusManager;
    // Slider currently capturing mouse movement.
    private UiSlider activeSlider;
    /**
     * Keyboard navigation is enabled after clicking
     * inside the new UI and disabled when clicking outside.
     */
    private boolean keyboardNavigationActive;

    // ------------------------------------------------------------
    // Physical keyboard capture
    // ------------------------------------------------------------

    private IntConsumer physicalKeyReceiver;
    private Runnable physicalKeyCancel;

    private int captureEscapeKey = -1;

    // If capture was requested using Enter/Space,
    // wait for that initiating key to be released.
    private boolean awaitingActivationRelease;
    private boolean activatingButtonFromKeyboard;
    private UiKey activatingNavigationKey;
    private UiKey captureActivationKey;

    // Prevent the captured key's repeat/release events
    // from reaching normal UI or gameplay handlers.
    private int swallowedPhysicalKey = -1;

    /**
     * Tracks held navigation keys so GLFW key-repeat
     * events cannot repeatedly activate buttons.
     */
    private final EnumSet<UiKey> heldKeys = EnumSet.noneOf(UiKey.class);

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

    /**
     * Begins capturing the next physical key press.
     *
     * The UI layer remains independent of the platform:
     * the caller supplies the physical Escape key code.
     */
    public void beginPhysicalKeyCapture(
            int escapeKeyCode,
            IntConsumer onCaptured,
            Runnable onCancelled) {

        Objects.requireNonNull(onCaptured, "onCaptured");
        Objects.requireNonNull(onCancelled, "onCancelled");

        if (physicalKeyReceiver != null) {
            throw new IllegalStateException(
                    "A key capture is already active");
        }

        physicalKeyReceiver = onCaptured;
        physicalKeyCancel = onCancelled;

        captureEscapeKey = escapeKeyCode;

        captureActivationKey = activatingButtonFromKeyboard
                ? activatingNavigationKey
                : null;

        awaitingActivationRelease = captureActivationKey != null;
    }

    public boolean isCapturingPhysicalKey() {
        return physicalKeyReceiver != null;
    }

    /**
     * Handles raw physical keyboard events.
     *
     * Must be called before normal UI navigation
     * and gameplay shortcut processing.
     *
     * @return true if the event was consumed
     */
    public boolean physicalKeyEvent(
            int keyCode,
            boolean pressed) {

        // The key that completed capture must remain
        // consumed until the player releases it.
        if (keyCode == swallowedPhysicalKey) {

            if (!pressed) {
                swallowedPhysicalKey = -1;
            }

            return true;
        }

        if (!isCapturingPhysicalKey()) {
            return false;
        }

        if (awaitingActivationRelease) {
            return true;
        }

        // While capturing, consume key releases
        // and ignore unknown key codes.
        if (!pressed || keyCode <= 0) {
            return true;
        }

        IntConsumer receiver = physicalKeyReceiver;
        Runnable cancellation = physicalKeyCancel;

        boolean cancelled = keyCode == captureEscapeKey;

        // Clear capture before invoking callbacks.
        // Callbacks may open windows or start another capture.
        clearPhysicalKeyCapture();

        swallowedPhysicalKey = keyCode;

        if (cancelled) {
            cancellation.run();
        } else {
            receiver.accept(keyCode);
        }

        return true;
    }

    /**
     * Cancels capture without changing any assignment.
     */
    public void cancelPhysicalKeyCapture() {

        if (!isCapturingPhysicalKey()) {
            return;
        }

        Runnable cancellation = physicalKeyCancel;

        clearPhysicalKeyCapture();

        cancellation.run();
    }

    private void clearPhysicalKeyCapture() {

        physicalKeyReceiver = null;
        physicalKeyCancel = null;
        captureActivationKey = null;
        captureEscapeKey = -1;
        awaitingActivationRelease = false;
    }

    /**
     * Enables keyboard navigation when a modal window
     * is opened programmatically.
     *
     * The first Tab press will focus the first
     * available control without requiring a mouse click.
     *
     * This does not automatically focus a text field.
     */
    public void activateModalKeyboardNavigation(
            UiWindow window) {

        Objects.requireNonNull(window, "window");

        if (!window.modal() || !window.isOpen()) {
            throw new IllegalArgumentException(
                    "Window must be an open modal");
        }

        focusManager.activateScopeFor(window);

        // The window must belong to this UI root.
        if (focusManager.activeWindow() != window) {
            throw new IllegalArgumentException(
                    "Modal window is not attached to this UI root");
        }

        keyboardNavigationActive = true;
    }

    /**
     * Returns true when the new UI owns text input.
     *
     * Only an enabled, focused text field can
     * receive typed characters.
     */
    public boolean textInputActive() {
        return keyboardNavigationActive
                && focusManager.focusedElement() instanceof UiTextField;
    }

    /**
     * Inserts a Unicode character into the focused field.
     *
     * Returns true if the event belongs to the field,
     * even when the character is rejected.
     */
    public boolean characterTyped(int codePoint) {

        if (!textInputActive()) {
            return false;
        }

        // GLFW should deliver Unicode scalar values,
        // but validate them before constructing a String.
        if (!Character.isValidCodePoint(codePoint)
                || (codePoint >= Character.MIN_SURROGATE
                        && codePoint <= Character.MAX_SURROGATE)
                || Character.isISOControl(codePoint)) {

            return true;
        }

        UiTextField field = (UiTextField) focusManager.focusedElement();

        field.insertText(
                new String(Character.toChars(codePoint)));

        return true;
    }

    // ------------------------------------------------------------
    // Keyboard input
    // ------------------------------------------------------------

    /**
     * Handles keyboard navigation and button activation.
     *
     * Returns true when the event is consumed by the
     * new UI and should not reach the original game.
     */

    public boolean keyPressed(
            UiKey key,
            boolean shiftDown) {

        Objects.requireNonNull(key, "key");
        if (isCapturingPhysicalKey()) {
            return true;
        }

        if (!keyboardNavigationActive) {
            return false;
        }

        // Editing keys may repeat while held.
        // Tab and button activation must not.
        boolean repeatable = switch (key) {
            case BACKSPACE, DELETE, LEFT, RIGHT -> true;
            default -> false;
        };

        if (heldKeys.contains(key) && !repeatable) {
            return true;
        }

        boolean consumed = false;

        // --------------------------------------------------------
        // Focus navigation
        // --------------------------------------------------------

        if (key == UiKey.TAB) {

            if (shiftDown) {
                focusManager.focusPrevious();
            } else {
                focusManager.focusNext();
            }

            revealFocusedElement();
            consumed = true;

            // --------------------------------------------------------
            // Text-field editing
            // --------------------------------------------------------

        } else if (focusManager.focusedElement() instanceof UiTextField field) {

            // A focused text field owns navigation
            // and editing keys.
            consumed = true;

            switch (key) {

                case BACKSPACE ->
                    field.deleteBackward();

                case DELETE ->
                    field.deleteForward();

                case LEFT ->
                    field.moveCursor(-1);

                case RIGHT ->
                    field.moveCursor(1);

                case HOME ->
                    field.moveCursorToStart();

                case END ->
                    field.moveCursorToEnd();

                case ESCAPE -> {
                    focusManager.clearFocus();
                    keyboardNavigationActive = false;
                }

                // Printable spaces are inserted through
                // the GLFW character callback instead.
                case SPACE, ENTER -> {
                    // Consume the physical key event.
                }

                default -> {
                    // Tab was handled above.
                }
            }

        } else if (focusManager.focusedElement() instanceof UiSlider slider) {

            switch (key) {

                case LEFT -> {
                    slider.stepBackward();
                    consumed = true;
                }

                case RIGHT -> {
                    slider.stepForward();
                    consumed = true;
                }

                case ENTER, SPACE -> {
                    // Retain existing keyboard activation.
                    slider.activate();
                    consumed = true;
                }

                default -> {
                    // Other keys are not handled here.
                }
            }

            // --------------------------------------------------------
            // Button activation
            // --------------------------------------------------------

        } else if (focusManager.focusedElement() instanceof UiButton button) {

            if (key == UiKey.ENTER
                    || key == UiKey.SPACE) {

                activatingButtonFromKeyboard = true;
                activatingNavigationKey = key;

                try {
                    button.activate();
                } finally {
                    activatingButtonFromKeyboard = false;
                    activatingNavigationKey = null;
                }

                focusManager.focusedElement();

                consumed = true;
            }
        }

        if (consumed) {
            heldKeys.add(key);
        }

        return consumed;
    }

    public void keyReleased(UiKey key) {

        Objects.requireNonNull(key, "key");

        heldKeys.remove(key);

        // Only releasing the key that activated
        // capture allows the next physical key
        // to be assigned.
        if (awaitingActivationRelease
                && key == captureActivationKey) {

            awaitingActivationRelease = false;
            captureActivationKey = null;
        }
    }

    /**
     * Scrolls containing viewports until the focused
     * element becomes visible.
     *
     * Works with nested scroll containers.
     */
    private void revealFocusedElement() {

        UiElement focused = focusManager.focusedElement();

        if (focused == null) {
            return;
        }

        root.updateLayout(root.viewportSize());

        for (UiElement parent = focused.parent(); parent != null; parent = parent.parent()) {

            if (!(parent instanceof UiScrollContainer scroll)) {
                continue;
            }

            UiRect viewport = scroll.bounds();
            UiRect item = focused.bounds();

            long itemBottom = (long) item.y() + item.height();

            long viewportBottom = (long) viewport.y() + viewport.height();

            long delta = 0;

            if (item.y() < viewport.y()) {
                // Item lies above the visible viewport.
                delta = (long) item.y() - viewport.y();

            } else if (itemBottom > viewportBottom) {
                // Item lies below the visible viewport.
                delta = itemBottom - viewportBottom;
            }

            if (delta != 0 && scroll.scrollBy(
                    (int) Math.max(
                            Integer.MIN_VALUE,
                            Math.min(Integer.MAX_VALUE, delta)))) {

                root.updateLayout(root.viewportSize());
            }
        }
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
        pointerX = x;
        pointerY = y;
        // A captured slider continues dragging even when
        // the pointer moves outside the control.
        if (activeSlider != null) {

            activeSlider.dragTo(x);

            return true;
        }

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
        if (isCapturingPhysicalKey()) {
            cancelPhysicalKeyCapture();
        }
        clearPressed();

        UiElement target = hitTest(x, y);

        pressedTarget = target;
        updateHovered(target);
        // Clicking inside the new UI enables keyboard navigation.
        // Clicking outside returns control to the original game.
        keyboardNavigationActive = target != null;

        // Select the keyboard scope belonging to the
        // window that received this mouse press.
        focusManager.activateScopeFor(target);

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

        // Position the caret when clicking inside
        // an enabled text field.
        if (target instanceof UiTextField field
                && field.enabled()) {

            field.placeCursorAt(x);
        }

        UiWindow window = findOwningWindow(target);

        boolean floatingWindow = window != null
                && window.parent() != null
                && window.parent().layoutStrategy() == UiAbsoluteLayout.INSTANCE;

        // Raise the owning window before handling the press.
        if (floatingWindow) {
            window.parent().bringToFront(window);
        }

        if (target instanceof UiSlider slider
                && slider.enabled()) {

            // Start horizontal slider dragging.
            activeSlider = slider;
            pressedTarget = slider;

            slider.setPressed(true);
            slider.beginDrag(x);

            return true;

        } else if (target instanceof UiScrollBar bar) {

            // Start vertical scrollbar dragging.
            draggingScrollBar = bar;

            UiRect thumb = bar.thumbBounds();

            if (thumb.contains(x, y)) {

                // Preserve the offset where the player
                // grabbed the scrollbar thumb.
                scrollThumbGrabY = y - thumb.y();

            } else {

                // Clicking the track moves the thumb
                // towards the clicked position.
                scrollThumbGrabY = thumb.height() / 2;
            }

            // Update the scroll position immediately.
            bar.scrollToPointer(
                    y,
                    scrollThumbGrabY);

            root.updateLayout(root.viewportSize());

            return true;

        } else if (floatingWindow) {

            if (target == window
                    && window.resizable() && !window.locked()
                    && window.resizeHandleBounds().contains(x, y)) {

                // Existing resize behaviour.
                resizingWindow = window;

                resizeStartX = x;
                resizeStartY = y;

                resizeStartWidth = window.bounds().width();
                resizeStartHeight = window.bounds().height();

            } else if (target == window && window.draggable() && !window.locked()
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

        // --------------------------------------------------------
        // Finish slider dragging before button activation
        // --------------------------------------------------------

        if (activeSlider != null) {

            UiSlider slider = activeSlider;

            activeSlider = null;

            // Apply the final position and commit once.
            slider.endDrag(x);
            slider.setPressed(false);

            // Release pointer capture.
            pressedTarget = null;

            updateHovered(hitTest(x, y));

            // CRITICAL:
            // Do not fall through to UiButton.activate().
            return true;
        }

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

    /**
     * Cancels all active UI input.
     *
     * Called when the game loses window focus or
     * changes to another screen.
     *
     * Preserves window positions, scroll positions
     * and text-field contents.
     */
    public void cancelInput() {

        // Cancel button presses, dragging, resizing,
        // scrollbar interactions and mouse hover.
        cancelPointer();
        cancelPhysicalKeyCapture();
        swallowedPhysicalKey = -1;

        // Release any navigation keys that were held.
        heldKeys.clear();

        // Remove keyboard focus and the active window scope.
        focusManager.clearFocus();
        focusManager.activateScopeFor(null);

        // The new UI must be clicked again before
        // receiving keyboard navigation or text input.
        keyboardNavigationActive = false;
    }

    private void clearPressed() {

        if (pressedTarget instanceof UiButton button) {
            button.setPressed(false);
        }

        if (activeSlider != null) {

            // Finish an interrupted drag and save
            // any live value changes.
            activeSlider.cancelDrag();

            activeSlider.setPressed(false);
            activeSlider = null;
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

        if (draggingWindow == null
                || !draggingWindow.draggable()
                || draggingWindow.locked()) {

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

        if (!window.isOpen()
                || !window.resizable()
                || window.locked()) {

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

        UiWindow modal = activeModal();

        if (modal != null) {

            // Search only inside the modal's component tree.
            UiElement target = hitTest(modal, x, y);

            if (target != null) {
                return target;
            }

            // Even outside its bounds, the modal captures
            // input so it cannot reach another window
            // or the game behind it.
            return modal;
        }

        // Normal behaviour when no modal exists.
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

        if (element instanceof UiWindow window
                && window.resizable()
                && !window.locked()
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
                || element instanceof UiScrollBar
                || element instanceof UiTextField) {

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

    /**
     * Finds the frontmost open modal belonging to
     * this input router's root.
     */
    private UiWindow activeModal() {

        if (!(root.content() instanceof UiContainer desktop)) {
            return null;
        }

        var children = desktop.children();

        for (int i = children.size() - 1; i >= 0; i--) {

            if (children.get(i) instanceof UiWindow window
                    && window.isOpen()
                    && window.modal()) {

                return window;
            }
        }

        return null;
    }

    /**
     * Last known pointer position in logical UI units.
     */
    public UiPoint pointerPosition() {
        return new UiPoint(pointerX, pointerY);
    }

    /**
     * Returns the button underneath the pointer,
     * including disabled buttons.
     *
     * A button removed from the tree or contained
     * inside a closed window cannot show a tooltip.
     */
    public UiButton hoveredButton() {

        UiButton button = hoveredButton;

        if (button == null) {
            return null;
        }

        boolean attachedToRoot = false;

        for (UiElement element = button; element != null; element = element.parent()) {

            if (!element.visible()) {
                return null;
            }

            if (element == root.content()) {
                attachedToRoot = true;
            }
        }

        if (!attachedToRoot) {
            return null;
        }

        UiWindow modal = activeModal();

        if (modal != null
                && findOwningWindow(button) != modal) {

            return null;
        }

        return button;
    }

}
