
package xaos.ui.integration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;

/**
 * Read-only model for the XML production panel.
 *
 * Handles:
 * - XML menu navigation
 * - Resolved names and icons
 * - Live production queue counts
 *
 * Does not render UI or modify gameplay queues.
 */
public final class UiProductionMenuModel {

    public static final String ROOT_MENU = "game.left.production.root";

    /**
     * One entry ready for the production UI.
     *
     * Counts are null for non-production entries.
     */
    public record Entry(
            MenuItemDefinition definition,
            UiGameplayMenuResolver.ResolvedItem presentation,
            UiProductionQueueCounts counts) {
    }

    private MenuManager menus;

    private final UiGameplayMenuResolver resolver;

    private final Function<String, UiProductionQueueCounts> queueReader;

    private final Deque<String> history = new ArrayDeque<>();

    private String currentMenuId = ROOT_MENU;

    public UiProductionMenuModel(
            MenuManager menus,
            UiGameplayMenuResolver resolver,
            Function<String, UiProductionQueueCounts> queueReader) {

        this.resolver = Objects.requireNonNull(resolver);

        this.queueReader = Objects.requireNonNull(queueReader);

        setMenus(menus);
    }

    /**
     * Replace the registry after a world or mod reload.
     */
    public void setMenus(MenuManager newMenus) {

        Objects.requireNonNull(newMenus);

        // Ensure the expected production root exists.
        newMenus.requireMenu(ROOT_MENU);

        if (menus == newMenus) {
            return;
        }

        menus = newMenus;
        currentMenuId = ROOT_MENU;
        history.clear();
    }

    public String currentMenuId() {
        return currentMenuId;
    }

    public MenuDefinition currentMenu() {
        return menus.requireMenu(currentMenuId);
    }

    public boolean canGoBack() {
        return !history.isEmpty();
    }

    /**
     * Build a snapshot of the visible menu entries.
     *
     * Queue values are read on demand rather than
     * cached, so changes to production are reflected
     * when the UI refreshes.
     */
    public List<Entry> entries() {

        List<Entry> result = new ArrayList<>();

        for (MenuItemDefinition item : currentMenu().getItems()) {

            if ("back".equals(item.getType())
                    && !canGoBack()) {
                continue;
            }

            var presentation = resolver.resolve(item);

            UiProductionQueueCounts counts = null;

            if (isProductionAction(item)) {

                counts = queueReader.apply(
                        item.getParameter());
            }

            result.add(new Entry(
                    item,
                    presentation,
                    counts));
        }

        return List.copyOf(result);
    }

    /**
     * Navigate through an actual submenu entry
     * belonging to the currently displayed menu.
     */
    public void navigate(MenuItemDefinition item) {

        Objects.requireNonNull(item);

        if (!currentMenu().getItems().contains(item)) {
            throw new IllegalArgumentException(
                    "Item does not belong to current menu: "
                            + item.getId());
        }

        if (!"submenu".equals(item.getType())) {
            throw new IllegalArgumentException(
                    "Not a submenu: " + item.getId());
        }

        String target = item.getTargetMenu();

        // Fail clearly for broken menu references.
        menus.requireMenu(target);

        history.addLast(currentMenuId);
        currentMenuId = target;
    }

    public void back() {

        if (!history.isEmpty()) {
            currentMenuId = history.removeLast();
        }
    }

    private static boolean isProductionAction(
            MenuItemDefinition item) {

        return "action".equals(item.getType())
                && "QUEUE".equalsIgnoreCase(
                        item.getCommand())
                && item.getParameter() != null
                && !item.getParameter().isBlank();
    }
}
