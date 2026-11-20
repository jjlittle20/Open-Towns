
package xaos.ui.components;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import xaos.ui.core.UiContainer;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiLinearLayout;
import xaos.ui.layout.UiSizePolicy;
import xaos.ui.rendering.UiTextMetrics;

/**
 * Reusable tab bar with a single visible content page.
 *
 * Tab definitions can come from XML or Java.
 * The component only manages display and selection.
 */
public final class UiTabbedPane extends UiContainer {

    public enum Placement {
        TOP, BOTTOM, LEFT, RIGHT
    }

    private record Tab(
            UiButton button,
            UiContainer page) {
    }

    private final Map<String, Tab> tabs = new LinkedHashMap<>();

    private final UiContainer tabBar = new UiContainer();
    private final UiContainer pages = new UiContainer();
    private final UiTextMetrics metrics;

    private Placement placement;
    private String selectedTabId;

    public UiTabbedPane(UiTextMetrics metrics) {
        this(metrics, Placement.TOP);
    }

    public UiTabbedPane(
            UiTextMetrics metrics,
            Placement placement) {

        this.metrics = Objects.requireNonNull(
                metrics, "metrics");

        setHorizontalSizePolicy(UiSizePolicy.FILL);
        setVerticalSizePolicy(UiSizePolicy.FILL);
        setSpacing(4);

        tabBar.setSpacing(4);
        tabBar.setPadding(UiInsets.all(2));

        pages.setHorizontalSizePolicy(UiSizePolicy.FILL);
        pages.setVerticalSizePolicy(UiSizePolicy.FILL);
        pages.setClipChildren(true);

        setPlacement(placement);
    }

    /**
     * Registers a tab and returns its content container.
     */
    public UiContainer addTab(String id, String label) {

        if (id == null || id.isBlank()
                || tabs.containsKey(id)) {

            throw new IllegalArgumentException(
                    "Invalid or duplicate tab ID: " + id);
        }

        UiButton button = new UiButton(
                Objects.requireNonNull(label, "label"),
                metrics);

        button.setOnClick(() -> selectTab(id));

        UiContainer page = new UiContainer();

        page.setHorizontalSizePolicy(UiSizePolicy.FILL);
        page.setVerticalSizePolicy(UiSizePolicy.FILL);
        page.setVisible(false);

        tabBar.add(button);
        pages.add(page);

        tabs.put(id, new Tab(button, page));

        if (selectedTabId == null) {
            selectTab(id);
        }

        return page;
    }

    /**
     * Switches the visible page without rebuilding it.
     */
    public boolean selectTab(String id) {

        if (!tabs.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Unknown tab ID: " + id);
        }

        if (Objects.equals(selectedTabId, id)) {
            return false;
        }

        selectedTabId = id;

        for (var entry : tabs.entrySet()) {

            boolean active = entry.getKey().equals(id);

            entry.getValue().button().setSelected(active);
            entry.getValue().page().setVisible(active);
        }

        invalidateLayout();
        return true;
    }

    public String selectedTabId() {
        return selectedTabId;
    }

    public UiContainer page(String id) {

        Tab tab = tabs.get(id);

        if (tab == null) {
            throw new IllegalArgumentException(
                    "Unknown tab ID: " + id);
        }

        return tab.page();
    }

    public UiButton tabButton(String id) {

        Tab tab = tabs.get(id);

        if (tab == null) {
            throw new IllegalArgumentException(
                    "Unknown tab ID: " + id);
        }

        return tab.button();
    }

    public Placement placement() {
        return placement;
    }

    /**
     * Repositions the strip while retaining all tabs.
     */
    public void setPlacement(Placement next) {

        Objects.requireNonNull(next, "placement");

        if (next == placement) {
            return;
        }

        placement = next;

        boolean horizontal = next == Placement.TOP
                || next == Placement.BOTTOM;

        setLayoutStrategy(
                horizontal
                        ? UiLinearLayout.VERTICAL
                        : UiLinearLayout.HORIZONTAL);

        tabBar.setLayoutStrategy(
                horizontal
                        ? UiLinearLayout.HORIZONTAL
                        : UiLinearLayout.VERTICAL);

        tabBar.setHorizontalSizePolicy(
                horizontal
                        ? UiSizePolicy.FILL
                        : UiSizePolicy.PREFERRED);

        tabBar.setVerticalSizePolicy(
                horizontal
                        ? UiSizePolicy.PREFERRED
                        : UiSizePolicy.FILL);

        clear();

        if (next == Placement.TOP
                || next == Placement.LEFT) {

            add(tabBar);
            add(pages);

        } else {

            add(pages);
            add(tabBar);
        }
    }
}
