
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.Function;

import xaos.internal.api.mods.ModUiErrors;
import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;

import xaos.ui.components.UiButton;
import xaos.ui.components.UiButtonAppearance;
import xaos.ui.components.UiWindow;
import xaos.ui.components.UiWindowAppearance;
import xaos.ui.core.UiButtonAvailabilityTracker;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiLinearLayout;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiToolbarPager;
import xaos.ui.layout.UiToolbarRowLayout;
import xaos.ui.layout.UiWindowAnchor;
import xaos.ui.rendering.UiSprite;
import xaos.ui.rendering.UiTextMetrics;

import xaos.ui.integration.UiGameplayMenuResolver.IconReference;
import xaos.ui.integration.UiGameplayMenuResolver.IconType;

/**
 * XML-driven bottom gameplay toolbar.
 *
 * Initial milestone:
 * - Root menu only
 * - Classic horizontal icon layout
 * - Ten visible entries
 * - One-item scrolling
 *
 * Gameplay execution and submenus are connected
 * in later milestones.
 */
public final class UiBottomMenuPreview {

    public static final String ROOT_MENU = "game.bottom.root";

    private static final int VISIBLE_SLOTS = 10;
    private static final int BUTTON_SIZE = 64;
    private static final int ICON_SIZE = 52;
    private static final int SCROLL_WIDTH = 32;

    private static final int CONTENT_WIDTH = 960;
    private static final int TOOLBAR_WIDTH = CONTENT_WIDTH;

    private final UiSystem ui;
    private final UiWindow window;
    private final UiContainer buttonRow;

    private final UiButton leftArrow;
    private final UiButton rightArrow;

    private final UiToolbarPager pager = new UiToolbarPager(VISIBLE_SLOTS);

    private final UiGameplayMenuResolver resolver;
    private final UiTextMetrics metrics;

    private MenuManager menus;

    private Function<IconReference, UiSprite> iconResolver = reference -> null;

    private UiGameplayActionBinding actionBinding = UiGameplayActionBinding.disabled();

    private Runnable clickFeedback = () -> {
    };
    private final UiBottomSubmenuPopup popup;

    private int popupRightLimitX;

    private final UiButtonAvailabilityTracker availabilityTracker = new UiButtonAvailabilityTracker();

    public UiBottomMenuPreview(
            UiSystem ui,
            MenuManager menus,
            UiGameplayMenuResolver resolver,
            UiTextMetrics metrics) {

        this.ui = Objects.requireNonNull(ui);
        this.menus = Objects.requireNonNull(menus);
        this.resolver = Objects.requireNonNull(resolver);
        this.metrics = Objects.requireNonNull(metrics);

        if (!menus.hasMenu(ROOT_MENU)) {
            throw new IllegalArgumentException(
                    "Missing bottom root menu: "
                            + ROOT_MENU);
        }

        window = new UiWindow("Bottom Toolbar");
        window.setAppearance(
                UiWindowAppearance.BOTTOM_TOOLBAR);
        // Original toolbar has no title strip.
        window.titleBar().setVisible(false);

        window.setDraggable(false);
        window.setResizable(false);
        window.setModal(false);

        window.setMinimumSize(UiSize.ZERO);

        window.setUserSize(
                new UiSize(TOOLBAR_WIDTH, BUTTON_SIZE));

        window.setAnchor(UiWindowAnchor.BOTTOM);

        // Original open/close tab is 32px high.
        window.setAnchorInsets(
                new UiInsets(0, 0, 32, 0));

        window.setPadding(UiInsets.NONE);
        window.setSpacing(0);

        window.content().setPadding(UiInsets.NONE);
        window.content().setSpacing(0);

        window.content().setLayoutStrategy(
                UiAbsoluteLayout.INSTANCE);
        // --------------------------------------------------
        // Left scroll arrow
        // --------------------------------------------------

        leftArrow = createArrow("<");

        leftArrow.setOnClick(() -> {
            if (pager.scrollLeft()) {
                clickFeedback.run();
                rebuild();
            }
        });
        // --------------------------------------------------
        // Main icon row
        // --------------------------------------------------

        buttonRow = new UiContainer();

        buttonRow.setPadding(UiInsets.NONE);

        buttonRow.setPreferredSize(
                new UiSize(CONTENT_WIDTH, BUTTON_SIZE));

        buttonRow.setLayoutStrategy(
                new UiToolbarRowLayout(
                        VISIBLE_SLOTS,
                        new UiSize(
                                BUTTON_SIZE,
                                BUTTON_SIZE)));

        // --------------------------------------------------
        // Right scroll arrow
        // --------------------------------------------------

        rightArrow = createArrow(">");

        rightArrow.setOnClick(() -> {
            if (pager.scrollRight()) {
                clickFeedback.run();
                rebuild();
            }
        });

        /*
         * Classic Towns has a 960px toolbar.
         *
         * The 32px scroll arrows occupy its
         * left and right edges, while the ten
         * menu buttons use the full 960px row.
         */
        leftArrow.setPosition(
                new UiPoint(0, 0));

        buttonRow.setPosition(
                new UiPoint(0, 0));

        rightArrow.setPosition(
                new UiPoint(
                        TOOLBAR_WIDTH - SCROLL_WIDTH,
                        0));

        window.content().add(leftArrow);
        window.content().add(buttonRow);
        window.content().add(rightArrow);
        popup = new UiBottomSubmenuPopup(
                ui,
                menus,
                resolver,
                metrics);
        rebuild();

        // Start hidden until the game explicitly
        // enables the new bottom toolbar.
        window.close();

        ui.windows().register(window);
    }

