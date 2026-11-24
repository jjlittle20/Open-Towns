package xaos.api.ui;

import java.util.List;

/**
 * Public API for interacting with the Open Towns user interface.
 *
 * Mods should use this API rather than accessing internal panel
 * and menu classes directly.
 */
public interface UiApi {

        void showMessage(
                        UiMessageType type,
                        String message);

        default void showMessage(
                        String message) {
                showMessage(
                                UiMessageType.SYSTEM,
                                message);
        }

        void showInfoDialog(
                        String title,
                        List<String> lines);

        UiActionRegistration registerGameAction(
                        String id,
                        String label,
                        Runnable action);

        /**
         * Creates a mod-owned UI window.
         *
         * The ID must be namespaced, for example:
         * "example-mod:inventory".
         *
         * The window starts hidden.
         *
         * Track the returned handle through
         * ModContext.worldResource() or resource()
         * to ensure automatic cleanup.
         */
        UiWindowHandle createWindow(
                        String id,
                        String title);

        /**
         * Adds a runtime button to an existing menu.
         *
         * The actionId must identify an action registered
         * through registerGameAction().
         *
         * Both IDs must be namespaced.
         *
         * The icon may be null.
         *
         * Track the returned handle with
         * ModContext.worldResource() or resource().
         */
        UiMenuItemRegistration addMenuAction(
                        String menuId,
                        String itemId,
                        String title,
                        String icon,
                        String actionId);

        /**
         * Convenience overload for an item without an icon.
         */
        default UiMenuItemRegistration addMenuAction(
                        String menuId,
                        String itemId,
                        String title,
                        String actionId) {

                return addMenuAction(
                                menuId,
                                itemId,
                                title,
                                null,
                                actionId);
        }

}