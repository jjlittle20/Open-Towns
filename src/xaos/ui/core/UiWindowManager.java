
package xaos.ui.core;

import java.util.List;
import java.util.Objects;
import xaos.ui.components.UiWindow;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiSize;

/**
 * Manages windows within a UI desktop.
 *
 * Windows are kept in stacking order, with the last
 * window appearing above earlier windows.
 *
 * Closing a window preserves its contents and state.
 */
public final class UiWindowManager {

    private final UiContainer desktop;
    private final UiRoot root;

    public UiWindowManager() {
        desktop = new UiContainer();
        desktop.setLayoutStrategy(
                UiAbsoluteLayout.INSTANCE);

        root = new UiRoot(desktop);
    }

    // ------------------------------------------------------------
    // Root
    // ------------------------------------------------------------

    public UiRoot root() {
        return root;
    }

    /**
     * Updates the UI layout and keeps registered windows
     * accessible after viewport or content changes.
     *
     * @return true if layout or window positioning changed
     */
    public boolean updateLayout(UiSize viewport) {
        Objects.requireNonNull(viewport, "viewport");

        boolean updated = root.updateLayout(viewport);
        boolean repositioned = false;

        for (UiWindow window : windows()) {
            if (UiWindowPlacement.constrainToViewport(
                    window, viewport)) {

                repositioned = true;
            }
        }

        // Moving windows invalidates their layouts.
        // Perform a second pass only when necessary.
        if (repositioned) {
            root.updateLayout(viewport);
        }

        return updated || repositioned;
    }

    // ------------------------------------------------------------
    // Window registration
    // ------------------------------------------------------------

    /**
     * Registers a window with this manager.
     *
     * The window must not already belong to a container.
     * Its existing visibility is preserved.
     */
    public void register(UiWindow window) {
        Objects.requireNonNull(window, "window");

        if (window.parent() != null) {
            throw new IllegalArgumentException(
                    "Window already belongs to a container");
        }

        desktop.add(window);
    }

    /**
     * Returns registered windows in stacking order.
     *
     * The returned list cannot be modified.
     */
    public List<UiWindow> windows() {
        return desktop.children().stream()
                .filter(UiWindow.class::isInstance)
                .map(UiWindow.class::cast)
                .toList();
    }

    public boolean isManaged(UiWindow window) {
        return window != null && window.parent() == desktop;
    }

    // ------------------------------------------------------------
    // Window lifecycle
    // ------------------------------------------------------------

    /**
     * Opens a registered window and brings it forward.
     */
    public void open(UiWindow window) {
        requireManaged(window);

        window.open();
        desktop.bringToFront(window);
    }

    /**
     * Hides a registered window without removing it.
     */
    public void close(UiWindow window) {
        requireManaged(window);

        window.close();
    }

    // ------------------------------------------------------------
    // Stacking order
    // ------------------------------------------------------------

    /**
     * Raises an open window above the other windows.
     *
     * Returns true if the stacking order changed.
     */
    public boolean bringToFront(UiWindow window) {
        requireManaged(window);

        if (!window.isOpen()) {
            return false;
        }

        return desktop.bringToFront(window);
    }

    // ------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------

    private void requireManaged(UiWindow window) {
        Objects.requireNonNull(window, "window");

        if (!isManaged(window)) {
            throw new IllegalArgumentException(
                    "Window is not registered with this manager");
        }
    }
}
