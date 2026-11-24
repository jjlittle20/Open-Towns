
package xaos.internal.api.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import xaos.api.ui.UiMenuItemRegistration;
import xaos.panels.UI.UiDispatcher;
import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;

class UiRuntimeMenuActionsTest {

    private static final String ROOT = "game.right.root";

    private final List<UiMenuItemRegistration> handles = new ArrayList<>();

    private static MenuManager manager() {

        MenuManager manager = new MenuManager();

        MenuDefinition menu = new MenuDefinition(
                ROOT, null, "Construction");

        menu.addItem(new MenuItemDefinition(
                ROOT + ".back",
                "back",
                null,
                null,
                null,
                false,
                true,
                null,
                null,
                null,
                null,
                true));

        manager.registerMenu(menu);

        return manager;
    }

    private UiMenuItemRegistration register(
            String id) {

        UiMenuItemRegistration handle = UiRuntimeMenuActions.register(
                ROOT,
                id,
                "Mod Button",
                null,
                "test:hello");

        handles.add(handle);

        return handle;
    }

    private static List<String> ids(
            MenuManager manager) {

        return manager.requireMenu(ROOT)
                .getItems()
                .stream()
                .map(MenuItemDefinition::getId)
                .toList();
    }

    @AfterEach
    void cleanup() {

        for (UiMenuItemRegistration handle : handles) {
            handle.unregister();
        }

        UiDispatcher.processPending();

        UiRuntimeMenuActions.bind(
                null, () -> {
                });

        UiDispatcher.processPending();
    }

    @Test
    void registrationAddsAndRemovesButton() {

        MenuManager manager = manager();

        AtomicInteger updates = new AtomicInteger();

        UiRuntimeMenuActions.bind(
                manager,
                updates::incrementAndGet);

        UiMenuItemRegistration handle = register("test:button");

        UiDispatcher.processPending();

        assertTrue(handle.isActive());

        assertEquals(
                List.of(
                        "test:button",
                        ROOT + ".back"),
                ids(manager));

        assertEquals(1, updates.get());

        handle.unregister();
        UiDispatcher.processPending();

        assertFalse(handle.isActive());

        assertEquals(
                List.of(ROOT + ".back"),
                ids(manager));

        assertEquals(2, updates.get());
    }

    @Test
    void duplicateRegistrationIsRejected() {

        UiRuntimeMenuActions.bind(
                manager(), () -> {
                });

        register("test:duplicate");

        assertThrows(
                IllegalArgumentException.class,
                () -> register("test:duplicate"));
    }

    @Test
    void registrationSurvivesMenuRegistryReload() {

        MenuManager first = manager();

        UiRuntimeMenuActions.bind(
                first, () -> {
                });

        UiMenuItemRegistration handle = register("test:persistent");

        UiDispatcher.processPending();

        assertTrue(
                ids(first).contains("test:persistent"));

        // Simulate Game.reloadGameplayMenus().
        MenuManager second = manager();

        UiRuntimeMenuActions.bind(
                second, () -> {
                });

        UiDispatcher.processPending();

        assertTrue(
                ids(second).contains("test:persistent"));

        assertFalse(
                ids(first).contains("test:persistent"));

        handle.unregister();
        UiDispatcher.processPending();

        assertFalse(
                ids(second).contains("test:persistent"));
    }

    @Test
    void registrationCanPrecedeMenuLoading() {

        UiRuntimeMenuActions.bind(
                null, () -> {
                });

        UiMenuItemRegistration handle = register("test:early");

        UiDispatcher.processPending();

        assertTrue(handle.isActive());

        MenuManager manager = manager();

        UiRuntimeMenuActions.bind(
                manager, () -> {
                });

        UiDispatcher.processPending();

        assertTrue(
                ids(manager).contains("test:early"));
    }

    @Test
    void unregisterBeforeDispatchDoesNotAddButton() {

        MenuManager manager = manager();

        UiRuntimeMenuActions.bind(
                manager, () -> {
                });

        UiMenuItemRegistration handle = register("test:cancelled");

        handle.unregister();

        UiDispatcher.processPending();

        assertFalse(
                ids(manager).contains("test:cancelled"));
    }
}
