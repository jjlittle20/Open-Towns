package xaos.api.ui;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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

        /**
         * Registers a Java callback and adds a button
         * to an existing gameplay menu.
         *
         * The item ID is also used as the action ID,
         * avoiding mismatches between the two.
         *
         * The icon may be null.
         *
         * Track the result with context.worldResource()
         * for automatic cleanup when the world unloads.
         */
        default UiMenuButtonRegistration registerMenuButton(
                        String menuId,
                        String itemId,
                        String title,
                        String icon,
                        Runnable callback) {

                Objects.requireNonNull(callback, "callback");

                // First register the action.
                UiActionRegistration action = registerGameAction(
                                itemId,
                                title,
                                callback);

                UiMenuItemRegistration item;

                try {

                        // Both registrations use the same ID.
                        item = addMenuAction(
                                        menuId,
                                        itemId,
                                        title,
                                        icon,
                                        itemId);

                } catch (RuntimeException | Error exception) {

                        // Do not leave an orphaned callback if
                        // creating the menu registration fails.
                        action.unregister();
                        throw exception;
                }

                return new UiMenuButtonRegistration() {

                        private final AtomicBoolean disposed = new AtomicBoolean(false);

                        @Override
                        public String menuId() {
                                return item.menuId();
                        }

                        @Override
                        public String itemId() {
                                return item.itemId();
                        }

                        @Override
                        public boolean isActive() {

                                return !disposed.get()
                                                && item.isActive()
                                                && action.isActive();
                        }

                        @Override
                        public void unregister() {

                                if (!disposed.compareAndSet(
                                                false, true)) {
                                        return;
                                }

                                try {
                                        item.unregister();
                                } finally {
                                        action.unregister();
                                }
                        }
                };
        }

        /**
         * Convenience overload without an icon.
         */
        default UiMenuButtonRegistration registerMenuButton(
                        String menuId,
                        String itemId,
                        String title,
                        Runnable callback) {

                return registerMenuButton(
                                menuId,
                                itemId,
                                title,
                                null,
                                callback);
        }

}