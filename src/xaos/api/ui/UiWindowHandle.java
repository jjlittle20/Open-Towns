
package xaos.api.ui;

import xaos.api.mods.ModResource;

/**
 * Public handle for a mod-owned UI window.
 *
 * The implementation belongs to the game.
 * Mods must not access internal UI components.
 */
public interface UiWindowHandle extends ModResource {

    /**
     * Unique namespaced window ID.
     */
    String id();

    /**
     * Update the title displayed by the window.
     */
    void setTitle(String title);

    /**
     * Add a text label to the window.
     */
    void addLabel(String text);

    /**
     * Add a button with a callback.
     */
    void addButton(
            String text,
            Runnable onClick);

    /**
     * Show the window, or bring it forward
     * if it is already open.
     */
    void show();

    /**
     * Hide the window without destroying it.
     */
    void hide();

    /**
     * Whether the window is currently visible.
     *
     * UI changes are dispatched to the game
     * thread, so this reflects the last applied
     * UI update.
     */
    boolean isOpen();

    /**
     * Whether this handle remains registered.
     */
    boolean isActive();

    /**
     * Permanently unregister the window.
     *
     * Automatically called when tracked by
     * ModContext.worldResource() and the
     * world unloads.
     */
    @Override
    void close();
}
