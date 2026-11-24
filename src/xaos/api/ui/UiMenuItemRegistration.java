
package xaos.api.ui;

import xaos.api.mods.ModResource;

/**
 * Public registration handle for a menu item
 * contributed by a Java mod.
 */
public interface UiMenuItemRegistration
        extends ModResource {

    String menuId();

    String itemId();

    boolean isActive();

    /**
     * Permanently removes this contribution.
     * Calling it repeatedly is safe.
     */
    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
