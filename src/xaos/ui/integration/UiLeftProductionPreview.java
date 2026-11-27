
package xaos.ui.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import xaos.main.Game;
import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;
import xaos.platform.lwjgl3.input.Keyboard;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiButtonAppearance;
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiGridLayout;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiLinearLayout;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiSizePolicy;
import xaos.ui.layout.UiWindowAnchor;
import xaos.ui.layout.UiWindowDockMode;
import xaos.ui.rendering.UiSprite;
import xaos.ui.rendering.UiTextMetrics;

import xaos.ui.integration.UiGameplayMenuResolver.IconReference;

/**
 * Framework-based production panel preview.
 *
 * Uses migrated XML menus and live production
 * counts without modifying gameplay queues.
 *
 * Not yet connected to Game.java.
 */
public final class UiLeftProductionPreview {

    private static final int BUTTON_SIZE = 64;
    private static final int ICON_SIZE = 52;

    private static final int PRODUCTION_CELL_WIDTH = 128;
    private static final int PRODUCTION_CELL_HEIGHT = 80;

    private final UiSystem ui;
    private final UiProductionMenuModel model;
    private final UiTextMetrics metrics;

    private final UiWindow window;
    private final UiScrollContainer scroll;
    private final UiContainer itemGrid;

    private Function<IconReference, UiSprite> iconResolver = reference -> null;

    private Runnable clickFeedback = () -> {
    };
    private final UiProductionQueueAdapter queueAdapter = new UiProductionQueueAdapter();
    private final Map<String, UiLabel> countLabels = new HashMap<>();
    private final Map<String, UiLabel> worldCountLabels = new HashMap<>();
    private final Map<String, UiLabel> automatedCountLabels = new HashMap<>();

    UiLabel automatedCountLabel(String itemId) {
        return automatedCountLabels.get(itemId);
    }

    public UiLeftProductionPreview(
            UiSystem ui,
            UiProductionMenuModel model,
            UiTextMetrics metrics) {

        this.ui = Objects.requireNonNull(ui);
        this.model = Objects.requireNonNull(model);
        this.metrics = Objects.requireNonNull(metrics);

        window = new UiWindow("Production");

        window.titleBar().setVisible(false);
        window.setModal(false);
        window.setDraggable(false);
        window.setResizable(false);

        window.setPosition(new UiPoint(12, 96));
        window.setUserSize(new UiSize(340, 480));

        window.content().setPadding(UiInsets.all(6));
        window.content().setSpacing(8);

        scroll = new UiScrollContainer();

        scroll.content().setSpacing(8);

        // Same two-column grid layout as the right menu.
        itemGrid = new UiContainer();

        itemGrid.setLayoutStrategy(
                new UiGridLayout(2, true));

        itemGrid.setHorizontalSizePolicy(
                UiSizePolicy.FILL);

        itemGrid.setPadding(UiInsets.NONE);
        itemGrid.setSpacing(8);

        window.content().add(scroll);

        rebuild();

        // The legacy panel remains responsible
        // for production until the new one is wired.
        window.close();
        ui.windows().register(window);
    }

    private UiContainer createWorldCountIcon(
            UiProductionMenuModel.Entry entry,
            UiButton icon) {

        final int iconSize = BUTTON_SIZE;

        UiContainer overlay = new UiContainer();

        overlay.setLayoutStrategy(
                UiAbsoluteLayout.INSTANCE);

        overlay.setPadding(UiInsets.NONE);
        overlay.setMinimumSize(new UiSize(iconSize, iconSize));
        overlay.setPreferredSize(new UiSize(iconSize, iconSize));

        // Draw the circular production icon first.
        icon.setPosition(UiPoint.ZERO);
        overlay.add(icon);

        // Draw the world count above the icon.
        UiLabel worldCount = new UiLabel("", metrics);
        worldCount.setVisible(false);

        overlay.add(worldCount);

        worldCountLabels.put(
                entry.definition().getId(),
                worldCount);

        return overlay;
    }

    public UiWindow window() {
        return window;
    }

    public String currentMenuId() {
        return model.currentMenuId();
    }

    public void open() {
        ui.windows().open(window);
    }

    public void close() {
        ui.windows().close(window);
    }

