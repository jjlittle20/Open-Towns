
package xaos.ui.integration;

import java.util.Objects;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.SmartMenu;

/**
 * Temporary compatibility bridge for XML menus
 * whose child screens have not yet been migrated.
 *
 * Resolves an XML targetMenu ID to the matching
 * child of an existing legacy SmartMenu.
 */
public final class UiLegacySubmenuBridge {

    private UiLegacySubmenuBridge() {
    }

    public static SmartMenu find(
            MenuDefinition definition,
            SmartMenu legacyParent,
            String targetMenuId) {

        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(legacyParent, "legacyParent");
        Objects.requireNonNull(targetMenuId, "targetMenuId");

        for (MenuItemDefinition item : definition.getItems()) {

            if (!"submenu".equals(item.getType())
                    || !targetMenuId.equals(
                            item.getTargetMenu())) {
                continue;
            }

            for (SmartMenu child : legacyParent.getItems()) {

                if (child.getType() == SmartMenu.TYPE_MENU
                        && item.getId().equals(child.getID())) {
                    return child;
                }
            }

            throw new IllegalStateException(
                    "Legacy submenu not found for XML item: "
                            + item.getId());
        }

        throw new IllegalArgumentException(
                "No submenu target '" + targetMenuId
                        + "' in " + definition.getId());
    }
}
