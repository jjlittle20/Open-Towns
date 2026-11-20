
package xaos.ui.integration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import xaos.panels.menus.MenuDefinition;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiSystem;

/**
 * Manages windows created from migrated XML menus.
 *
 * Responsibilities:
 * - Register migrated menu implementations
 * - Resolve XML submenu navigation
 * - Lazily construct windows
 * - Register windows with UiSystem
 * - Open, close and reuse windows
 *
 * Menus without a registered implementation
 * remain handled by the legacy menu system.
 *
 * Internal compatibility layer, not the public API.
 */
public final class UiMenuWindowController {

    private final UiSystem uiSystem;

    private final UiMenuNavigation navigation = new UiMenuNavigation();

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public UiMenuWindowController(UiSystem uiSystem) {

        this.uiSystem = Objects.requireNonNull(
                uiSystem, "uiSystem");
    }

    // ------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------

    /**
     * Registers a new-framework implementation for
     * an XML target menu ID.
     *
     * The factory is called only when the menu is
     * first opened.
     */
    public void register(
            String targetMenuId,
            Supplier<? extends UiWindow> windowFactory) {

        Objects.requireNonNull(
                windowFactory, "windowFactory");

        // UiMenuNavigation validates the ID and
        // rejects duplicate registrations.
        navigation.register(
                targetMenuId,
                () -> open(targetMenuId));

        entries.put(
                targetMenuId,
                new Entry(windowFactory));
    }

    /**
     * Resolves a selected legacy XML menu item.
     *
     * An empty result means legacy navigation
     * should continue normally.
     */
    public Optional<Runnable> resolve(
            MenuDefinition sourceMenu,
            String selectedItemId) {

        return navigation.resolve(
                sourceMenu,
                selectedItemId);
    }

    // ------------------------------------------------------------
    // Opening windows
    // ------------------------------------------------------------

    public void open(String targetMenuId) {

        Entry entry = requireEntry(targetMenuId);

        if (entry.window == null) {

            UiWindow window = Objects.requireNonNull(
                    entry.factory.get(),
                    "Menu factory returned null: "
                            + targetMenuId);

            // Register once, then reuse this window.
            uiSystem.windows().register(window);

            entry.window = window;
        }

        // Respect the window's configuration.
        if (entry.window.modal()) {

            uiSystem.openModal(entry.window);

        } else {

            uiSystem.windows().open(entry.window);
        }
    }

    // ------------------------------------------------------------
    // Closing windows
    // ------------------------------------------------------------

    public void close(String targetMenuId) {

        Entry entry = requireEntry(targetMenuId);

        if (entry.window == null
                || !entry.window.isOpen()) {
            return;
        }

        if (entry.window.modal()) {

            uiSystem.closeModal(entry.window);

        } else {

            uiSystem.windows().close(entry.window);
            uiSystem.cancelInput();
        }
    }

    /**
     * Closes all currently open migrated menus.
     *
     * Used when leaving the main menu or
     * rebuilding its legacy menu state.
     */
    public void closeAll() {

        for (String targetMenuId : entries.keySet()) {
            close(targetMenuId);
        }
    }

    // ------------------------------------------------------------
    // State
    // ------------------------------------------------------------

    public boolean isOpen(String targetMenuId) {

        Entry entry = requireEntry(targetMenuId);

        return entry.window != null
                && entry.window.isOpen();
    }

    /**
     * Returns whether a menu has a registered
     * new-framework implementation.
     *
     * Does not create or open the window.
     */
    public boolean isRegistered(String targetMenuId) {
        return targetMenuId != null
                && entries.containsKey(targetMenuId);
    }

    public boolean isAnyOpen() {

        for (Entry entry : entries.values()) {

            if (entry.window != null
                    && entry.window.isOpen()) {
                return true;
            }
        }

        return false;
    }

    private Entry requireEntry(String targetMenuId) {

        Entry entry = entries.get(targetMenuId);

        if (entry == null) {
            throw new IllegalArgumentException(
                    "No migrated menu registered: "
                            + targetMenuId);
        }

        return entry;
    }

    // ------------------------------------------------------------
    // Internal registration
    // ------------------------------------------------------------

    private static final class Entry {

        private final Supplier<? extends UiWindow> factory;

        private UiWindow window;

        private Entry(
                Supplier<? extends UiWindow> factory) {

            this.factory = factory;
        }
    }
}
