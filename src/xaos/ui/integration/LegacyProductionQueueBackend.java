
package xaos.ui.integration;

import java.util.Objects;

import xaos.campaign.TutorialTrigger;
import xaos.main.Game;
import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.SmartMenu;

/**
 * Bridges production menu definitions to legacy gameplay.
 */
public final class LegacyProductionQueueBackend
        implements UiProductionQueueAdapter.Backend {

    private final MenuItemDefinition item;

    public LegacyProductionQueueBackend(MenuItemDefinition item) {
        this.item = Objects.requireNonNull(item, "item");
    }

    @Override
    public void execute(UiProductionQueueAdapter.Action action) {

        String parameter = item.getParameter();

        switch (action) {

            case ADD_REGULAR:
                CommandPanel.executeCommand(
                        item.getCommand(),
                        parameter,
                        item.getParameter2(),
                        null,
                        null,
                        SmartMenu.ICON_TYPE_UI);
                break;

            case REMOVE_REGULAR:
                Game.getWorld().getTaskManager()
                        .removeFromQueue(parameter);
                break;

            case INCREASE_AUTOMATED:
                Game.getWorld().getTaskManager()
                        .addItemOnAutomatedQueue(parameter);
                break;

            case DECREASE_AUTOMATED:
                Game.getWorld().getTaskManager()
                        .removeItemOnAutomatedQueue(parameter);
                break;

            default:
                throw new IllegalArgumentException(
                        "Unsupported production action: " + action);
        }
    }

    @Override
    public void tutorial(UiProductionQueueAdapter.Action action) {

        int trigger;

        switch (action) {
            case ADD_REGULAR:
                trigger = TutorialTrigger.ICON_INT_REGULAR_PLUS;
                break;
            case REMOVE_REGULAR:
                trigger = TutorialTrigger.ICON_INT_REGULAR_MINUS;
                break;
            case INCREASE_AUTOMATED:
                trigger = TutorialTrigger.ICON_INT_AUTOMATED_PLUS;
                break;
            case DECREASE_AUTOMATED:
                trigger = TutorialTrigger.ICON_INT_AUTOMATED_MINUS;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported production action: " + action);
        }

        Game.updateTutorialFlow(
                TutorialTrigger.TYPE_INT_ICONHIT,
                trigger,
                null,
                item.getParameter());
    }
}
