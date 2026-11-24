
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class MenuRuntimeItemTest {

    private static final String ROOT = "game.right.root";

    private static MenuItemDefinition action(
            String id) {

        return new MenuItemDefinition(
                id,
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
                id,
                null,
                id,
                null);
    }

    private static MenuItemDefinition back() {

        return new MenuItemDefinition(
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
                true);
    }

    private static MenuManager manager() {

        MenuManager manager = new MenuManager();

        MenuDefinition menu = new MenuDefinition(
                ROOT, null, "Construction");

        menu.addItem(action("game:original"));
        menu.addItem(back());

        manager.registerMenu(menu);

        return manager;
    }

    private static List<String> ids(
            MenuManager manager) {

        return manager.requireMenu(ROOT)
                .getItems()
                .stream()
                .map(MenuItemDefinition::getId)
                .toList();
    }

    @Test
    void runtimeItemAppearsBeforeBack() {

        MenuManager manager = manager();

        manager.addRuntimeItem(
                ROOT,
                action("mod-a:hello"));

        assertEquals(
                List.of(
                        "game:original",
                        "mod-a:hello",
                        ROOT + ".back"),
                ids(manager));
    }

    @Test
    void unloadingOneModPreservesOtherItems() {

        MenuManager manager = manager();

        MenuItemDefinition first = action("mod-a:hello");

        MenuItemDefinition second = action("mod-b:hello");

        manager.addRuntimeItem(ROOT, first);
        manager.addRuntimeItem(ROOT, second);

        assertTrue(
                manager.removeRuntimeItem(ROOT, first));

        assertEquals(
                List.of(
                        "game:original",
                        "mod-b:hello",
                        ROOT + ".back"),
                ids(manager));
    }

    @Test
    void duplicateIdDoesNotModifyMenu() {

        MenuManager manager = manager();

        MenuItemDefinition original = action("mod-a:hello");

        manager.addRuntimeItem(ROOT, original);

        MenuDefinition before = manager.requireMenu(ROOT);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.addRuntimeItem(
                        ROOT,
                        action("mod-a:hello")));

        assertSame(
                before,
                manager.requireMenu(ROOT));
    }

    @Test
    void staleRegistrationCannotRemoveReplacement() {

        MenuManager manager = manager();

        MenuItemDefinition old = action("mod-a:hello");

        manager.addRuntimeItem(ROOT, old);

        assertTrue(
                manager.removeRuntimeItem(ROOT, old));

        MenuItemDefinition replacement = action("mod-a:hello");

        manager.addRuntimeItem(ROOT, replacement);

        // The original handle no longer owns it.
        assertFalse(
                manager.removeRuntimeItem(ROOT, old));

        assertTrue(
                ids(manager).contains("mod-a:hello"));

        assertTrue(
                manager.removeRuntimeItem(
                        ROOT, replacement));
    }

    @Test
    void runtimeEntriesPreserveExistingTabs() {

        MenuManager manager = new MenuManager();

        MenuDefinition menu = new MenuDefinition(
                ROOT, null, "Construction");

        MenuTabDefinition tab = new MenuTabDefinition(
                "general",
                null,
                "General",
                List.of(action("game:inside-tab")));

        MenuTabsDefinition tabs = new MenuTabsDefinition(
                "game:tabs",
                MenuTabsDefinition.Placement.TOP,
                List.of(tab));

        menu.addTabs(tabs);
        menu.addItem(back());

        manager.registerMenu(menu);

        MenuItemDefinition added = action("mod-a:hello");

        manager.addRuntimeItem(ROOT, added);

        assertSame(
                tabs,
                manager.requireMenu(ROOT)
                        .getContent().get(0));

        assertTrue(
                manager.requireMenu(ROOT)
                        .getItems().contains(added));

        manager.removeRuntimeItem(ROOT, added);

        assertSame(
                tabs,
                manager.requireMenu(ROOT)
                        .getContent().get(0));

        assertEquals(
                List.of(
                        "game:inside-tab",
                        ROOT + ".back"),
                ids(manager));
    }
}
