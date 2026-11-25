
package xaos.ui.integration;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;
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

    /**
     * Matches an individual action or any action
     * contained within a submenu.
     *
     * This allows Stockpiles and Zones to remain
     * highlighted while their selected placement
     * task is active.
     */
    public static boolean matchesOrContains(
            MenuItemDefinition item,
            Task task,
            MenuManager menus) {

        if (item == null || task == null) {
            return false;
        }

        if ("action".equals(item.getType())) {
            return matches(item, task);
        }

        if (!"submenu".equals(item.getType())
                || menus == null) {
            return false;
        }

        return menuContains(
                item.getTargetMenu(),
                task,
                menus,
                new HashSet<>());
    }

    private static boolean menuContains(
            String menuId,
            Task task,
            MenuManager menus,
            Set<String> visited) {

        if (menuId == null
                || !visited.add(menuId)
                || !menus.hasMenu(menuId)) {

            return false;
        }

        for (MenuItemDefinition child : menus.requireMenu(menuId).getItems()) {

            if ("action".equals(child.getType())
                    && matches(child, task)) {
                return true;
            }

            if ("submenu".equals(child.getType())
                    && menuContains(
                            child.getTargetMenu(),
                            task,
                            menus,
                            visited)) {
                return true;
            }
        }

        return false;
    }

}
