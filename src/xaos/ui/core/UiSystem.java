
package xaos.ui.core;

import java.util.Objects;

import xaos.ui.components.UiWindow;
import xaos.ui.input.UiInputRouter;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiElementPainter;
import xaos.ui.rendering.UiRenderer;
import xaos.ui.rendering.UiSceneRenderer;
import xaos.ui.theme.UiThemeRegistry;

/**
 * Owns the runtime state of one UI component tree.
 *
 * Coordinates windows, input, layout and themes.
 *
 * Does not depend on GLFW, OpenGL or game logic.
 *
 * Not part of the public modding API.
 */
public final class UiSystem {

    private final UiWindowManager windows;
    private final UiInputRouter input;
    private final UiThemeRegistry themes;

    // ------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------

    public UiSystem() {
        this(
                new UiWindowManager(),
                new UiThemeRegistry());
    }

    /**
     * Allows existing window managers and theme
     * registries to be adopted by the UI system.
     *
     * Useful while migrating older UI code.
     */
    public UiSystem(
            UiWindowManager windows,
            UiThemeRegistry themes) {

        this.windows = Objects.requireNonNull(
                windows, "windows");

        this.themes = Objects.requireNonNull(
                themes, "themes");

        // The input router must use the SAME root
        // as the window manager.
        this.input = new UiInputRouter(
                windows.root());
    }

    // ------------------------------------------------------------
    // Owned systems
    // ------------------------------------------------------------

    public UiWindowManager windows() {
        return windows;
    }

    public UiRoot root() {
        return windows.root();
    }

    public UiInputRouter input() {
        return input;
    }

    public UiThemeRegistry themes() {
        return themes;
    }

    // ------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------

    /**
     * Updates layout using the current viewport size.
     *
     * Includes window positioning constraints.
     */
    public boolean updateLayout(UiSize viewport) {

        return windows.updateLayout(
                Objects.requireNonNull(
                        viewport, "viewport"));
    }

    // ------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------

    /**
     * Renders the current UI tree.
     *
     * The caller provides a graphics backend and
     * component painter.
     *
     * Call updateLayout() before rendering.
     */
    public void render(
            UiRenderer renderer,
            UiElementPainter painter) {

        UiSceneRenderer.render(
                root(),
                Objects.requireNonNull(
                        renderer, "renderer"),
                Objects.requireNonNull(
                        painter, "painter"));
    }

    // ------------------------------------------------------------
    // Input lifecycle
    // ------------------------------------------------------------

    /**
     * Cancels active mouse and keyboard interactions.
     *
     * Preserves all window and component state.
     */
    public void cancelInput() {
        input.cancelInput();
    }

    /**
     * Opens a modal window and transfers input ownership
     * to it.
     *
     * The window must already be registered.
     */
    public void openModal(UiWindow window) {

        Objects.requireNonNull(window, "window");

        if (!window.modal()) {
            throw new IllegalArgumentException(
                    "Window is not configured as modal");
        }

        // Release any drag, held key or focus belonging
        // to the previously active UI.
        input.cancelInput();

        windows.open(window);

        // A newly opened modal immediately owns keyboard
        // navigation. No initial mouse click is necessary.
        input.activateModalKeyboardNavigation(window);

    }

    /**
     * Closes a modal window and releases its input.
     */
    public void closeModal(UiWindow window) {

        Objects.requireNonNull(window, "window");

        windows.close(window);
        input.cancelInput();

        UiWindow remaining = windows.activeModal();

        if (remaining != null) {

            // If another modal remains open underneath,
            // restore its keyboard navigation immediately.
            input.activateModalKeyboardNavigation(remaining);
        }

    }

}
