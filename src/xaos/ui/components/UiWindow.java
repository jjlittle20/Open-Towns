
package xaos.ui.components;

import java.util.Objects;
import java.util.Optional;

import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiLinearLayout;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiSizePolicy;
import xaos.ui.layout.UiWindowAnchor;
import xaos.ui.rendering.UiTextMetrics;

/**
 * A reusable window with a title bar and content area.
 *
 * This class defines the structure and layout of a window.
 * Rendering, dragging, closing and input handling will be
 * implemented separately.
 */
public class UiWindow extends UiContainer {

    private String title;

    private final UiContainer titleBar;
    private final UiContainer content;
    /**
     * Explicit dimensions selected by the player.
     *
     * Null means the window uses automatic content sizing.
     */
    private UiSize userSize;
    public static final int RESIZE_HANDLE_SIZE = 16;

    private boolean resizable = true;
    private boolean draggable = true;
    private boolean locked = false;

    private UiWindowLockButton lockButton;
    private boolean centered = false;
    private boolean modal = false;
    private UiWindowCloseButton closeButton;

    public boolean draggable() {
        return draggable;
    }

    public void setDraggable(boolean draggable) {

        this.draggable = draggable;

        // Only draggable windows display a lock button.
        if (lockButton != null) {
            lockButton.setVisible(draggable);
        }
    }

    /**
     * Whether this window is locked against
     * mouse-driven movement and resizing.
     */
    public boolean locked() {
        return locked;
    }

    /**
     * Locks or unlocks this window.
     *
     * Locking does not change draggable/resizable,
     * and does not disable content interaction.
     */
    public void setLocked(boolean locked) {

        if (this.locked == locked) {
            return;
        }

        this.locked = locked;

        if (lockButton != null) {
            lockButton.refresh(this);
        }
    }

    public boolean centered() {
        return centered;
    }

    public void setCentered(boolean centered) {
        if (this.centered != centered) {
            this.centered = centered;
            invalidateLayout();
        }
    }

    /**
     * Whether this window blocks interaction with
     * other windows and the game behind it.
     */
    public boolean modal() {
        return modal;
    }

    public void setModal(boolean modal) {
        this.modal = modal;
    }

    // ------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------

    public UiWindow(String title) {
        this.title = Objects.requireNonNull(title, "title");

        setLayoutStrategy(UiLinearLayout.VERTICAL);

        setPadding(UiInsets.all(8));
        setSpacing(4);

        // Default minimum window dimensions.
        setMinimumSize(new UiSize(180, 80));

        // ------------------------------------------------------------
        // Title bar
        // ------------------------------------------------------------

        titleBar = new UiContainer();

        titleBar.setLayoutStrategy(
                UiLinearLayout.HORIZONTAL);

        titleBar.setPadding(
                new UiInsets(2, 2, 2, 2));

        titleBar.setPreferredSize(new UiSize(0, 24));
        titleBar.setMinimumSize(new UiSize(0, 24));

        titleBar.setHorizontalSizePolicy(
                UiSizePolicy.FILL);

        // Flexible space before the window controls.
        // This pushes buttons towards the right edge.
        UiElement titleSpace = new UiElement();

        titleSpace.setHorizontalSizePolicy(
                UiSizePolicy.FILL);

        titleBar.add(titleSpace);

        // Content area.
        content = new UiContainer();

        content.setLayoutStrategy(UiLinearLayout.VERTICAL);
        // Content must not render outside the window's
        // allocated content area.
        content.setClipChildren(true);

        content.setHorizontalSizePolicy(
                UiSizePolicy.FILL);

        content.setVerticalSizePolicy(
                UiSizePolicy.FILL);

        // Add the structural elements to the window.
        add(titleBar);
        add(content);
    }

    // ------------------------------------------------------------
    // Title
    // ------------------------------------------------------------

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        String newTitle = Objects.requireNonNull(
                title, "title");

