
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class MenuProductionMigrationTest {

    private static MenuManager loadMenus()
            throws Exception {

        URL resource = MenuProductionMigrationTest.class
                .getClassLoader()
                .getResource(
                        "data/menus/left/production/root.xml");

        assertNotNull(resource);

        Path directory = Path.of(
                resource.toURI()).getParent();

        MenuManager manager = new MenuManager();

        manager.loadMenus(directory.toFile());

        return manager;
    }

    @Test
    void productionBranchHasValidHierarchy()
            throws Exception {

        MenuManager manager = loadMenus();

        assertDoesNotThrow(
                () -> manager.validateMenuTree(
                        "game.left.production.root"));

        MenuDefinition root = manager.requireMenu(
                "game.left.production.root");

        MenuDefinition food = manager.resolveSubmenu(
                root.getItems().get(0));

        MenuDefinition baking = manager.resolveSubmenu(
                food.getItems().get(0));

        assertEquals(
                "game.left.production.food",
                food.getId());

        assertEquals(
                "game.left.production.food.baking",
                baking.getId());
    }

    @Test
    void bakingActionsPreserveOriginalOrder()
            throws Exception {

        MenuManager manager = loadMenus();

        MenuDefinition baking = manager.requireMenu(
                "game.left.production.food.baking");

        List<MenuItemDefinition> actions = baking.getItems().stream()
                .filter(item -> "action".equals(item.getType()))
                .toList();

        assertEquals(9, actions.size());

        assertEquals(
                List.of(
                        "qapplepie",
                        "qbread",
                        "qcake",
                        "qbananabread",
                        "qmeatpie",
                        "qpearpie",
                        "qsnowcherrypie",
                        "qbananapie",
                        "qcookie"),
                actions.stream()
                        .map(MenuItemDefinition::getParameter)
                        .toList());

        assertTrue(actions.stream()
                .allMatch(item -> "QUEUE".equals(item.getCommand())));

        assertEquals(
                "back",
                baking.getItems()
                        .get(baking.getItems().size() - 1)
                        .getType());
    }
}
