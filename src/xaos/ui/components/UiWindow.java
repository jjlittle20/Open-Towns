
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
