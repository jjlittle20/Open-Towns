
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

        return availability(item).canExecute();
    }

    @Override
    public UiActionAvailability availability(
            MenuItemDefinition item) {

        return determineAvailability(
                item,
                Game.getWorld() != null,
                id -> ActionManager.getItem(id) != null,
                id -> BuildingManager.getItem(id) != null);
    }

    /**
     * Preserves the existing boolean helper used
     * by earlier tests.
     */
    static boolean canExecute(
            MenuItemDefinition item,
            boolean worldLoaded,
            Predicate<String> actionExists,
            Predicate<String> buildingExists) {

        return determineAvailability(
                item,
                worldLoaded,
                actionExists,
                buildingExists).canExecute();
    }

    /**
     * Converts legacy availability checks into
     * explanatory results.
     *
     * Only checks supported commands, loaded world
     * and definition existence.
     *
     * Missing construction materials are NOT
     * grounds for disabling an action.
     */
    static UiActionAvailability determineAvailability(
            MenuItemDefinition item,
            boolean worldLoaded,
            Predicate<String> actionExists,
            Predicate<String> buildingExists) {

        if (item == null) {

            return UiActionAvailability.blocked(
                    "Invalid gameplay action.");
        }

        if (!"action".equals(item.getType())) {

            return UiActionAvailability.blocked(
                    "This menu entry is not an action.");
        }

        if (!worldLoaded) {

            return UiActionAvailability.blocked(
                    "No game world is loaded.");
        }

        String command = canonicalCommand(item.getCommand());

        if (command == null) {

            return UiActionAvailability.blocked(
                    "Unsupported gameplay command: "
                            + item.getCommand());
        }

        String id = item.getParameter();

        if (!hasText(id)) {

            return UiActionAvailability.blocked(
                    "Missing gameplay definition ID.");
        }

        if (CommandPanel.COMMAND_BUILD.equals(command)) {

            if (!buildingExists.test(id)) {

                return UiActionAvailability.blocked(
                        "Building definition not found: "
                                + id);
            }

            return UiActionAvailability.allowed();
        }

        if (!actionExists.test(id)) {

            return UiActionAvailability.blocked(
                    "Construction action not found: "
                            + id);
        }

        return UiActionAvailability.allowed();
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
