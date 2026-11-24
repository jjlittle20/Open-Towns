
package xaos.ui.core;

import java.util.List;
import java.util.Objects;
import xaos.ui.components.UiWindow;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiWindowAnchor;

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

        // ------------------------------------------------------------
        // Calculate docking dimensions
        // ------------------------------------------------------------

        boolean dockSizeChanged = false;

        for (UiWindow window : windows()) {

            UiSize requestedSize = UiWindowPlacement.dockedSize(
                    window, viewport);

            if (window.applyDockedSize(requestedSize)) {
                dockSizeChanged = true;
            }
        }

        // Window bounds must reflect the new size
        // before screen-edge positioning is calculated.
        if (dockSizeChanged) {
            root.updateLayout(viewport);
        }

        boolean repositioned = false;

        for (UiWindow window : windows()) {

            boolean moved;

            if (window.anchor() != UiWindowAnchor.NONE) {

                // Anchored panels take priority over
                // normal floating/centred positioning.
                moved = UiWindowPlacement.anchorInViewport(
                        window,
                        viewport);

            } else if (window.centered()) {

                // Preserve existing modal menu behaviour.
                moved = UiWindowPlacement.centerInViewport(
                        window,
                        viewport);

            } else {

                // Preserve existing floating-window behaviour.
                moved = UiWindowPlacement.constrainToViewport(
                        window,
                        viewport);
            }

            if (moved) {
                repositioned = true;
            }
        }

        // Moving windows invalidates their layouts.
        // Perform a second pass only when necessary.
        if (repositioned) {
            root.updateLayout(viewport);
        }

        return updated || dockSizeChanged || repositioned;
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

        UiWindow modal = activeModal();

        if (modal != null) {
            desktop.bringToFront(modal);
        }

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

    /**
     * Returns the frontmost open modal window,
     * or null when no modal is active.
     */
    public UiWindow activeModal() {

        List<UiWindow> ordered = windows();

        for (int i = ordered.size() - 1; i >= 0; i--) {

            UiWindow window = ordered.get(i);

            if (window.isOpen() && window.modal()) {
                return window;
            }
        }

        return null;
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

        // Newly opened windows normally come forward.
        desktop.bringToFront(window);

        // But an existing modal must stay above any
        // ordinary window that opens behind it.
        UiWindow modal = activeModal();

        if (modal != null) {
            desktop.bringToFront(modal);
        }
    }

    /**
     * Hides a registered window without removing it.
     */
    public void close(UiWindow window) {
        requireManaged(window);

        window.close();

        // If another modal remains open, bring it forward.
        UiWindow modal = activeModal();

        if (modal != null) {
            desktop.bringToFront(modal);
        }

    }

    /**
     * Permanently removes a window from this manager.
     *
     * Unlike close(), this detaches the window from
     * the UI tree so it can be garbage-collected
     * or registered with another manager.
     *
     * Callers must cancel any active input belonging
     * to the window before unregistering it.
     */
    public void unregister(UiWindow window) {

        requireManaged(window);

        // Hide before detaching it.
        window.close();

        // Removing the child also clears its parent.
        desktop.remove(window);

        // Preserve modal stacking if another modal
        // window remains open underneath.
        UiWindow modal = activeModal();

        if (modal != null) {
            desktop.bringToFront(modal);
        }
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

        UiWindow modal = activeModal();

        // Ordinary windows cannot rise above
        // an active modal window.
        if (modal != null && modal != window) {
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
