
package xaos.ui.integration;

import xaos.main.Game;
import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.SmartMenu;
import xaos.tiles.Tile;

/**
 * Compatibility adapter for original Towns
 * bottom-toolbar gameplay commands.
 *
 * Reuses CommandPanel rather than recreating
 * task selection or placement logic.
 */
public final class UiLegacyBottomActionBinding
        implements UiGameplayActionBinding {

    /**
     * Validated command data, separate from
     * actual gameplay execution.
     */
    record CommandRequest(
            String command,
            String parameter,
            String parameter2,
            String iconId,
            int iconType) {
    }

    @Override
    public boolean isAvailable(MenuItemDefinition item) {

        return canExecute(
                item,
                Game.getWorld() != null);
    }

    /**
     * Pure validation helper for unit tests.
     */
    static boolean canExecute(
            MenuItemDefinition item,
            boolean worldLoaded) {

        if (!worldLoaded
                || item == null
                || !"action".equals(item.getType())) {

            return false;
        }

        String command = canonicalCommand(
                item.getCommand());

        if (command == null) {
            return false;
        }

        // These commands need a parameter
        // identifying their type or action.
        if (requiresParameter(command)
                && !hasText(item.getParameter())) {

            return false;
        }

        return true;
    }

    /**
     * Only forward commands supported by
     * the original bottom gameplay toolbar.
     */
    static String canonicalCommand(String command) {

        if (command == null) {
            return null;
        }

        if (CommandPanel.COMMAND_CUSTOM_ACTION
                .equalsIgnoreCase(command)) {

            return CommandPanel.COMMAND_CUSTOM_ACTION;
        }

        if (CommandPanel.COMMAND_DIG
                .equalsIgnoreCase(command)) {

            return CommandPanel.COMMAND_DIG;
        }

        if (CommandPanel.COMMAND_MINE
                .equalsIgnoreCase(command)) {

            return CommandPanel.COMMAND_MINE;
        }

        if (CommandPanel.COMMAND_STOCKPILE
                .equalsIgnoreCase(command)) {

            return CommandPanel.COMMAND_STOCKPILE;
        }

        if (CommandPanel.COMMAND_CREATE_ZONE
                .equalsIgnoreCase(command)) {

            return CommandPanel.COMMAND_CREATE_ZONE;
        }

        if (CommandPanel.COMMAND_CANCEL_ORDER
                .equalsIgnoreCase(command)) {

            return CommandPanel.COMMAND_CANCEL_ORDER;
        }

        return null;
    }

    private static boolean requiresParameter(
            String command) {

        return CommandPanel.COMMAND_CUSTOM_ACTION
                .equals(command)
                || CommandPanel.COMMAND_STOCKPILE
                        .equals(command)
                || CommandPanel.COMMAND_CREATE_ZONE
                        .equals(command);
    }

    /**
     * Builds the original command arguments.
     *
     * Important: comma-separated CUSTOMACTION
     * parameters are forwarded unchanged.
     */
    static CommandRequest commandRequest(
            MenuItemDefinition item) {

        if (!canExecute(item, true)) {
            throw new IllegalArgumentException(
                    "Unsupported bottom toolbar action: "
                            + (item == null
                                    ? "null"
                                    : item.getId()));
        }

        return new CommandRequest(
                canonicalCommand(item.getCommand()),
                item.getParameter(),
                item.getParameter2(),
                item.getIcon(),
                SmartMenu.ICON_TYPE_UI);
    }

    @Override
    public void execute(MenuItemDefinition item) {

        // Recheck gameplay state before executing.
        if (!isAvailable(item)) {
            throw new IllegalStateException(
                    "Bottom toolbar action unavailable: "
                            + (item == null
                                    ? "null"
                                    : item.getId()));
        }

        CommandRequest request = commandRequest(item);

        Tile icon = null;

        if (hasText(request.iconId())) {
            icon = new Tile(request.iconId());
        }

        // The original command dispatcher creates
        // the game's selection/placement task.
        CommandPanel.executeCommand(
                request.command(),
                request.parameter(),
                request.parameter2(),
                null,
                icon,
                request.iconType());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
