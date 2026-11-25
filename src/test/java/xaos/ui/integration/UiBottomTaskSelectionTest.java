
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuItemDefinition;
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
}
