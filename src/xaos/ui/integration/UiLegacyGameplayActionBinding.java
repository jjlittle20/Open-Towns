
package xaos.ui.integration;

import xaos.actions.ActionManager;
import xaos.actions.ActionManagerItem;
import xaos.main.Game;
import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.SmartMenu;
import xaos.tiles.Tile;

/**
 * Compatibility adapter for original Towns actions.
 *
 * Initially enables only QUEUEANDPLACEAREA.
 * Other command types will be added after testing.
 */
public final class UiLegacyGameplayActionBinding
        implements UiGameplayActionBinding {

    @Override
    public boolean isAvailable(
            MenuItemDefinition item) {

        if (item == null
                || !"action".equals(item.getType())) {

            return false;
        }

        // Begin with one known gameplay command.
        // Don't enable arbitrary commands supplied
        // through XML until their bindings are tested.
        if (!CommandPanel.COMMAND_QUEUE_AND_PLACE_AREA
                .equalsIgnoreCase(item.getCommand())) {

            return false;
        }

        String parameter = item.getParameter();

        if (parameter == null || parameter.isBlank()) {
            return false;
        }

        // Placement actions require an active world.
        if (Game.getWorld() == null) {
            return false;
        }

        // Only enable actions registered with Towns.
        return ActionManager.getItem(parameter) != null;
    }

    @Override
    public void execute(MenuItemDefinition item) {

        // Recheck availability at click time.
        if (!isAvailable(item)) {
            throw new IllegalStateException(
                    "Gameplay action is unavailable: "
                            + item.getId());
        }

        ActionManagerItem action = ActionManager.getItem(
                item.getParameter());

        // --------------------------------------------------------
        // Preserve legacy icon metadata
        // --------------------------------------------------------

        String iconId = item.getIcon();

        int iconType = SmartMenu.ICON_TYPE_UI;

        if (iconId == null || iconId.isBlank()) {

            // Match the original SmartMenu behaviour:
            // queued actions use their generated item
            // as the icon when available.
            iconId = action.getGeneratedItem();
            iconType = SmartMenu.ICON_TYPE_ITEM;
        }

        Tile icon = null;

        if (iconId != null && !iconId.isBlank()) {
            icon = new Tile(iconId);
        }

        // --------------------------------------------------------
        // Execute through the original dispatcher
        // --------------------------------------------------------

        CommandPanel.executeCommand(
                item.getCommand(),
                item.getParameter(),
                item.getParameter2(),
                null,
                icon,
                iconType);
    }
}