    public void setClickFeedback(Runnable feedback) {
        clickFeedback = Objects.requireNonNull(feedback);
    }

    public void setIconResolver(
            Function<IconReference, UiSprite> resolver) {

        iconResolver = Objects.requireNonNull(resolver);
        rebuild();
    }

    /**
     * Reload the menu definitions, including mod
     * contributions, and return to the root menu.
     */
    public void setMenus(MenuManager menus) {
        model.setMenus(menus);
        rebuild();
    }

    /**
     * Rebuild after runtime menu extensions change.
     */
    public void refreshCurrentMenu() {
        rebuild();
    }

    /**
     * Refresh production counts without rebuilding
     * buttons, navigation or scroll position.
     */
    public void refreshCounts() {

        for (var entry : model.entries()) {

            String id = entry.definition().getId();
            UiProductionQueueCounts counts = entry.counts();

            if (counts == null) {
                continue;
            }

            // Regular and automated queue counts.

            // Regular count, centred between left controls.
            UiLabel regularLabel = countLabels.get(id);

            if (regularLabel != null) {

                String value = Integer.toString(counts.regular());

                regularLabel.setText(value);

                UiSize textSize = metrics.measure(value);

                regularLabel.setPosition(
                        new UiPoint(
                                16 - textSize.width() / 2,
                                32));
            }

            // Automated count, centred between right controls.
            UiLabel automatedLabel = automatedCountLabels.get(id);

            if (automatedLabel != null) {

                String value = Integer.toString(counts.automated());

                automatedLabel.setText(value);

                UiSize textSize = metrics.measure(value);

                automatedLabel.setPosition(
                        new UiPoint(
                                112 - textSize.width() / 2,
                                32));
            }

            // In-world count displayed over the item icon.
            UiLabel worldLabel = worldCountLabels.get(id);

            if (worldLabel != null) {

                int inWorld = counts.inWorld();

                worldLabel.setVisible(inWorld > 0);

                if (inWorld > 0) {

                    String value = Integer.toString(inWorld);

                    worldLabel.setText(value);

                    UiSize textSize = metrics.measure(value);

                    final int iconSize = BUTTON_SIZE;

                    // Centre the text horizontally.
                    int x = (iconSize - textSize.width()) / 2;

                    // Position over the upper quarter.
                    int y = (iconSize / 4)
                            - (textSize.height() / 2);

                    worldLabel.setPosition(
                            new UiPoint(x, y));
                }
            }
        }
    }

    /**
     * Position the panel against the left edge.
     *
     * The provided insets reserve space for
     * surrounding gameplay UI.
     */
    public void dockLeft(UiInsets insets) {

        Objects.requireNonNull(insets);

        window.setCentered(false);
        window.setAnchor(UiWindowAnchor.LEFT);
        window.setAnchorInsets(insets);

        window.setDockMode(
                UiWindowDockMode.FILL_HEIGHT);

        window.setDraggable(false);
        window.setResizable(false);
    }

    private void rebuild() {
        automatedCountLabels.clear();
        worldCountLabels.clear();
        countLabels.clear();

        itemGrid.clear();
        scroll.content().clear();
        scroll.scrollTo(0);

        // The window still retains a logical title,
        // but the gameplay UI does not display it.
        String title = model.currentMenu().getTitle();

        if (title == null || title.isBlank()) {
            title = model.currentMenuId();
        }

        window.setTitle(title);

        // Put the icon grid first in the scroll area.
        scroll.content().add(itemGrid);

        UiButton explicitBack = null;

        for (var entry : model.entries()) {

            MenuItemDefinition item = entry.definition();
            String type = item.getType();

            // Keep Back separate from the two-column grid.
            if ("back".equalsIgnoreCase(type)) {

                if (model.canGoBack()) {
                    explicitBack = createButton(entry);
                }

                continue;
            }

            if ("spacer".equalsIgnoreCase(type)) {
                continue;
            }

            UiButton button = createButton(entry);

            if (isProductionAction(item)) {

                itemGrid.add(
                        createProductionCell(entry, button));

            } else {

                itemGrid.add(
                        createSimpleMenuCell(button));
            }
        }

        // Back is always underneath the grid,
        // including when a mod omits its XML entry.
        if (model.canGoBack()) {

            UiButton back = explicitBack;

            if (back == null) {

                back = new UiButton("<", metrics);

                back.setAppearance(
                        UiButtonAppearance.MENU_ACTION);

                back.setMinimumSize(
                        new UiSize(BUTTON_SIZE, BUTTON_SIZE));

                back.setPreferredSize(
                        new UiSize(BUTTON_SIZE, BUTTON_SIZE));

                back.setPadding(UiInsets.NONE);
                back.setTooltip("Back");

                back.setOnClick(() -> {
                    clickFeedback.run();
                    model.back();
                    rebuild();
                });
            }

            UiContainer backRow = new UiContainer();

            backRow.setLayoutStrategy(
                    new UiGridLayout(1, true));

            backRow.setHorizontalSizePolicy(
                    UiSizePolicy.FILL);

            backRow.setPadding(UiInsets.NONE);
            backRow.add(back);

            scroll.content().add(backRow);
        }

        refreshCounts();
    }

