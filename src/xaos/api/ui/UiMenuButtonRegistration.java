
package xaos.api.ui;

import xaos.api.mods.ModResource;

/**
 * Owns both a Java UI action and its
 * corresponding menu button.
 */
public interface UiMenuButtonRegistration
        extends ModResource {

    String menuId();

    String itemId();

    boolean isActive();

    /**
     * Unregisters both the menu button
     * and the associated Java action.
     */
    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
