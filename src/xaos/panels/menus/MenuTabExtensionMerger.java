
package xaos.panels.menus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import xaos.panels.menus.MenuExtensionDefinition.AddedTab;
import xaos.panels.menus.MenuExtensionDefinition.TabItem;

/**
 * Applies tab additions to a private working
 * copy of a menu.
 */
final class MenuTabExtensionMerger {

    private MenuTabExtensionMerger() {
    }

    static void apply(
            String menuId,
            List<MenuContentDefinition> content,
            Set<String> usedIds,
            MenuExtensionDefinition extension) {

        // --------------------------------------------------------
        // Add new tabs first
        // --------------------------------------------------------

        for (AddedTab addition : extension.addedTabs()) {

            int groupIndex = findGroup(
                    content,
                    addition.targetTabs());

            MenuTabsDefinition group = (MenuTabsDefinition) content.get(groupIndex);

            List<MenuTabDefinition> tabs = new ArrayList<>(group.getTabs());

            for (MenuTabDefinition existing : tabs) {

                if (existing.getId().equals(
                        addition.tab().getId())) {

                    throw new IllegalArgumentException(
                            "Duplicate tab ID: "
                                    + addition.tab().getId());
                }
            }

            // Ensure the new tab's items don't
            // conflict with existing menu items.
            for (MenuItemDefinition item : addition.tab().getItems()) {

                reserveItemId(
                        usedIds, item, menuId);
            }

            int index = addition.before() != null
                    ? findTab(tabs, addition.before())
                    : addition.after() != null
                            ? findTab(tabs, addition.after()) + 1
                            : tabs.size();

            tabs.add(index, addition.tab());

            content.set(
                    groupIndex,
                    new MenuTabsDefinition(
                            group.getId(),
                            group.getPlacement(),
                            tabs));
        }

        // --------------------------------------------------------
        // Extend existing or newly added tabs
        // --------------------------------------------------------

        for (TabItem addition : extension.tabItems()) {

            int groupIndex = findGroup(
                    content,
                    addition.targetTabs());

            MenuTabsDefinition group = (MenuTabsDefinition) content.get(groupIndex);

            List<MenuTabDefinition> tabs = new ArrayList<>(group.getTabs());

            int tabIndex = findTab(
                    tabs,
                    addition.targetTab());

            MenuTabDefinition originalTab = tabs.get(tabIndex);

            List<MenuItemDefinition> items = new ArrayList<>(originalTab.getItems());

            reserveItemId(
                    usedIds,
                    addition.item(),
                    menuId);

            int index = addition.before() != null
                    ? findItem(items, addition.before())
                    : addition.after() != null
                            ? findItem(items, addition.after()) + 1
                            : beforeTrailingBack(items);

            items.add(index, addition.item());

            tabs.set(
                    tabIndex,
                    new MenuTabDefinition(
                            originalTab.getId(),
                            originalTab.getTitleKey(),
                            originalTab.getTitle(),
                            items));

            content.set(
                    groupIndex,
                    new MenuTabsDefinition(
                            group.getId(),
                            group.getPlacement(),
                            tabs));
        }
    }

    private static void reserveItemId(
            Set<String> usedIds,
            MenuItemDefinition item,
            String menuId) {

        if (item.getId() == null
                || item.getId().isBlank()) {

            throw new IllegalArgumentException(
                    "Blank item ID in menu: " + menuId);
        }

        if (!usedIds.add(item.getId())) {

            throw new IllegalArgumentException(
                    "Duplicate menu item ID: "
                            + item.getId()
                            + " in " + menuId);
        }
    }

    private static int findGroup(
            List<MenuContentDefinition> content,
            String id) {

        int found = -1;

        for (int i = 0; i < content.size(); i++) {

            if (content.get(i) instanceof MenuTabsDefinition group
                    && group.getId().equals(id)) {

                if (found >= 0) {
                    throw new IllegalArgumentException(
                            "Ambiguous tabs group ID: " + id);
                }

                found = i;
            }
        }

        if (found < 0) {
            throw new IllegalArgumentException(
                    "Tabs group not found: " + id);
        }

        return found;
    }

    private static int findTab(
            List<MenuTabDefinition> tabs,
            String id) {

        for (int i = 0; i < tabs.size(); i++) {

            if (tabs.get(i).getId().equals(id)) {
                return i;
            }
        }

        throw new IllegalArgumentException(
                "Tab not found: " + id);
    }

    private static int findItem(
            List<MenuItemDefinition> items,
            String id) {

        for (int i = 0; i < items.size(); i++) {

            if (items.get(i).getId().equals(id)) {
                return i;
            }
        }

        throw new IllegalArgumentException(
                "Tab item not found: " + id);
    }

    private static int beforeTrailingBack(
            List<MenuItemDefinition> items) {

        int index = items.size();

        while (index > 0
                && "back".equals(
                        items.get(index - 1).getType())) {

            index--;
        }

        return index;
    }
}
