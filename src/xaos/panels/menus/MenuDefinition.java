
package xaos.panels.menus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One menu definition with a stable ID
 * and ordered contents.
 */
public final class MenuDefinition {

    private final String id;
    private final String titleKey;
    private final String title;

    // Existing flattened view.
    private final List<MenuItemDefinition> items = new ArrayList<>();

    // Structured view for the new UI builder.
    private final List<MenuContentDefinition> content = new ArrayList<>();

    private final boolean transparent = true;

    /**
     * Existing constructor remains available.
     */
    public MenuDefinition(String id, String titleKey) {
        this(id, titleKey, null);
    }

    /**
     * Supports either a localisation key
     * or a literal title.
     */
    public MenuDefinition(
            String id,
            String titleKey,
            String title) {

        this.id = Objects.requireNonNull(id, "id");
        this.titleKey = titleKey;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Legacy flattened view, including items
     * nested inside XML tabs.
     */
    public List<MenuItemDefinition> getItems() {
        return items;
    }

    /**
     * Preserves the original XML structure
     * and declaration order.
     */
    public List<MenuContentDefinition> getContent() {
        return List.copyOf(content);
    }

    public void addItem(MenuItemDefinition item) {

        items.add(
                Objects.requireNonNull(item, "item"));

        content.add(item);
    }

    public void addTabs(MenuTabsDefinition group) {

        Objects.requireNonNull(group, "group");

        content.add(group);

        for (MenuTabDefinition tab : group.getTabs()) {
            items.addAll(tab.getItems());
        }
    }

    public boolean isTransparent() {
        return transparent;
    }
}
