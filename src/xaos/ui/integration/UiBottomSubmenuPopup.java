
package xaos.ui.integration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import xaos.internal.api.mods.ModUiErrors;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiButtonAppearance;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiButtonAvailabilityTracker;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiBottomPopupPlacement;
import xaos.ui.layout.UiGridLayout;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiSprite;
import xaos.ui.rendering.UiTextMetrics;

import xaos.ui.integration.UiGameplayMenuResolver.IconReference;

/**
 * XML-driven submenu popup for the bottom toolbar.
 *
 * Uses the standard themed window background and
 * the Classic Towns circular menu buttons.
 */
public final class UiBottomSubmenuPopup {

    private static final int BUTTON_SIZE = 64;
    private static final int ICON_SIZE = 52;

    private final UiSystem ui;
    private final UiWindow window;
    private final UiScrollContainer scroll;
    private final UiGameplayMenuResolver resolver;
    private final UiTextMetrics metrics;

    private final Deque<String> history = new ArrayDeque<>();

    private final UiButtonAvailabilityTracker availability = new UiButtonAvailabilityTracker();

    private MenuManager menus;

    private UiGameplayActionBinding actionBinding = UiGameplayActionBinding.disabled();

    private Function<IconReference, UiSprite> iconResolver = reference -> null;

    private Runnable clickFeedback = () -> {
    };

    private int currentColumns;

    public UiBottomSubmenuPopup(
            UiSystem ui,
            MenuManager menus,
            UiGameplayMenuResolver resolver,
            UiTextMetrics metrics) {

        this.ui = Objects.requireNonNull(ui);
        this.menus = Objects.requireNonNull(menus);
        this.resolver = Objects.requireNonNull(resolver);
        this.metrics = Objects.requireNonNull(metrics);

        window = new UiWindow("Bottom Submenu");

        window.titleBar().setVisible(false);
        window.setDraggable(false);
        window.setResizable(false);
        window.setModal(false);
        window.setMinimumSize(UiSize.ZERO);
        window.setPadding(UiInsets.NONE);
        window.setSpacing(0);

        window.content().setPadding(UiInsets.NONE);
        window.content().setSpacing(0);

        scroll = new UiScrollContainer();

        scroll.setPadding(UiInsets.NONE);

        scroll.content().setPadding(
                UiInsets.all(
                        UiBottomPopupPlacement.SPACING));

        scroll.content().setSpacing(
                UiBottomPopupPlacement.SPACING);

        window.content().add(scroll);

        window.close();
        ui.windows().register(window);
    }

    public UiWindow window() {
        return window;
    }

    public UiScrollContainer scroll() {
        return scroll;
    }

    public boolean isOpen() {
        return window.isOpen();
    }

    public String currentMenuId() {
        return history.peekLast();
    }

    public void setMenus(MenuManager menus) {

        this.menus = Objects.requireNonNull(menus);

        // A mod may have removed an entire submenu.
        if (currentMenuId() != null
                && !menus.hasMenu(currentMenuId())) {

            hide();
            return;
        }

        refresh();
    }

    public void setActionBinding(
            UiGameplayActionBinding binding) {

        actionBinding = Objects.requireNonNull(
                binding);

        refresh();
    }

    public void setIconResolver(
            Function<IconReference, UiSprite> resolver) {

        iconResolver = Objects.requireNonNull(
                resolver);

        refresh();
    }

    public void setClickFeedback(Runnable feedback) {

        clickFeedback = Objects.requireNonNull(
                feedback);
    }

    public void showMenu(String menuId) {

        if (!menus.hasMenu(menuId)) {
            throw new IllegalArgumentException(
                    "Unknown bottom submenu: " + menuId);
        }

        history.clear();
        history.addLast(menuId);

        rebuild();

        ui.windows().open(window);
    }

    public void hide() {

        history.clear();
        availability.clear();

        if (window.isOpen()) {
            ui.cancelInput();
            ui.windows().close(window);
        }
    }

    public void refresh() {

        if (window.isOpen()
                && currentMenuId() != null) {

            rebuild();
        }
    }

    public void refreshActionAvailability() {
        availability.refresh();
    }