    public UiWindow window() {
        return window;
    }

    public MenuManager menus() {
        return menus;
    }

    public UiContainer buttonRow() {
        return buttonRow;
    }

    public UiBottomSubmenuPopup popup() {
        return popup;
    }

    /**
     * Optional right boundary used to avoid
     * overlapping the gameplay right panel.
     */
    public void setPopupRightLimit(int rightLimitX) {
        popupRightLimitX = Math.max(0, rightLimitX);
    }

    /**
     * Repositions an open submenu after viewport
     * or toolbar layout changes.
     */
    public void updatePopupPlacement() {

        popup.updatePlacement(
                window.bounds(),
                ui.root().viewportSize(),
                popupRightLimitX);
    }

    public int firstVisibleIndex() {
        return pager.firstIndex();
    }

    public void open() {
        ui.windows().open(window);
    }

    public void close() {

        popup.hide();

        ui.cancelInput();
        ui.windows().close(window);
    }

    /**
     * Update after a mod/menu reload.
     */
    public void setMenus(MenuManager newMenus) {

        Objects.requireNonNull(newMenus);

        if (!newMenus.hasMenu(ROOT_MENU)) {
            throw new IllegalArgumentException(
                    "Missing bottom root menu");
        }

        menus = newMenus;
        pager.reset();
        rebuild();
        popup.setMenus(newMenus);
    }

    /**
     * Installs the same icon resolver used by
     * the new right-hand gameplay panel.
     */
    public void setIconResolver(
            Function<IconReference, UiSprite> resolver) {

        iconResolver = Objects.requireNonNull(
                resolver, "resolver");
        popup.setIconResolver(iconResolver);
        rebuild();
    }

    /**
     * Connects gameplay commands to toolbar buttons.
     *
     * This can handle original Towns commands or
     * registered MOD_ACTION callbacks.
     */
    public void setActionBinding(
            UiGameplayActionBinding binding) {

        actionBinding = Objects.requireNonNull(
                binding, "binding");
        popup.setActionBinding(actionBinding);
        rebuild();
    }

    /**
     * Optional sound or other click feedback.
     */
    public void setClickFeedback(Runnable feedback) {

        clickFeedback = Objects.requireNonNull(
                feedback, "feedback");
        popup.setClickFeedback(clickFeedback);
    }

    /**
     * Update button availability without replacing
     * the existing UI components.
     */
    public void refreshActionAvailability() {

        availabilityTracker.refresh();
        popup.refreshActionAvailability();
    }

    /**
     * Refresh after Java or XML mods change
     * the current menu registry.
     */
    public void refresh() {

        rebuild();
        popup.refresh();
    }

    private UiButton createArrow(String fallback) {

        UiButton button = new UiButton("", metrics);

        button.setMinimumSize(
                new UiSize(SCROLL_WIDTH, BUTTON_SIZE));

        button.setPreferredSize(
                new UiSize(SCROLL_WIDTH, BUTTON_SIZE));

        button.setPadding(UiInsets.NONE);

        // The arrow artwork already contains
        // its own background.
        button.setBackgroundVisible(false);

        button.setText(fallback);

        return button;
    }

