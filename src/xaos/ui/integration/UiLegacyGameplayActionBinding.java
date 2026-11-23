
package xaos.ui.integration;

import java.util.function.Predicate;

import xaos.actions.ActionManager;
import xaos.actions.ActionManagerItem;
import xaos.main.Game;
import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.SmartMenu;
import xaos.tiles.Tile;
import xaos.tiles.entities.buildings.BuildingManager;

/**
 * Compatibility binding for the original
 * right-hand gameplay menu commands.
 *
 * Supports:
 * - QUEUEANDPLACE
 * - QUEUEANDPLACEAREA
 * - BUILD
 *
 * Uses the existing CommandPanel dispatcher.
 */
public final class UiLegacyGameplayActionBinding
        implements UiGameplayActionBinding {

    @Override
    public boolean isAvailable(MenuItemDefinition item) {

        return canExecute(
                item,
                Game.getWorld() != null,
                id -> ActionManager.getItem(id) != null,
                id -> BuildingManager.getItem(id) != null);
    }

    /**
     * Separates availability decisions from
     * game state so they can be unit tested.
     *
     * This checks definition existence, not
     * resource or building prerequisites.
     */
    static boolean canExecute(
            MenuItemDefinition item,
            boolean worldLoaded,
            Predicate<String> actionExists,
            Predicate<String> buildingExists) {

        if (item == null
                || !"action".equals(item.getType())
                || !worldLoaded
                || !hasText(item.getParameter())) {

            return false;
        }

        String command = canonicalCommand(item.getCommand());

        if (command == null) {
            return false;
        }

        if (CommandPanel.COMMAND_BUILD.equals(command)) {
            return buildingExists.test(
                    item.getParameter());
        }

        return actionExists.test(
                item.getParameter());
    }

    /**
     * Only explicitly supported commands can
     * be passed into the legacy dispatcher.
     */
    static String canonicalCommand(String command) {

        if (CommandPanel.COMMAND_QUEUE_AND_PLACE_AREA
                .equalsIgnoreCase(command)) {

            return CommandPanel.COMMAND_QUEUE_AND_PLACE_AREA;
        }

        if (CommandPanel.COMMAND_QUEUE_AND_PLACE
                .equalsIgnoreCase(command)) {

            return CommandPanel.COMMAND_QUEUE_AND_PLACE;
        }

        if (CommandPanel.COMMAND_BUILD
                .equalsIgnoreCase(command)) {

            return CommandPanel.COMMAND_BUILD;
        }

        return null;
    }

    /**
     * Icon metadata forwarded to the original
     * task/placement command handlers.
     */
    record IconSelection(
            String iconId,
            int iconType) {
    }

    /**
     * Match the original SmartMenu icon rules.
     *
     * Explicit XML icons use ICON_TYPE_UI.
     * Queued actions without an explicit icon
     * use their generated item's icon.
     *
     * Buildings don't automatically use the
     * building ID as an item icon.
     */
    static IconSelection selectIcon(
            MenuItemDefinition item,
            String generatedItem) {

        if (hasText(item.getIcon())) {

            return new IconSelection(
                    item.getIcon(),
                    SmartMenu.ICON_TYPE_UI);
        }

        String command = canonicalCommand(item.getCommand());

        if ((CommandPanel.COMMAND_QUEUE_AND_PLACE
                .equals(command)
                || CommandPanel.COMMAND_QUEUE_AND_PLACE_AREA
                        .equals(command))
                && hasText(generatedItem)) {

            return new IconSelection(
                    generatedItem,
                    SmartMenu.ICON_TYPE_ITEM);
        }

        return new IconSelection(
                null,
                SmartMenu.ICON_TYPE_UI);
    }

    @Override
    public void execute(MenuItemDefinition item) {

        // The state or definition may have changed
        // since the button was last rendered.
        if (!isAvailable(item)) {

            throw new IllegalStateException(
                    "Gameplay action unavailable: "
                            + (item == null
                                    ? "null"
                                    : item.getId()));
        }

        String command = canonicalCommand(item.getCommand());

        String generatedItem = null;

        // Both queued placement types use
        // definitions from ActionManager.
        if (!CommandPanel.COMMAND_BUILD.equals(command)) {

            ActionManagerItem action = ActionManager.getItem(
                    item.getParameter());

            if (action != null) {
                generatedItem = action.getGeneratedItem();
            }
        }

        IconSelection selection = selectIcon(item, generatedItem);

        Tile icon = null;

        if (hasText(selection.iconId())) {
            icon = new Tile(selection.iconId());
        }

        // Leave task creation, placement selection
        // and building behaviour to the original
        // Towns command dispatcher.
        CommandPanel.executeCommand(
                command,
                item.getParameter(),
                item.getParameter2(),
                null,
                icon,
                selection.iconType());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
