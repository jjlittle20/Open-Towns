
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.Function;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;

import xaos.ui.components.UiButton;
import xaos.ui.components.UiButtonAppearance;
import xaos.ui.components.UiWindow;
import xaos.ui.components.UiWindowAppearance;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiLinearLayout;
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
    private static final int TOOLBAR_WIDTH = CONTENT_WIDTH + SCROLL_WIDTH * 2;

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
                UiLinearLayout.HORIZONTAL);

        // --------------------------------------------------
        // Left scroll arrow
        // --------------------------------------------------

        leftArrow = createArrow("<");

        leftArrow.setOnClick(() -> {
            if (pager.scrollLeft()) {
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
                rebuild();
            }
        });

        window.content().add(leftArrow);
        window.content().add(buttonRow);
        window.content().add(rightArrow);

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

    public int firstVisibleIndex() {
        return pager.firstIndex();
    }

    public void open() {
        ui.windows().open(window);
    }

    public void close() {
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
    }

    /**
     * Installs the same icon resolver used by
     * the new right-hand gameplay panel.
     */
    public void setIconResolver(
            Function<IconReference, UiSprite> resolver) {

        iconResolver = Objects.requireNonNull(
                resolver, "resolver");

        rebuild();
    }

    /**
     * Refresh after Java or XML mods change
     * the current menu registry.
     */
    public void refresh() {
        rebuild();
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

        // Visual-only milestone.
        // Enable interaction when gameplay commands
        // and submenu navigation are connected.
        button.setEnabled(false);

        return button;
    }

    private void rebuild() {

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