    private void updateArrow(
            UiButton button,
            String iconId,
            String fallback) {

        UiSprite sprite = iconResolver.apply(
                new IconReference(iconId, IconType.UI));

        button.setIcon(sprite);

        if (sprite != null) {

            button.setText("");

            button.setIconSize(
                    new UiSize(
                            SCROLL_WIDTH,
                            BUTTON_SIZE));

        } else {
            button.setText(fallback);
        }
    }

    private UiButton createMenuButton(
            MenuItemDefinition item) {

        var resolved = resolver.resolve(item);

        UiButton button = new UiButton("", metrics);

        UiButtonAppearance appearance = "submenu".equals(item.getType())
                ? UiButtonAppearance.MENU_SUBMENU
                : UiButtonAppearance.MENU_ACTION;

        button.setAppearance(appearance);

        button.setMinimumSize(
                new UiSize(BUTTON_SIZE, BUTTON_SIZE));

        button.setPreferredSize(
                new UiSize(BUTTON_SIZE, BUTTON_SIZE));

        button.setPadding(UiInsets.NONE);

        UiSprite icon = iconResolver.apply(resolved.icon());

        if (icon != null) {

            button.setIcon(icon);

            int width = icon.source().width();
            int height = icon.source().height();

            int largest = Math.max(width, height);

            button.setIconSize(new UiSize(
                    Math.max(1, width * ICON_SIZE / largest),
                    Math.max(1, height * ICON_SIZE / largest)));

        } else {

            // Keep missing mod icons visible.
            button.setText("?");
        }

        button.setTooltip(resolved.title());

        if ("submenu".equals(item.getType())) {

            String target = item.getTargetMenu();

            button.setTooltip(
                    resolved.title() + "\nOpen submenu");

            if (target == null || !menus.hasMenu(target)) {

                button.setEnabled(false);

            } else {

                button.setOnClick(() -> {

                    clickFeedback.run();

                    popup.showMenu(target);
                    updatePopupPlacement();
                });
            }

            return button;
        }

        if ("action".equals(item.getType())) {

            button.setOnClick(
                    () -> executeAction(item, button));

            // Recheck availability without replacing
            // the button or resetting the toolbar.
            availabilityTracker.track(
                    button,
                    () -> actionBinding.isAvailable(item),
                    exception -> ModUiErrors.report(
                            "open-towns",
                            "Bottom toolbar availability",
                            "Cannot check availability of "
                                    + item.getId(),
                            exception));

            return button;
        }

        // Unsupported entry types remain visible
        // but cannot execute anything.
        button.setEnabled(false);

        return button;

    }

    /**
     * Executes one bottom-toolbar action.
     *
     * Availability is checked again at click time
     * because gameplay state or mod registrations
     * may have changed since the last refresh.
     */
    private void executeAction(
            MenuItemDefinition item,
            UiButton button) {

        try {

            if (!actionBinding.isAvailable(item)) {

                button.setEnabled(false);
                return;
            }

            clickFeedback.run();

            actionBinding.execute(item);

        } catch (RuntimeException exception) {

            ModUiErrors.report(
                    "open-towns",
                    "Bottom toolbar action",
                    "Failed to execute "
                            + item.getId()
                            + " ("
                            + item.getCommand()
                            + ")",
                    exception);
        }
    }

    private void rebuild() {

        // The previous visible buttons are about
        // to be replaced. Remove their old checks.
        availabilityTracker.clear();

        MenuDefinition menu = menus.requireMenu(ROOT_MENU);

        var items = menu.getItems().stream()
                .filter(item -> !"spacer".equals(item.getType())
                        && !"back".equals(item.getType()))
                .toList();

        var visible = pager.visibleItems(items);

        buttonRow.clear();

        for (MenuItemDefinition item : visible) {
            buttonRow.add(createMenuButton(item));
        }

        leftArrow.setEnabled(pager.canScrollLeft());
        rightArrow.setEnabled(pager.canScrollRight());

        updateArrow(
                leftArrow,
                "bottom_scr_left",
                "<");

        updateArrow(
                rightArrow,
                "bottom_scr_right",
                ">");
    }
}
