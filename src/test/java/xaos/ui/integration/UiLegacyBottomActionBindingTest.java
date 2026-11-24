
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.SmartMenu;

class UiLegacyBottomActionBindingTest {

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
                "Test Action",
                "ui_test",
                parameter,
                null);
    }

    @Test
    void supportsEveryOriginalBottomCommand() {

        assertTrue(
                UiLegacyBottomActionBinding.canExecute(
                        item("CUSTOMACTION", "qchop"),
                        true));

        assertTrue(
                UiLegacyBottomActionBinding.canExecute(
                        item("DIG", null),
                        true));

        assertTrue(
                UiLegacyBottomActionBinding.canExecute(
                        item("MINE", null),
                        true));

        assertTrue(
                UiLegacyBottomActionBinding.canExecute(
                        item("STOCKPILE", "rawmaterials"),
                        true));

        assertTrue(
                UiLegacyBottomActionBinding.canExecute(
                        item("CREATEZONE", "zcarpentry"),
                        true));

        assertTrue(
                UiLegacyBottomActionBinding.canExecute(
                        item("CANCELORDER", null),
                        true));
    }

    @Test
    void commandsAreCaseInsensitive() {

        assertEquals(
                CommandPanel.COMMAND_MINE,
                UiLegacyBottomActionBinding
                        .canonicalCommand("mine"));

        assertEquals(
                CommandPanel.COMMAND_CUSTOM_ACTION,
                UiLegacyBottomActionBinding
                        .canonicalCommand("customaction"));
    }

    @Test
    void commaSeparatedActionsRemainUnchanged() {

        String parameter = "qchop,qchopplantedtree,qchopplantedtree2";

        var request = UiLegacyBottomActionBinding.commandRequest(
                item("CUSTOMACTION", parameter));

        assertEquals(
                CommandPanel.COMMAND_CUSTOM_ACTION,
                request.command());

        assertEquals(
                parameter,
                request.parameter());

        assertEquals(
                "ui_test",
                request.iconId());

        assertEquals(
                SmartMenu.ICON_TYPE_UI,
                request.iconType());
    }

    @Test
    void parameterlessSelectionCommandsAreValid() {

        for (String command : new String[] {
                "DIG",
                "MINE",
                "CANCELORDER"
        }) {

            assertTrue(
                    UiLegacyBottomActionBinding.canExecute(
                            item(command, null),
                            true));
        }
    }

    @Test
    void requiredParametersCannotBeMissing() {

        for (String command : new String[] {
                "CUSTOMACTION",
                "STOCKPILE",
                "CREATEZONE"
        }) {

            assertFalse(
                    UiLegacyBottomActionBinding.canExecute(
                            item(command, null),
                            true));
        }
    }

    @Test
    void actionsRequireLoadedWorld() {

        assertFalse(
                UiLegacyBottomActionBinding.canExecute(
                        item("DIG", null),
                        false));

        assertFalse(
                UiLegacyBottomActionBinding.canExecute(
                        item("CUSTOMACTION", "qchop"),
                        false));
    }

    @Test
    void rejectsUnrelatedAndUnknownCommands() {

        assertFalse(
                UiLegacyBottomActionBinding.canExecute(
                        item("BUILD", "qhouse"),
                        true));

        assertFalse(
                UiLegacyBottomActionBinding.canExecute(
                        item("MOD_ACTION", "example:hello"),
                        true));

        assertFalse(
                UiLegacyBottomActionBinding.canExecute(
                        item("UNKNOWN", null),
                        true));

        assertFalse(
                UiLegacyBottomActionBinding.canExecute(
                        null,
                        true));
    }

    @Test
    void preservesSecondCommandParameter() {

        MenuItemDefinition item = new MenuItemDefinition(
                "game.bottom.test",
                "action",
                null,
                null,
                "CUSTOMACTION",
                false,
                true,
                null,
                null,
                null,
                null,
                true,
                "Test",
                "ui_test",
                "qchop",
                "extra");

        var request = UiLegacyBottomActionBinding.commandRequest(
                item);

        assertEquals(
                "qchop",
                request.parameter());

        assertEquals(
                "extra",
                request.parameter2());
    }
}
