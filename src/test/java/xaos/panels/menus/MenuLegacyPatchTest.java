
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuLegacyPatch.*;

class MenuLegacyPatchTest {

    private static final String ROOT = "game.right.root";

    private static MenuItemDefinition item(
            String id,
            String title,
            String type) {

        return new MenuItemDefinition(
                id,
                type,
                null,
                null,
                null,
                false,
                true,
                null,
                null,
                null,
                null,
                true,
                title,
                null,
                null,
                null);
    }

    private static MenuManager manager() {

        MenuManager manager = new MenuManager();

        MenuDefinition menu = new MenuDefinition(
                ROOT,
                null,
                "Construction");

        menu.addItem(
                item("game.right.walls",
                        "Walls", "action"));

        menu.addItem(
                item("game.right.furniture",
                        "Furniture", "action"));

        menu.addItem(
                item("game.right.back",
                        "Back", "back"));

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
    void deleteRemovesOnlySpecifiedItem() {

        MenuManager manager = manager();

        manager.applyLegacyPatch(
                new MenuLegacyPatch(
                        ROOT,
                        List.of(
                                new Remove(
                                        "GAME.RIGHT.FURNITURE"))));

        assertEquals(
                List.of(
                        "game.right.walls",
                        "game.right.back"),
                ids(manager));
    }

    @Test
    void newItemIsInsertedBeforeBack() {

        MenuManager manager = manager();

        manager.applyLegacyPatch(
                new MenuLegacyPatch(
                        ROOT,
                        List.of(
                                new Add(
                                        item(
                                                "game.right.mod.item",
                                                "Mod Item",
                                                "action")))));

        assertEquals(
                List.of(
                        "game.right.walls",
                        "game.right.furniture",
                        "game.right.mod.item",
                        "game.right.back"),
                ids(manager));
    }

    @Test
    void clearRemovesEntireMenuContents() {

        MenuManager manager = manager();

        manager.applyLegacyPatch(
                new MenuLegacyPatch(
                        ROOT,
                        List.of(new Clear())));

        assertTrue(ids(manager).isEmpty());

        assertTrue(
                manager.requireMenu(ROOT)
                        .getContent()
                        .isEmpty());
    }

    @Test
    void replacementPreservesItemPosition() {

        MenuManager manager = manager();

        manager.applyLegacyPatch(
                new MenuLegacyPatch(
                        ROOT,
                        List.of(
                                new Replace(
                                        "game.right.walls",
                                        item(
                                                "game.right.walls",
                                                "Modified Walls",
                                                "action")))));

        assertEquals(
                "Modified Walls",
                manager.requireMenu(ROOT)
                        .getItems()
                        .get(0)
                        .getTitle());

        assertEquals(
                "game.right.furniture",
                ids(manager).get(1));
    }

    @Test
    void failedPatchDoesNotPartiallyModifyMenu() {

        MenuManager manager = manager();

        MenuDefinition original = manager.requireMenu(ROOT);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.applyLegacyPatch(
                        new MenuLegacyPatch(
                                ROOT,
                                List.of(
                                        new Add(
                                                item(
                                                        "game.right.new",
                                                        "New Item",
                                                        "action")),
                                        new Remove(
                                                "game.right.missing")))));

        // The original definition remains registered.
        assertSame(
                original,
                manager.requireMenu(ROOT));

        assertEquals(
                List.of(
                        "game.right.walls",
                        "game.right.furniture",
                        "game.right.back"),
                ids(manager));
    }

    @Test
    void removeThenAddCanReplaceLegacyEntry() {

        MenuManager manager = manager();

        manager.applyLegacyPatch(
                new MenuLegacyPatch(
                        ROOT,
                        List.of(
                                new Remove(
                                        "game.right.furniture"),
                                new Add(
                                        item(
                                                "game.right.furniture",
                                                "Revised Furniture",
                                                "action")))));

        assertEquals(
                "Revised Furniture",
                manager.requireMenu(ROOT)
                        .getItems()
                        .get(1)
                        .getTitle());

        assertEquals(
                "game.right.back",
                ids(manager).get(2));
    }
}
