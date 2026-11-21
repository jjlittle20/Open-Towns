
package xaos.ui.integration;

import xaos.panels.menus.MenuItemDefinition;

/**
 * Connects declarative menu actions to gameplay.
 *
 * The UI does not need to know whether the action
 * belongs to Towns or to a future Java mod.
 */
public interface UiGameplayActionBinding {

    /**
     * Whether this action can currently be used.
     */
    boolean isAvailable(MenuItemDefinition item);

    /**
     * Executes the action on the game thread.
     */
    void execute(MenuItemDefinition item);

    /**
     * Safe default for preview-only menus.
     */
    static UiGameplayActionBinding disabled() {

        return new UiGameplayActionBinding() {

            @Override
            public boolean isAvailable(
                    MenuItemDefinition item) {

                return false;
            }

            @Override
            public void execute(
                    MenuItemDefinition item) {

                // Preview-only: never execute.
            }
        };
    }
}
