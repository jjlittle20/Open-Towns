package xaos.panels.menus;

import java.util.List;
import java.util.Objects;

/** One named tab and the menu items displayed inside it. */
public final class MenuTabDefinition {

    private final String id;
    private final String titleKey;
    private final String title;
    private final List<MenuItemDefinition> items;

    public MenuTabDefinition(String id, String titleKey, String title,
            List<MenuItemDefinition> items) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Tab id cannot be blank");
        }
        if ((titleKey == null || titleKey.isBlank())
                && (title == null || title.isBlank())) {
            throw new IllegalArgumentException(
                    "Tab must provide titleKey or title: " + id);
        }
        this.id = id;
        this.titleKey = titleKey;
        this.title = title;
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
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

    public List<MenuItemDefinition> getItems() {
        return items;
    }
}
