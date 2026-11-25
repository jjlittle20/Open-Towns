
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

    private static List<String> productionActions(
            MenuManager manager,
            String menuId) {

        List<MenuItemDefinition> actions = manager.requireMenu(menuId)
                .getItems().stream()
                .filter(item -> "action".equals(item.getType()))
                .toList();

        assertTrue(actions.stream()
                .allMatch(item -> "QUEUE".equals(item.getCommand())));

        return actions.stream()
                .map(MenuItemDefinition::getParameter)
                .toList();
    }

    @Test
    void gatheringMenusHaveValidSubmenuTargets()
            throws Exception {

        MenuManager manager = loadMenus();

        assertDoesNotThrow(
                () -> manager.validateMenuTree(
                        "game.left.production.root"));

        MenuDefinition food = manager.requireMenu(
                "game.left.production.food");

        assertEquals(

                List.of(
                        "game.left.production.food.gathering",
                        "game.left.production.food.butchering",
                        "game.left.production.food.baking",
                        "game.left.production.food.cooking",
                        "game.left.production.food.back"),
                food.getItems().stream()
                        .map(MenuItemDefinition::getId)
                        .toList());

        MenuDefinition gathering = manager.resolveSubmenu(
                food.getItems().get(0));

        assertEquals(
                "game.left.production.food.gathering",
                gathering.getId());

        assertEquals(9, gathering.getItems().size());

        assertEquals(
                "game.left.production.food.gathering.trees",
                manager.resolveSubmenu(
                        gathering.getItems().get(0)).getId());

        assertEquals(
                "game.left.production.food.gathering.animals",
                manager.resolveSubmenu(
                        gathering.getItems().get(1)).getId());
    }

    @Test
    void gatheringActionsPreserveOriginalOrder()
            throws Exception {

        MenuManager manager = loadMenus();

        assertEquals(
                List.of(
                        "qharvestwheat",
                        "qharvestsugarcane",
                        "qharvestpapple",
                        "qharvestpbanana",
                        "qharvestppear",
                        "qharvestpblueradish",
                        "qharvestpsnowcherry",
                        "qpickpmushroom",
                        "qpickpcavemushroom",
                        "qharvestpcactus"),
                productionActions(
                        manager,
                        "game.left.production.food.gathering.trees"));

        assertEquals(
                List.of(
                        "qmilkcow",
                        "qmilkwildcow",
                        "qmilkdoe",
                        "qmilksnoat",
                        "qmilkwildsnoat",
                        "qmilkunifallow",
                        "qmilkwildunifallow",
                        "qgatherchickenegg",
                        "qgatherwildchickenegg",
                        "qgatherskootenegg",
                        "qgatherwildskootenegg"),
                productionActions(
                        manager,
                        "game.left.production.food.gathering.animals"));

        assertEquals(
                List.of(
                        "qfishing",
                        "qflour",
                        "qsugar",
                        "qjunglesalad",
                        "qgathersnowpile",
                        "qwaterbucket"),
                productionActions(
                        manager,
                        "game.left.production.food.gathering"));

        MenuDefinition gathering = manager.requireMenu(
                "game.left.production.food.gathering");

        assertEquals(
                "back",
                gathering.getItems().get(
                        gathering.getItems().size() - 1).getType());
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

        MenuItemDefinition bakingLink = food.getItems()
                .stream()
                .filter(item -> "game.left.production.food.baking"
                        .equals(item.getTargetMenu()))
                .findFirst()
                .orElseThrow();

        MenuDefinition baking = manager.resolveSubmenu(bakingLink);

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

    @Test
    void foodCategoryMatchesOriginalOrder()
            throws Exception {

        MenuManager manager = loadMenus();

        assertDoesNotThrow(
                () -> manager.validateMenuTree(
                        "game.left.production.root"));

        MenuDefinition food = manager.requireMenu(
                "game.left.production.food");

        assertEquals(
                List.of(
                        "game.left.production.food.gathering",
                        "game.left.production.food.butchering",
                        "game.left.production.food.baking",
                        "game.left.production.food.cooking",
                        "game.left.production.food.back"),
                food.getItems().stream()
                        .map(MenuItemDefinition::getId)
                        .toList());

        assertEquals(
                "back",
                food.getItems().get(
                        food.getItems().size() - 1).getType());
    }

    @Test
    void butcheringActionsPreserveOriginalOrder()
            throws Exception {

        MenuManager manager = loadMenus();

        assertEquals(
                List.of(
                        "qbutcherbadger",
                        "qbutcherpoultry",
                        "qbutchercow",
                        "qbutcherpig",
                        "qbutchersnoat",
                        "qbutcherforbones"),
                productionActions(
                        manager,
                        "game.left.production.food.butchering"));
    }

    @Test
    void cookingActionsPreserveOriginalOrder()
            throws Exception {

        MenuManager manager = loadMenus();

        MenuDefinition cooking = manager.requireMenu(
                "game.left.production.food.cooking");

        // Meat must be the first item.
        MenuItemDefinition meatLink = cooking.getItems().get(0);

        assertEquals(
                "submenu",
                meatLink.getType());

        assertEquals(
                "game.left.production.food.cooking.meat",
                meatLink.getTargetMenu());

        assertEquals(
                "ui_meat",
                meatLink.getIcon());

        assertEquals(
                List.of(
                        "qomelette",
                        "qbakedbanana",
                        "qhamandeggs",
                        "qsoup",
                        "qcactussoup",
                        "qmountainstew"),
                productionActions(
                        manager,
                        "game.left.production.food.cooking"));

        assertEquals(
                "back",
                cooking.getItems().get(
                        cooking.getItems().size() - 1).getType());
    }

    @Test
    void meatActionsPreserveOriginalOrder()
            throws Exception {

        MenuManager manager = loadMenus();

        assertEquals(
                List.of(
                        "qcookedbadgermeat",
                        "qcookedpoultry",
                        "qcookedpork",
                        "qcookedsteak",
                        "qroast",
                        "qcookedfish"),
                productionActions(
                        manager,
                        "game.left.production.food.cooking.meat"));

        MenuDefinition meat = manager.requireMenu(
                "game.left.production.food.cooking.meat");

        assertEquals(
                "back",
                meat.getItems().get(
                        meat.getItems().size() - 1).getType());
    }

}