        if (!this.title.equals(newTitle)) {
            this.title = newTitle;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Window components
    // ------------------------------------------------------------

    /**
     * Returns the window's title-bar container.
     *
     * Custom controls may be added to this container.
     */
    public UiContainer titleBar() {
        return titleBar;
    }

    /**
     * Installs the standard title-bar lock button.
     *
     * Repeated calls reuse the same button.
     *
     * The button is only visible when draggable
     * is enabled.
     */
    public void installLockButton(
            UiTextMetrics textMetrics) {

        Objects.requireNonNull(
                textMetrics, "textMetrics");

        if (lockButton == null) {

            lockButton = new UiWindowLockButton(
                    this, textMetrics);

            titleBar.add(lockButton);
        }

        lockButton.setVisible(draggable);
        lockButton.refresh(this);
    }

    /**
     * Returns the title-bar lock button,
     * if it has been installed.
     */
    public Optional<UiWindowLockButton> lockButton() {
        return Optional.ofNullable(lockButton);
    }

    /**
     * Installs or updates the standard title-bar
     * close button.
     *
     * Repeated calls update the callback without
     * creating duplicate buttons.
     *
     * The callback should use the appropriate
     * window manager or UiSystem close method.
     */
    public void setCloseAction(
            UiTextMetrics textMetrics,
            Runnable onClose) {

        Objects.requireNonNull(
                textMetrics, "textMetrics");

        Objects.requireNonNull(
                onClose, "onClose");
        // Install Lock before X so the title-bar order
        // is Lock / Close.
        installLockButton(textMetrics);

        if (closeButton == null) {

            closeButton = new UiWindowCloseButton(
                    textMetrics,
                    onClose);

            // The title bar already contains flexible
            // space, so this appears on the right.
            titleBar.add(closeButton);

        } else {

            closeButton.setOnClick(onClose);
        }
    }

    /**
     * Returns the standard close button if one
     * has been installed.
     */
    public Optional<UiWindowCloseButton> closeButton() {
        return Optional.ofNullable(closeButton);
    }

    /**
     * Returns the window's content container.
     *
     * UI controls should be added to this container,
     * not directly to the window.
     */
    public UiContainer content() {
        return content;
    }

    // ------------------------------------------------------------
    // Window lifecycle
    // ------------------------------------------------------------

    /**
     * Opens this window.
     *
     * The window retains its existing contents, position
     * and state when reopened.
     */
    public void open() {
        setVisible(true);
    }

    /**
     * Closes the window without destroying it.
     *
     * The window remains in its parent container and
     * can be reopened later.
     */
    public void close() {
        setVisible(false);
    }

    /**
     * Returns whether the window is currently open.
     */
    public boolean isOpen() {
        return visible();
    }

    // ------------------------------------------------------------
    // Screen-edge anchoring
    // ------------------------------------------------------------

    private UiWindowAnchor anchor = UiWindowAnchor.NONE;

    private UiInsets anchorInsets = UiInsets.NONE;

    /**
     * Returns the window's requested screen anchor.
     *
     * NONE means normal floating-window positioning.
     */
    public UiWindowAnchor anchor() {
        return anchor;
    }

    public void setAnchor(UiWindowAnchor next) {

        Objects.requireNonNull(next, "next");

        if (anchor != next) {
            anchor = next;
            invalidateLayout();
        }
    }

    /**
     * Returns the spacing between the window
     * and the screen edges.
     */
    public UiInsets anchorInsets() {
        return anchorInsets;
    }

    public void setAnchorInsets(UiInsets next) {

        Objects.requireNonNull(next, "next");

        if (!anchorInsets.equals(next)) {
            anchorInsets = next;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // User-controlled sizing
    // ------------------------------------------------------------

    /**
     * Returns the explicit size selected by the user,
     * if one has been assigned.
     */
    public Optional<UiSize> userSize() {
        return Optional.ofNullable(userSize);
    }

    /**
     * Sets an explicit window size.
     *
     * This overrides automatic content measurement,
     * but the window's minimum size still applies.
     */
    public void setUserSize(UiSize size) {
        UiSize newSize = Objects.requireNonNull(
                size, "size");

        if (!newSize.equals(userSize)) {
            userSize = newSize;
            invalidateLayout();
        }
    }

    /**
     * Returns the window to automatic sizing.
     *
     * The previous preferred size and content
     * measurements are used again.
     */
    public void clearUserSize() {
        if (userSize != null) {
            userSize = null;
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

        // No user size: use the existing automatic
        // container measurement.
        if (userSize == null) {
            return super.measure(available);
        }

        // Explicitly sized windows may be smaller
        // than their content's preferred dimensions.
        // Minimum window dimensions still apply.
        return new UiSize(
                Math.max(
                        minimumSize().width(),
                        userSize.width()),
                Math.max(
                        minimumSize().height(),
                        userSize.height()));
    }

    // ------------------------------------------------------------
    // Resizing
    // ------------------------------------------------------------

    /**
     * Returns whether the player can resize this window.
     */
    public boolean resizable() {
        return resizable;
    }

    /**
     * Enables or disables mouse-driven resizing.
     *
     * Does not prevent programmatic size changes.
     */
    public void setResizable(boolean resizable) {
        this.resizable = resizable;
    }

    /**
     * Returns the resize handle's position.
     *
     * The handle occupies the bottom-right corner
     * of the window's current bounds.
     */
    public UiRect resizeHandleBounds() {
        UiRect bounds = bounds();

        int width = Math.min(
                RESIZE_HANDLE_SIZE, bounds.width());

        int height = Math.min(
                RESIZE_HANDLE_SIZE, bounds.height());

        return new UiRect(
                Math.addExact(
                        bounds.x(), bounds.width() - width),
                Math.addExact(
                        bounds.y(), bounds.height() - height),
                width,
                height);
    }

}
