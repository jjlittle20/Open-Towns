
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

    @Test
    void xmlItemCollisionDoesNotReplaceExistingButton() {

        MenuManager manager = manager();

        MenuItemDefinition xmlItem = new MenuItemDefinition(
                "test:collision",
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
                "XML Button",
                null,
                "test:callback",
                null);

        manager.requireMenu(ROOT).addItem(xmlItem);

        UiRuntimeMenuActions.bind(
                manager, () -> {
                });

        // Java contribution uses the same ID
        // as the XML-defined button.
        UiMenuItemRegistration handle = register("test:collision");

        UiDispatcher.processPending();

        // The Java registration must be invalidated.
        assertFalse(handle.isActive());

        // The original XML entry must be preserved.
        List<MenuItemDefinition> matching = manager.requireMenu(ROOT)
                .getItems()
                .stream()
                .filter(item -> item.getId()
                        .equals("test:collision"))
                .toList();

        assertEquals(1, matching.size());
        assertSame(xmlItem, matching.get(0));
    }

    @Test
    void twoModsCanAddAndRemoveButtonsIndependently() {

        MenuManager manager = manager();

        UiRuntimeMenuActions.bind(
                manager, () -> {
                });

        UiMenuItemRegistration first = register("mod-a:hello");

        UiMenuItemRegistration second = register("mod-b:hello");

        UiDispatcher.processPending();

        assertTrue(first.isActive());
        assertTrue(second.isActive());

        assertTrue(ids(manager).contains(
                "mod-a:hello"));

        assertTrue(ids(manager).contains(
                "mod-b:hello"));

        // Simulate Mod A unloading.
        first.unregister();

        UiDispatcher.processPending();

        assertFalse(
                ids(manager).contains("mod-a:hello"));

        // Mod B must remain untouched.
        assertTrue(second.isActive());

        assertTrue(
                ids(manager).contains("mod-b:hello"));

        // Simulate Mod B unloading.
        second.unregister();

        UiDispatcher.processPending();

        assertFalse(
                ids(manager).contains("mod-b:hello"));
    }

}
