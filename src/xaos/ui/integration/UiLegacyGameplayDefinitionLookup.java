
package xaos.ui.integration;

import java.util.function.BiFunction;

import xaos.actions.ActionManager;
import xaos.actions.ActionManagerItem;
import xaos.panels.CommandPanel;

import xaos.tiles.entities.buildings.BuildingManager;
import xaos.tiles.entities.buildings.BuildingManagerItem;

import xaos.tiles.entities.items.ItemManager;
import xaos.tiles.entities.items.ItemManagerItem;

import xaos.zones.ZoneManager;
import xaos.zones.ZoneManagerItem;

import xaos.ui.integration.UiGameplayMenuResolver.DefinitionInfo;

/**
 * Reads gameplay menu presentation data from
 * the original Towns definition managers.
 *
 * This is an internal compatibility adapter.
 */
public final class UiLegacyGameplayDefinitionLookup
        implements BiFunction<String, String, DefinitionInfo> {

    @Override
    public DefinitionInfo apply(
            String command,
            String parameter) {

        if (command == null
                || parameter == null
                || parameter.isBlank()) {

            return null;
        }

        // --------------------------------------------------------
        // Queued actions
        // --------------------------------------------------------

        if (matches(command,
                CommandPanel.COMMAND_QUEUE,
                CommandPanel.COMMAND_QUEUE_AND_PLACE,
                CommandPanel.COMMAND_QUEUE_AND_PLACE_ROW,
                CommandPanel.COMMAND_QUEUE_AND_PLACE_AREA)) {

            ActionManagerItem action = ActionManager.getItem(parameter);

            if (action == null) {
                return null;
            }

            return new DefinitionInfo(
                    action.getName(),
                    action.getGeneratedItem());
        }

        // --------------------------------------------------------
        // Other custom actions
        // --------------------------------------------------------

        if (matches(command,
                CommandPanel.COMMAND_CUSTOM_ACTION)) {

            ActionManagerItem action = ActionManager.getItem(parameter);

            if (action == null) {
                return null;
            }

            // Original SmartMenu derives the name,
            // but not necessarily an item icon.
            return new DefinitionInfo(
                    action.getName(),
                    null);
        }

        // --------------------------------------------------------
        // Item creation
        // --------------------------------------------------------

        if (matches(command,
                CommandPanel.COMMAND_CREATE,
                CommandPanel.COMMAND_CREATE_AND_PLACE,
                CommandPanel.COMMAND_CREATE_AND_PLACE_ROW,
                CommandPanel.COMMAND_CREATE_IN_A_BUILDING)) {

            ItemManagerItem item = ItemManager.getItem(parameter);

            if (item == null) {
                return null;
            }

            return new DefinitionInfo(
                    item.getName(),
                    parameter);
        }

        // --------------------------------------------------------
        // Buildings
        // --------------------------------------------------------

        if (matches(command,
                CommandPanel.COMMAND_BUILD)) {

            BuildingManagerItem building = BuildingManager.getItem(parameter);

            if (building == null) {
                return null;
            }

            return new DefinitionInfo(
                    building.getName(),
                    null);
        }

        // --------------------------------------------------------
        // Zones
        // --------------------------------------------------------

        if (matches(command,
                CommandPanel.COMMAND_CREATE_ZONE)) {

            ZoneManagerItem zone = ZoneManager.getItem(parameter);

            if (zone == null) {
                return null;
            }

            return new DefinitionInfo(
                    zone.getName(),
                    null);
        }

        // Unknown commands are not resolved here.
        // Future mod action providers may handle them.
        return null;
    }

    private static boolean matches(
            String actual,
            String... commands) {

        for (String command : commands) {
            if (command.equalsIgnoreCase(actual)) {
                return true;
            }
        }

        return false;
    }
}
