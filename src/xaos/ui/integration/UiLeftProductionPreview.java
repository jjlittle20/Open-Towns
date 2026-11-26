
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
    private static final int PRODUCTION_CELL_HEIGHT = 64;

    private final UiSystem ui;
    private final UiProductionMenuModel model;
    private final UiTextMetrics metrics;

    private final UiWindow window;
    private final UiLabel heading;
    private final UiScrollContainer scroll;

    private Function<IconReference, UiSprite> iconResolver = reference -> null;

    private Runnable clickFeedback = () -> {
    };
    private final UiProductionQueueAdapter queueAdapter = new UiProductionQueueAdapter();
    private final Map<String, UiLabel> countLabels = new HashMap<>();
    private final Map<String, UiLabel> worldCountLabels = new HashMap<>();

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

        heading = new UiLabel("", metrics);
        window.content().add(heading);

        scroll = new UiScrollContainer();
        scroll.content().setSpacing(6);

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
            UiLabel label = countLabels.get(id);

            if (label != null) {
                label.setText(formatCounts(counts));
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
        worldCountLabels.clear();
        countLabels.clear();
        scroll.content().clear();
        scroll.scrollTo(0);

        String title = model.currentMenu().getTitle();

        if (title == null || title.isBlank()) {
            title = model.currentMenuId();
        }

        window.setTitle(title);
        heading.setText(title);

        boolean hasBackItem = false;

        for (var entry : model.entries()) {

            MenuItemDefinition item = entry.definition();

            String type = item.getType();

            if ("back".equals(type)) {
                hasBackItem = true;
            }

            UiContainer row = new UiContainer();

            row.setLayoutStrategy(
                    UiLinearLayout.HORIZONTAL);

            row.setHorizontalSizePolicy(
                    UiSizePolicy.FILL);

            row.setSpacing(8);

            UiButton button = createButton(entry);

            if (isProductionAction(item)) {

                row.add(createProductionCell(entry, button));

            } else {

                row.add(button);
            }

            UiContainer description = new UiContainer();

            description.setLayoutStrategy(
                    UiLinearLayout.VERTICAL);

            description.setSpacing(4);

            description.setHorizontalSizePolicy(
                    UiSizePolicy.FILL);

            String label = entry.presentation().title();

            description.add(
                    new UiLabel(label, metrics));

            if (entry.counts() != null) {

                UiLabel counts = new UiLabel(
                        formatCounts(entry.counts()),
                        metrics);

                description.add(counts);

                countLabels.put(
                        item.getId(),
                        counts);
            }

            row.add(description);

            scroll.content().add(row);

        }

        // Mods may omit an explicit Back item.
        // Keep nested menus navigable.
        if (model.canGoBack() && !hasBackItem) {

            UiButton back = new UiButton("<", metrics);

            back.setAppearance(
                    UiButtonAppearance.MENU_ACTION);

            back.setMinimumSize(
                    new UiSize(BUTTON_SIZE, BUTTON_SIZE));

            back.setPreferredSize(
                    new UiSize(BUTTON_SIZE, BUTTON_SIZE));

            back.setTooltip("Back");

            back.setOnClick(() -> {
                clickFeedback.run();
                model.back();
                rebuild();
            });

            scroll.content().add(back);
        } // Initialise in-world overlays after rebuilding.
        refreshCounts();
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

    private static String formatCounts(
            UiProductionQueueCounts counts) {

        return "Regular: " + counts.regular()
                + "  Automated: " + counts.automated();
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
                new UiPoint(32, 0));

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
                32);

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
                32);

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
