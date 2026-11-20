package xaos.panels.menus;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** An ordered set of tabs with a configurable tab-strip placement. */
public final class MenuTabsDefinition implements MenuContentDefinition {

    public enum Placement {
        TOP, BOTTOM, LEFT, RIGHT;

        public static Placement fromXml(String value) {
            if (value == null || value.isBlank()) {
                return TOP;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Invalid tab placement: " + value, exception);
            }
        }
    }

    private final String id;
    private final Placement placement;
    private final List<MenuTabDefinition> tabs;

    public MenuTabsDefinition(String id, Placement placement,
            List<MenuTabDefinition> tabs) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Tabs id cannot be blank");
        }
        this.id = id;
        this.placement = Objects.requireNonNull(placement, "placement");
        this.tabs = List.copyOf(Objects.requireNonNull(tabs, "tabs"));
        if (this.tabs.isEmpty()) {
            throw new IllegalArgumentException("Tabs group cannot be empty: " + id);
        }
        Set<String> ids = new HashSet<>();
        for (MenuTabDefinition tab : this.tabs) {
            if (!ids.add(tab.getId())) {
                throw new IllegalArgumentException(
                        "Duplicate tab id in " + id + ": " + tab.getId());
            }
        }
    }

    public String getId() {
        return id;
    }

    public Placement getPlacement() {
        return placement;
    }

    public List<MenuTabDefinition> getTabs() {
        return tabs;
    }
}
