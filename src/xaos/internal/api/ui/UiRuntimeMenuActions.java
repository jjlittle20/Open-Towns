
package xaos.internal.api.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import xaos.api.ui.UiMenuItemRegistration;
import xaos.internal.api.mods.ModUiErrors;
import xaos.panels.UI.UiDispatcher;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;

/**
 * Runtime menu contributions from Java mods.
 *
 * Registry changes run through UiDispatcher.
 * The public SDK never sees MenuManager or
 * MenuItemDefinition.
 */
public final class UiRuntimeMenuActions {

    private static final Map<String, Registration> entries = new LinkedHashMap<>();

    private static MenuManager currentMenus;
    private static Runnable refreshUi = () -> {
    };

    private UiRuntimeMenuActions() {
    }

    /**
     * Called whenever the game replaces its
     * gameplay menu registry.
     */
    public static synchronized void bind(
            MenuManager menus,
            Runnable refresh) {

        currentMenus = menus;
        refreshUi = Objects.requireNonNull(
                refresh, "refresh");

        UiDispatcher.invokeLater(() -> {

            for (Registration entry : snapshot()) {
                install(entry);
            }
        });
    }

    public static synchronized UiMenuItemRegistration register(
            String menuId,
            String itemId,
            String title,
            String icon,
            String actionId) {

        requireText(menuId, "Menu ID");
        requireNamespaced(itemId, "Item ID");
        requireNamespaced(actionId, "Action ID");
        requireText(title, "Item title");

        String key = key(menuId, itemId);

        if (entries.containsKey(key)) {
            throw new IllegalArgumentException(
                    "Runtime menu item already registered: "
                            + itemId);
        }

        MenuItemDefinition item = new MenuItemDefinition(
                itemId,
                "action",
                null,
                null,
                "MOD_ACTION",
                false,
                true,
                null,
                null,
                null,
                null,
                true,
                title,
                icon,
                actionId,
                null);

        Registration registration = new Registration(menuId, item);

        entries.put(key, registration);

        UiDispatcher.invokeLater(
                () -> install(registration));

        return registration;
    }

    private static synchronized List<Registration> snapshot() {
        return new ArrayList<>(entries.values());
    }

    private static void install(Registration entry) {

        MenuManager target;

        synchronized (UiRuntimeMenuActions.class) {

            if (!entry.active
                    || entries.get(entry.key()) != entry) {
                return;
            }

            target = currentMenus;
        }

        // Registration may occur before the
        // game has loaded its menu definitions.
        if (target == null) {
            return;
        }

        if (entry.installedIn == target) {
            return;
        }

        // Remove the contribution from the old
        // registry before applying it to the new.
        if (entry.installedIn != null) {
            entry.installedIn.removeRuntimeItem(
                    entry.menuId,
                    entry.item);
            entry.installedIn = null;
        }

        try {

            target.addRuntimeItem(
                    entry.menuId,
                    entry.item);

            entry.installedIn = target;

            // A mod could have unloaded while
            // the registration was being applied.
            if (!entry.active) {

                target.removeRuntimeItem(
                        entry.menuId,
                        entry.item);

                entry.installedIn = null;
                return;
            }

            changed(target);

        } catch (RuntimeException exception) {

            ModUiErrors.report(
                    namespace(entry.item.getId()),
                    "Runtime menu registration",
                    "Could not add " + entry.item.getId()
                            + " to " + entry.menuId,
                    exception);
        }
    }

    private static void changed(MenuManager source) {

        Runnable callback;

        synchronized (UiRuntimeMenuActions.class) {

            if (currentMenus != source) {
                return;
            }

            callback = refreshUi;
        }

        callback.run();
    }

    private static void requireText(
            String text,
            String description) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    description + " cannot be blank");
        }
    }

    private static void requireNamespaced(
            String id,
            String description) {

        requireText(id, description);

        int separator = id.indexOf(':');

        if (separator <= 0
                || separator == id.length() - 1
                || id.chars().anyMatch(
                        Character::isWhitespace)) {

            throw new IllegalArgumentException(
                    description + " must be namespaced: "
                            + id);
        }
    }

    private static String namespace(String id) {
        return id.substring(0, id.indexOf(':'));
    }

    private static String key(
            String menuId,
            String itemId) {

        return menuId + "\u0000" + itemId;
    }

    private static final class Registration
            implements UiMenuItemRegistration {

        private final String menuId;
        private final MenuItemDefinition item;

        private volatile boolean active = true;

        // Accessed on the UI-dispatch thread.
        private MenuManager installedIn;

        private Registration(
                String menuId,
                MenuItemDefinition item) {

            this.menuId = menuId;
            this.item = item;
        }

        private String key() {
            return UiRuntimeMenuActions.key(
                    menuId, item.getId());
        }

        @Override
        public String menuId() {
            return menuId;
        }

        @Override
        public String itemId() {
            return item.getId();
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void unregister() {

            synchronized (UiRuntimeMenuActions.class) {

                if (!active) {
                    return;
                }

                active = false;
                entries.remove(key(), this);
            }

            UiDispatcher.invokeLater(() -> {

                MenuManager previous = installedIn;
                installedIn = null;

                if (previous != null
                        && previous.removeRuntimeItem(
                                menuId, item)) {

                    changed(previous);
                }
            });
        }
    }
}
