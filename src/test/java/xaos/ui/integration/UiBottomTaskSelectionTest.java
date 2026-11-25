
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;
import xaos.tasks.Task;

class UiBottomTaskSelectionTest {

    private static MenuItemDefinition item(
            String command,
            String parameter) {

        return new MenuItemDefinition(
                "game.bottom.test",
                "action",
                null,
                null,
                command,
                false,
                true,
                null,
                null,
                null,
                null,
                true,
                "Test",
                null,
                parameter,
                null);
    }

    @Test
    void highlightsMatchingDigTask() {

        Task dig = new Task(Task.TASK_DIG);

        assertTrue(
                UiBottomTaskSelection.matches(
                        item("DIG", null),
                        dig));

        assertFalse(
                UiBottomTaskSelection.matches(
                        item("MINE", null),
                        dig));
    }

    @Test
    void distinguishesCustomActions() {

        Task task = new Task(Task.TASK_CUSTOM_ACTION);

        task.setParameter("qchop");

        assertTrue(
                UiBottomTaskSelection.matches(
                        item("CUSTOMACTION", "qchop"),
                        task));

        assertFalse(
                UiBottomTaskSelection.matches(
                        item("CUSTOMACTION", "qcut"),
                        task));
    }

    @Test
    void distinguishesStockpileTypes() {

        Task task = new Task(Task.TASK_STOCKPILE);

        task.setParameter("rawmaterials");

        assertTrue(
                UiBottomTaskSelection.matches(
                        item("STOCKPILE", "rawmaterials"),
                        task));

        assertFalse(
                UiBottomTaskSelection.matches(
                        item("STOCKPILE", "rawfood"),
                        task));
    }

    @Test
    void rejectsMissingTaskAndUnsupportedCommands() {

        assertFalse(
                UiBottomTaskSelection.matches(
                        item("DIG", null),
                        null));

        assertFalse(
                UiBottomTaskSelection.matches(
                        item("MOD_ACTION", "example:test"),
                        new Task(Task.TASK_DIG)));
    }

    @Test
    void stockpileSubmenuMatchesSelectedChildTask() {

        MenuManager menus = new MenuManager();

        MenuDefinition stockpiles = new MenuDefinition(
                "game.bottom.stockpiles",
                null,
                "Stockpiles");

        stockpiles.addItem(item(
                "STOCKPILE",
                "rawmaterials"));

        menus.registerMenu(stockpiles);

        MenuItemDefinition submenu = new MenuItemDefinition(
                "game.bottom.stockpiles",
                "submenu",
                null,
                "game.bottom.stockpiles",
                null,
                false,
                true,
                null,
                null,
                null,
                null,
                true,
                "Stockpiles",
                null,
                null,
                null);

        Task active = new Task(Task.TASK_STOCKPILE);

        active.setParameter("rawmaterials");

        assertTrue(
                UiBottomTaskSelection.matchesOrContains(
                        submenu, active, menus));

        active.setParameter("rawfood");

        assertFalse(
                UiBottomTaskSelection.matchesOrContains(
                        submenu, active, menus));
    }

    @Test
    void submenuIgnoresUnrelatedTaskTypes() {

        MenuManager menus = new MenuManager();

        MenuDefinition zones = new MenuDefinition(
                "game.bottom.zones",
                null,
                "Zones");

        zones.addItem(item(
                "CREATEZONE",
                "zcarpentry"));

        menus.registerMenu(zones);

        MenuItemDefinition submenu = new MenuItemDefinition(
                "game.bottom.zones",
                "submenu",
                null,
                "game.bottom.zones",
                null,
                false,
                true,
                null,
                null,
                null,
                null,
                true,
                "Zones",
                null,
                null,
                null);

        assertFalse(
                UiBottomTaskSelection.matchesOrContains(
                        submenu,
                        new Task(Task.TASK_DIG),
                        menus));
    }

}
