
package xaos.ui.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;

/**
 * Routes XML-defined submenu navigation into
 * the new UI framework.
 *
 * Unregistered routes remain handled by the
 * legacy menu system during migration.
 *
 * Does not depend on SmartMenu, Game or OpenGL.
 */
public final class UiMenuNavigation {

    private final Map<String, Runnable> routes = new HashMap<>();

    /**
     * Registers a handler for a target menu ID.
     *
     * Example: "main.credits.root"
     */
    public void register(
            String targetMenuId,
            Runnable openAction) {

        if (targetMenuId == null
                || targetMenuId.isBlank()) {

            throw new IllegalArgumentException(
                    "Target menu ID cannot be empty");
        }

        Objects.requireNonNull(
                openAction, "openAction");

        if (routes.putIfAbsent(
                targetMenuId, openAction) != null) {

            throw new IllegalArgumentException(
                    "Menu route already registered: "
                            + targetMenuId);
        }
    }

    /**
     * Resolves a selected XML item to a registered
     * new-framework navigation action.
     *
     * Does not execute the action.
     *
     * Empty means the legacy menu system should
     * handle the navigation normally.
     */
    public Optional<Runnable> resolve(
            MenuDefinition sourceMenu,
            String selectedItemId) {

        Objects.requireNonNull(
                sourceMenu, "sourceMenu");

        if (selectedItemId == null) {
            return Optional.empty();
        }

        for (MenuItemDefinition item : sourceMenu.getItems()) {

            if (!selectedItemId.equals(item.getId())) {
                continue;
            }

            // Only submenu navigation is redirected.
            // Legacy command buttons, toggles and
            // other controls are unaffected.
            if (!"submenu".equals(item.getType())) {
                return Optional.empty();
            }

            String targetMenuId = item.getTargetMenu();

            if (targetMenuId == null) {
                return Optional.empty();
            }

            return Optional.ofNullable(
                    routes.get(targetMenuId));
        }

        return Optional.empty();
    }
}
