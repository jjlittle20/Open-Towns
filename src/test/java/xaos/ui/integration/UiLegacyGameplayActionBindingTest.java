
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.SmartMenu;

class UiLegacyGameplayActionBindingTest {

        private static MenuItemDefinition action(
                        String command,
                        String parameter,
                        String icon) {

                return new MenuItemDefinition(
                                "test.action",
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
                                null,
                                icon,
                                parameter,
                                null);
        }

        @Test
        void supportsAllThreeRightPanelCommands() {

                assertEquals(
                                CommandPanel.COMMAND_QUEUE_AND_PLACE_AREA,
                                UiLegacyGameplayActionBinding
                                                .canonicalCommand(
                                                                "QUEUEANDPLACEAREA"));

                assertEquals(
                                CommandPanel.COMMAND_QUEUE_AND_PLACE,
                                UiLegacyGameplayActionBinding
                                                .canonicalCommand(
                                                                "queueandplace"));

                assertEquals(
                                CommandPanel.COMMAND_BUILD,
                                UiLegacyGameplayActionBinding
                                                .canonicalCommand("BUILD"));

                assertNull(
                                UiLegacyGameplayActionBinding
                                                .canonicalCommand("CUSTOMACTION"));
        }

        @Test
        void queuedActionsUseActionDefinitions() {

                for (String command : new String[] {
                                "QUEUEANDPLACE",
                                "QUEUEANDPLACEAREA"
                }) {

                        MenuItemDefinition item = action(command, "qlogwall", null);

                        assertTrue(
                                        UiLegacyGameplayActionBinding
                                                        .canExecute(
                                                                        item,
                                                                        true,
                                                                        id -> id.equals("qlogwall"),
                                                                        id -> false));

                        assertFalse(
                                        UiLegacyGameplayActionBinding
                                                        .canExecute(
                                                                        item,
                                                                        true,
                                                                        id -> false,
                                                                        id -> true));
                }
        }

        @Test
        void buildUsesBuildingDefinitions() {

                MenuItemDefinition item = action("BUILD", "pigfarm", "ui_pigfarm");

                assertTrue(
                                UiLegacyGameplayActionBinding
                                                .canExecute(
                                                                item,
                                                                true,
                                                                id -> false,
                                                                id -> id.equals("pigfarm")));

                assertFalse(
                                UiLegacyGameplayActionBinding
                                                .canExecute(
                                                                item,
                                                                true,
                                                                id -> true,
                                                                id -> false));
        }

        @Test
        void actionsRequireActiveWorld() {

                MenuItemDefinition item = action("BUILD", "pigfarm", null);

                assertFalse(
                                UiLegacyGameplayActionBinding
                                                .canExecute(
                                                                item,
                                                                false,
                                                                id -> true,
                                                                id -> true));
        }

        @Test
        void unknownCommandsRemainDisabled() {

                MenuItemDefinition item = action(
                                "examplemod:custom-action",
                                "custom",
                                null);

                assertFalse(
                                UiLegacyGameplayActionBinding
                                                .canExecute(
                                                                item,
                                                                true,
                                                                id -> true,
                                                                id -> true));
        }

        @Test
        void queuedActionsUseGeneratedItemIcons() {

                MenuItemDefinition item = action(
                                "QUEUEANDPLACE",
                                "qlogwall",
                                null);

                var selection = UiLegacyGameplayActionBinding
                                .selectIcon(item, "logwall");

                assertEquals(
                                "logwall",
                                selection.iconId());

                assertEquals(
                                SmartMenu.ICON_TYPE_ITEM,
                                selection.iconType());
        }

        @Test
        void explicitXmlIconsTakePriority() {

                MenuItemDefinition item = action(
                                "BUILD",
                                "pigfarm",
                                "ui_pigfarm");

                var selection = UiLegacyGameplayActionBinding
                                .selectIcon(item, null);

                assertEquals(
                                "ui_pigfarm",
                                selection.iconId());

                assertEquals(
                                SmartMenu.ICON_TYPE_UI,
                                selection.iconType());
        }

        @Test
        void missingBuildingIconStaysNull() {

                MenuItemDefinition item = action("BUILD", "pigfarm", null);

                var selection = UiLegacyGameplayActionBinding
                                .selectIcon(item, null);

                assertNull(selection.iconId());
        }

        @Test
        void missingBuildingExplainsWhyUnavailable() {

                var item = action(
                                "BUILD", "missingbuilding", null);

                var result = UiLegacyGameplayActionBinding
                                .determineAvailability(
                                                item,
                                                true,
                                                id -> true,
                                                id -> false);

                assertFalse(result.canExecute());

                assertEquals(
                                UiActionAvailability.Status.UNAVAILABLE,
                                result.status());

                assertTrue(result.reason().contains(
                                "missingbuilding"));
        }

        @Test
        void unsupportedCommandProvidesReason() {

                var item = action(
                                "CUSTOMCOMMAND", "example", null);

                var result = UiLegacyGameplayActionBinding
                                .determineAvailability(
                                                item,
                                                true,
                                                id -> true,
                                                id -> true);

                assertFalse(result.canExecute());

                assertTrue(result.reason().contains(
                                "Unsupported gameplay command"));
        }

        @Test
        void existingActionRemainsAvailableWithoutStockCheck() {

                var item = action(
                                "QUEUEANDPLACEAREA", "qlogwall", null);

                var result = UiLegacyGameplayActionBinding
                                .determineAvailability(
                                                item,
                                                true,
                                                id -> id.equals("qlogwall"),
                                                id -> false);

                assertTrue(result.canExecute());

                assertEquals(
                                UiActionAvailability.Status.AVAILABLE,
                                result.status());

                assertEquals("", result.reason());
        }

        @Test
        void oldBindingsRemainCompatible() {

                UiGameplayActionBinding binding = new UiGameplayActionBinding() {

                        @Override
                        public boolean isAvailable(
                                        MenuItemDefinition item) {

                                return false;
                        }

                        @Override
                        public void execute(
                                        MenuItemDefinition item) {
                        }
                };

                var result = binding.availability(
                                action("BUILD", "pigfarm", null));

                assertEquals(
                                UiActionAvailability.Status.UNKNOWN,
                                result.status());

                assertFalse(result.canExecute());
        }

}