    /**
     * Displays an ordinary menu button in the same
     * cell width as a production item.
     *
     * This keeps submenu icons aligned with
     * production icons.
     */
    private UiContainer createSimpleMenuCell(
            UiButton button) {

        UiContainer cell = new UiContainer();

        cell.setLayoutStrategy(
                UiAbsoluteLayout.INSTANCE);

        cell.setPadding(UiInsets.NONE);

        cell.setMinimumSize(
                new UiSize(
                        PRODUCTION_CELL_WIDTH,
                        PRODUCTION_CELL_HEIGHT));

        cell.setPreferredSize(
                new UiSize(
                        PRODUCTION_CELL_WIDTH,
                        PRODUCTION_CELL_HEIGHT));

        // Centre the 64px icon horizontally in
        // its 128px grid cell.
        button.setPosition(
                new UiPoint(32, 8));
        cell.add(button);

        return cell;
    }

    private UiButton createButton(
            UiProductionMenuModel.Entry entry) {

        MenuItemDefinition item = entry.definition();

        String type = item.getType();
        String title = entry.presentation().title();

        UiButton button = new UiButton("", metrics);

        button.setAppearance(
                "submenu".equals(type)
                        ? UiButtonAppearance.MENU_SUBMENU
                        : UiButtonAppearance.MENU_ACTION);

        button.setMinimumSize(
                new UiSize(BUTTON_SIZE, BUTTON_SIZE));

        button.setPreferredSize(
                new UiSize(BUTTON_SIZE, BUTTON_SIZE));

        button.setPadding(UiInsets.NONE);

        var iconReference = entry.presentation().icon();

        if (iconReference != null) {

            UiSprite sprite = iconResolver.apply(iconReference);

            if (sprite != null) {

                button.setIcon(sprite);

                int width = sprite.source().width();
                int height = sprite.source().height();

                int largest = Math.max(
                        1, Math.max(width, height));

                button.setIconSize(new UiSize(
                        Math.max(1, width * ICON_SIZE / largest),
                        Math.max(1, height * ICON_SIZE / largest)));
            }
        }

        if (button.icon() == null) {

            button.setText(
                    "back".equals(type) ? "<"
                            : "submenu".equals(type) ? ">"
                                    : "?");
        }

        button.setTooltip(title);

        if ("submenu".equals(type)) {

            button.setTooltip(
                    title + "\nOpen submenu");

            button.setOnClick(() -> {
                clickFeedback.run();
                model.navigate(item);
                rebuild();
            });

        } else if ("back".equals(type)) {

            button.setOnClick(() -> {
                clickFeedback.run();
                model.back();
                rebuild();
            });

        } else {

            // Read-only preview until production
            // interactions are integrated.
            button.setEnabled(false);
            button.setTooltip(
                    title + "\nProduction controls not connected");
        }

        return button;

    }

    // Package-private access for isolated tests.
    UiScrollContainer scroll() {
        return scroll;
    }

    UiLabel countLabel(String itemId) {
        return countLabels.get(itemId);
    }

    private static boolean isProductionAction(MenuItemDefinition item) {
        return item != null
                && "action".equalsIgnoreCase(item.getType())
                && CommandPanel.COMMAND_QUEUE.equalsIgnoreCase(item.getCommand())
                && item.getParameter() != null
                && !item.getParameter().isBlank();
    }

