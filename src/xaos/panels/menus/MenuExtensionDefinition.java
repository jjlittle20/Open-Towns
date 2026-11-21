
package xaos.panels.menus;

import java.util.List;
import java.util.Objects;

/**
 * Immutable declarative changes to one existing menu.
 */
public record MenuExtensionDefinition(
        String id,
        String targetMenu,
        List<Contribution> contributions,
        List<AddedTab> addedTabs,
        List<TabItem> tabItems) {

    public MenuExtensionDefinition {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Extension ID cannot be blank");
        }

        if (targetMenu == null || targetMenu.isBlank()) {
            throw new IllegalArgumentException(
                    "Extension targetMenu cannot be blank");
        }

        contributions = List.copyOf(
                Objects.requireNonNull(
                        contributions, "contributions"));

        addedTabs = List.copyOf(
                Objects.requireNonNull(
                        addedTabs, "addedTabs"));

        tabItems = List.copyOf(
                Objects.requireNonNull(
                        tabItems, "tabItems"));
    }

    /**
     * Preserve the previous constructor.
     */
    public MenuExtensionDefinition(
            String id,
            String targetMenu,
            List<Contribution> contributions) {

        this(id, targetMenu, contributions,
                List.of(), List.of());
    }

    /**
     * Existing top-level item contribution.
     */
    public record Contribution(
            MenuItemDefinition item,
            String before,
            String after) {

        public Contribution {
            Objects.requireNonNull(item, "item");
            rejectBoth(before, after, item.getId());
        }
    }

    /**
     * Adds a complete tab to a named tabs group.
     */
    public record AddedTab(
            String targetTabs,
            MenuTabDefinition tab,
            String before,
            String after) {

        public AddedTab {

            requireText(targetTabs, "targetTabs");
            Objects.requireNonNull(tab, "tab");

            rejectBoth(before, after, tab.getId());
        }
    }

    /**
     * Inserts one item into an existing tab.
     */
    public record TabItem(
            String targetTabs,
            String targetTab,
            MenuItemDefinition item,
            String before,
            String after) {

        public TabItem {

            requireText(targetTabs, "targetTabs");
            requireText(targetTab, "targetTab");

            Objects.requireNonNull(item, "item");

            rejectBoth(before, after, item.getId());
        }
    }

    private static void requireText(
            String value,
            String field) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " cannot be blank");
        }
    }

    private static void rejectBoth(
            String before,
            String after,
            String id) {

        if (before != null && after != null) {
            throw new IllegalArgumentException(
                    "Cannot specify both before and after: "
                            + id);
        }
    }
}