    /**
     * Positions the popup above its toolbar.
     *
     * Called after the toolbar has valid bounds.
     * Also safe to call after a viewport resize.
     */
    public void updatePlacement(
            UiRect toolbarBounds,
            UiSize viewport,
            int rightLimitX) {

        if (!window.isOpen()
                || currentMenuId() == null
                || viewport.width() <= 0
                || toolbarBounds.width() <= 0) {
            return;
        }

        int leftX = Math.max(
                0,
                toolbarBounds.x() + 32);

        int rightLimit = rightLimitX <= 0
                ? viewport.width() - 8
                : Math.min(
                        viewport.width(),
                        rightLimitX);

        if (rightLimit <= leftX) {
            rightLimit = viewport.width();
        }

        int count = scroll.content()
                .children().size();

        var placement = UiBottomPopupPlacement.calculate(
                Math.max(1, count),
                leftX,
                rightLimit,
                toolbarBounds.y());

        // If a scrollbar is required, reserve
        // its width rather than obscuring icons.
        UiRect bounds = placement.bounds();

        if (placement.requiresScrolling()) {

            int scrollbarWidth = xaos.ui.components.UiScrollBar.WIDTH;

            int reducedLimit = Math.max(
                    leftX + 1,
                    rightLimit - scrollbarWidth);

            var adjusted = UiBottomPopupPlacement.calculate(
                    Math.max(1, count),
                    leftX,
                    reducedLimit,
                    toolbarBounds.y());

            int width = adjusted.bounds().width()
                    + scrollbarWidth;

            bounds = new UiRect(
                    Math.max(0, Math.min(
                            adjusted.bounds().x(),
                            rightLimit - width)),
                    adjusted.bounds().y(),
                    width,
                    adjusted.bounds().height());

            placement = adjusted;
        }

        if (currentColumns != placement.columns()) {

            currentColumns = placement.columns();

            scroll.content().setLayoutStrategy(
                    new UiGridLayout(currentColumns));
        }

        window.setUserSize(
                new UiSize(
                        bounds.width(),
                        bounds.height()));

        window.setPosition(
                new UiPoint(
                        bounds.x(),
                        bounds.y()));
    }

    private void goBack() {

        clickFeedback.run();

        history.removeLast();

        if (history.isEmpty()) {
            hide();
        } else {
            rebuild();
        }
    }

    private void navigate(String targetMenu) {

        if (!menus.hasMenu(targetMenu)) {
            return;
        }

        clickFeedback.run();

        history.addLast(targetMenu);
        rebuild();
    }

    private void execute(
            MenuItemDefinition item,
            UiButton button) {

        try {

            if (!actionBinding.isAvailable(item)) {
                button.setEnabled(false);
                return;
            }

            clickFeedback.run();
            actionBinding.execute(item);

            // A chosen stockpile or zone enters
            // placement mode, so dismiss the popup.
            hide();

        } catch (RuntimeException exception) {

            ModUiErrors.report(
                    "open-towns",
                    "Bottom submenu action",
                    "Failed to execute " + item.getId(),
                    exception);
        }
    }

    private UiButton createButton(
            MenuItemDefinition item) {

        var resolved = resolver.resolve(item);

        UiButton button = new UiButton("", metrics);

        boolean submenu = "submenu".equals(item.getType());

        button.setAppearance(
                submenu
                        ? UiButtonAppearance.MENU_SUBMENU
                        : UiButtonAppearance.MENU_ACTION);

        button.setMinimumSize(
                new UiSize(BUTTON_SIZE, BUTTON_SIZE));

        button.setPreferredSize(
                new UiSize(BUTTON_SIZE, BUTTON_SIZE));

        button.setPadding(UiInsets.NONE);

        UiSprite icon = resolved.icon() == null
                ? null
                : iconResolver.apply(resolved.icon());

        if (icon != null) {

            button.setIcon(icon);

            int width = icon.source().width();
            int height = icon.source().height();

            int largest = Math.max(
                    1,
                    Math.max(width, height));

            button.setIconSize(new UiSize(
                    Math.max(1,
                            width * ICON_SIZE / largest),
                    Math.max(1,
                            height * ICON_SIZE / largest)));

        } else {
            button.setText(
                    "back".equals(item.getType())
                            ? "<"
                            : "?");
        }

        button.setTooltip(resolved.title());

        switch (item.getType()) {

            case "back" ->
                button.setOnClick(this::goBack);

            case "submenu" -> {

                if (menus.hasMenu(item.getTargetMenu())) {

                    button.setOnClick(
                            () -> navigate(
                                    item.getTargetMenu()));

                } else {
                    button.setEnabled(false);
                }
            }

            case "action" -> {

                button.setOnClick(
                        () -> execute(item, button));

                availability.track(
                        button,
                        () -> actionBinding.isAvailable(item),
                        exception -> ModUiErrors.report(
                                "open-towns",
                                "Bottom submenu availability",
                                "Could not check " + item.getId(),
                                exception));
            }

            default -> button.setEnabled(false);
        }

        return button;
    }

    private void rebuild() {

        availability.clear();
        scroll.content().clear();
        scroll.scrollTo(0);

        String menuId = currentMenuId();

        if (menuId == null) {
            return;
        }

        List<MenuItemDefinition> items = menus.requireMenu(menuId).getItems()
                .stream()
                .filter(item -> !"spacer".equals(item.getType()))
                .toList();

        boolean hasBack = false;

        for (MenuItemDefinition item : items) {

            scroll.content().add(
                    createButton(item));

            if ("back".equals(item.getType())) {
                hasBack = true;
            }
        }

        // Support modded submenus without an
        // explicit XML Back entry.
        if (!hasBack) {

            UiButton back = new UiButton("<", metrics);

            back.setAppearance(
                    UiButtonAppearance.MENU_ACTION);

            back.setMinimumSize(
                    new UiSize(64, 64));

            back.setPreferredSize(
                    new UiSize(64, 64));

            back.setTooltip("Back");
            back.setOnClick(this::goBack);

            scroll.content().add(back);
        }
    }
}
