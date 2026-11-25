
package xaos.ui.integration;

import java.util.Objects;

import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;
import xaos.tasks.Task;

/**
 * Matches a bottom-menu action to the task
 * currently being created by the game.
 *
 * Does not store selection state.
 */
public final class UiBottomTaskSelection {

    private UiBottomTaskSelection() {
    }

    public static boolean matches(
            MenuItemDefinition item,
            Task task) {

        if (item == null
                || task == null
                || !"action".equals(item.getType())) {
            return false;
        }

        String command = UiLegacyBottomActionBinding
                .canonicalCommand(
                        item.getCommand());

        if (command == null) {
            return false;
        }

        return switch (command) {

            case CommandPanel.COMMAND_DIG ->
                task.getTask() == Task.TASK_DIG;

            case CommandPanel.COMMAND_MINE ->
                task.getTask() == Task.TASK_MINE;

            case CommandPanel.COMMAND_CANCEL_ORDER ->
                task.getTask() == Task.TASK_CANCEL_ORDER;

            case CommandPanel.COMMAND_CUSTOM_ACTION ->
                task.getTask() == Task.TASK_CUSTOM_ACTION
                        && sameParameters(item, task);

            case CommandPanel.COMMAND_STOCKPILE ->
                task.getTask() == Task.TASK_STOCKPILE
                        && Objects.equals(
                                item.getParameter(),
                                task.getParameter());

            case CommandPanel.COMMAND_CREATE_ZONE ->
                task.getTask() == Task.TASK_CREATE_ZONE
                        && Objects.equals(
                                item.getParameter(),
                                task.getParameter());

            default -> false;
        };
    }

    private static boolean sameParameters(
            MenuItemDefinition item,
            Task task) {

        return Objects.equals(
                item.getParameter(),
                task.getParameter())
                && Objects.equals(
                        item.getParameter2(),
                        task.getParameter2());
    }
}