    /**
     * Creates one compact production item.
     *
     * Layout:
     *
     * Regular +/- | Item icon | Automated +/-
     *
     * The icon includes its in-world count overlay.
     */
    private UiContainer createProductionCell(
            UiProductionMenuModel.Entry entry,
            UiButton icon) {

        MenuItemDefinition item = entry.definition();

        UiContainer cell = new UiContainer();

        cell.setLayoutStrategy(
                UiAbsoluteLayout.INSTANCE);

        cell.setPadding(UiInsets.NONE);

        cell.setMinimumSize(
                new UiSize(
                        PRODUCTION_CELL_WIDTH,
                        PRODUCTION_CELL_HEIGHT));

        cell.setPreferredSize(
                new UiSize(
                        PRODUCTION_CELL_WIDTH,
                        PRODUCTION_CELL_HEIGHT));

        // --------------------------------------------------
        // Central production icon and in-world count
        // --------------------------------------------------

        UiContainer iconOverlay = createWorldCountIcon(entry, icon);

        iconOverlay.setPosition(
                new UiPoint(32, 8));

        cell.add(iconOverlay);

        // --------------------------------------------------
        // Regular queue controls — left side
        // --------------------------------------------------

        addProductionControl(
                cell,
                item,
                "Add regular production",
                UiProductionQueueAdapter.Action.ADD_REGULAR,
                0,
                0);

        addProductionControl(
                cell,
                item,
                "Remove regular production",
                UiProductionQueueAdapter.Action.REMOVE_REGULAR,
                0,
                48);

        // --------------------------------------------------
        // Automated queue controls — right side
        // --------------------------------------------------

        addProductionControl(
                cell,
                item,
                "Increase automated target",
                UiProductionQueueAdapter.Action.INCREASE_AUTOMATED,
                96,
                0);

        addProductionControl(
                cell,
                item,
                "Decrease automated target",
                UiProductionQueueAdapter.Action.DECREASE_AUTOMATED,
                96,
                48);

        // Compact queue counts beneath the item controls.

        if (entry.counts() != null) {

            UiLabel regularCount = new UiLabel("", metrics);

            UiLabel automatedCount = new UiLabel("", metrics);

            cell.add(regularCount);
            cell.add(automatedCount);

            countLabels.put(
                    item.getId(),
                    regularCount);

            automatedCountLabels.put(
                    item.getId(),
                    automatedCount);
        }

        return cell;
    }

    /**
     * Adds one production control at an exact
     * position within the item cell.
     */
    private void addProductionControl(
            UiContainer cell,
            MenuItemDefinition item,
            String tooltip,
            UiProductionQueueAdapter.Action action,
            int x,
            int y) {

        UiButton button = createQueueButton(
                "",
                tooltip,
                item,
                action);

        button.setPosition(
                new UiPoint(x, y));

        cell.add(button);
    }

    private UiButton createQueueButton(
            String text,
            String tooltip,
            MenuItemDefinition item,
            UiProductionQueueAdapter.Action action) {

        UiButton button = new UiButton(text, metrics);

        boolean adding = action == UiProductionQueueAdapter.Action.ADD_REGULAR
                || action == UiProductionQueueAdapter.Action.INCREASE_AUTOMATED;

        button.setAppearance(
                adding
                        ? UiButtonAppearance.PRODUCTION_ADD
                        : UiButtonAppearance.PRODUCTION_REMOVE);

        button.setMinimumSize(new UiSize(32, 32));
        button.setPreferredSize(new UiSize(32, 32));
        button.setPadding(UiInsets.NONE);
        button.setTooltip(tooltip);

        button.setOnClick(() -> {

            if (!isProductionAction(item)
                    || Game.getWorld() == null) {
                return;
            }

            boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                    || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

            boolean control = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                    || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);

            queueAdapter.apply(
                    action,
                    shift,
                    control,
                    new LegacyProductionQueueBackend(item));

            clickFeedback.run();

            // Update displayed queue counts without
            // rebuilding the entire production menu.
            refreshCounts();
        });

        return button;
    }

    UiLabel worldCountLabelForTesting(String actionId) {
        return worldCountLabels.get(actionId);
    }
}
