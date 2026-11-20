package xaos.panels.menus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MenuDefinition {

    private final String id;
    private final String titleKey;

    // Existing SmartMenu consumers still use the flattened item list.
    private final List<MenuItemDefinition> items = new ArrayList<>();

    // New UI builders use the ordered, structured representation.
    private final List<MenuContentDefinition> content = new ArrayList<>();

    private final boolean transparent = true;

    public MenuDefinition(String id, String titleKey) {
        this.id = id;
        this.titleKey = titleKey;
    }

    public String getId() {
        return id;
    }

    public String getTitleKey() {
        return titleKey;
    }

    /** Legacy flattened view (includes items inside tabs). */
    public List<MenuItemDefinition> getItems() {
        return items;
    }

    /** Structured XML view, preserving top-level declaration order. */
    public List<MenuContentDefinition> getContent() {
        return List.copyOf(content);
    }

    public void addItem(MenuItemDefinition item) {
        items.add(Objects.requireNonNull(item, "item"));
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
