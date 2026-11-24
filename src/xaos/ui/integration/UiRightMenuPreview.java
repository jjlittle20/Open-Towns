
package xaos.ui.integration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import xaos.internal.api.mods.ModUiErrors;
import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;

import xaos.ui.components.UiButton;
import xaos.ui.components.UiButtonAppearance;
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiButtonAvailabilityTracker;
import xaos.ui.core.UiSystem;
import xaos.ui.integration.UiGameplayMenuResolver.IconReference;
import xaos.ui.layout.UiGridLayout;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiWindowAnchor;
import xaos.ui.layout.UiWindowDockMode;
import xaos.ui.rendering.UiSprite;
import xaos.ui.rendering.UiTextMetrics;
import java.util.function.Function;

import xaos.ui.rendering.UiSprite;
import xaos.ui.integration.UiGameplayMenuResolver.IconReference;

/**
 * XML-driven right-hand gameplay menu.
 *
 * Uses the shared menu registry, including
 * contributions from enabled mods.
 *
 * Executes actions through an injected binding
 * and can replace the legacy RightPanel.
 */
public final class UiRightMenuPreview {

    public static final String ROOT_MENU = "game.right.root";
    private static final int BUTTON_SIZE = 64;
    private static final int ICON_SIZE = 52;
    private static final int BUTTON_SPACING = 16;

    private final UiSystem ui;
    private final UiWindow window;

    private final UiScrollContainer scroll;

    private final UiGameplayMenuResolver resolver;

    private UiGameplayActionBinding actionBinding = UiGameplayActionBinding.disabled();
    // Resolves menu icon references into UI sprites.
    // Defaults to text-only buttons.
    private Function<IconReference, UiSprite> iconResolver = reference -> null;

    private BiFunction<MenuItemDefinition, String, String> tooltipResolver = (item, title) -> title;

    /**
     * Installs a resolver for action descriptions
     * and requirements.
     */
    public void setTooltipResolver(
            BiFunction<MenuItemDefinition, String, String> resolver) {

        tooltipResolver = Objects.requireNonNull(
                resolver, "resolver");

        rebuild();
    }

    private final UiTextMetrics metrics;
    private final Function<String, String> localize;

    private final Deque<String> history = new ArrayDeque<>();

    private MenuManager menus;
    private String currentMenuId = ROOT_MENU;
    private final UiButtonAvailabilityTracker availabilityTracker = new UiButtonAvailabilityTracker();
    private Runnable clickFeedback = () -> {
    };

    public UiRightMenuPreview(
            UiSystem ui,
            MenuManager menus,
            UiGameplayMenuResolver resolver,
            UiTextMetrics metrics,
            Function<String, String> localize) {

        this.ui = Objects.requireNonNull(ui);
        this.menus = Objects.requireNonNull(menus);
        this.resolver = Objects.requireNonNull(resolver);
        this.metrics = Objects.requireNonNull(metrics);
        this.localize = Objects.requireNonNull(localize);

        window = new UiWindow("Right Menu Preview");

        // The docked gameplay panel does not need
        // a floating window's title bar.
        //
        // Hiding the container also removes its
        // 24-pixel height from the window layout.
        window.titleBar().setVisible(false);

        window.setPosition(new UiPoint(30, 80));
        window.setUserSize(new UiSize(200, 440));

        window.setDraggable(true);
        window.setResizable(true);
        window.setModal(false);

        window.setCloseAction(
                metrics,
                () -> ui.windows().close(window));

        window.content().setPadding(UiInsets.all(6));
        window.content().setSpacing(6);

        scroll = new UiScrollContainer();

        scroll.content().setLayoutStrategy(
                new UiGridLayout(2, true));

        scroll.content().setSpacing(
                BUTTON_SPACING);

        window.content().add(scroll);

        rebuild();

        // Start hidden. The game opens it when
        // the player enters an active world.
        window.close();
        ui.windows().register(window);
    }

    public UiWindow window() {
        return window;
    }

    public MenuManager menus() {
        return menus;
    }

    public String currentMenuId() {
        return currentMenuId;
    }

    public void open() {
        ui.windows().open(window);
    }

    public void close() {
        ui.windows().close(window);
    }

    /**
     * Replace the registry after a world/mod reload.
     * Navigation resets to the root menu.
     */
    public void setMenus(MenuManager newMenus) {

        Objects.requireNonNull(newMenus);

        if (menus == newMenus) {
            return;
        }

        menus = newMenus;
        currentMenuId = ROOT_MENU;
        history.clear();

        rebuild();
    }

    /**
     * Refreshes the current menu without resetting
     * its navigation history.
     *
     * Used after Java mods add or remove items.
     */
    public void refreshCurrentMenu() {
        rebuild();
    }

    /**
     * Installs the action handler used by this window.
     *
     * Rebuilds the currently displayed menu so action
     * buttons reflect their availability.
     */
    public void setActionBinding(
            UiGameplayActionBinding binding) {

        actionBinding = Objects.requireNonNull(
                binding, "binding");

        rebuild();
    }

    /**
     * Installs the icon resolver for gameplay buttons.
     *
     * The preview remains usable without one.
     */
    public void setIconResolver(
            Function<IconReference, UiSprite> resolver) {

        iconResolver = Objects.requireNonNull(
                resolver, "resolver");

        rebuild();
    }

    private void navigate(String targetMenu) {

        // Broken submenu references must never
        // crash the gameplay UI.
        if (!menus.hasMenu(targetMenu)) {
            return;
        }

        history.addLast(currentMenuId);
        currentMenuId = targetMenu;

        rebuild();
    }

    private void goBack() {

        if (history.isEmpty()) {
            return;
        }

        currentMenuId = history.removeLast();
        rebuild();
    }

    private void rebuild() {
        // Old buttons are about to be replaced.
        // Their availability checks must not
        // remain registered.
        availabilityTracker.clear();
        scroll.content().clear();
        scroll.scrollTo(0);

        MenuDefinition menu = menus.requireMenu(currentMenuId);

        window.setTitle(
                "Actions: " + menuTitle(menu));
        boolean addedBack = false;
        for (MenuItemDefinition item : menu.getItems()) {

            String type = item.getType();

            if ("back".equals(type)) {
                addedBack = true;
                if (!history.isEmpty()) {

                    UiButton button = createMenuButton(
                            item,
                            resolveTitle(item),
                            UiButtonAppearance.MENU_ACTION);

                    button.setOnClick(
                            withClickFeedback(this::goBack));

                    scroll.content().add(button);
                }

                continue;
            }

            if ("spacer".equals(type)) {
                continue;
            }

            String label = resolveTitle(item);

            if ("submenu".equals(type)) {

                String target = item.getTargetMenu();

                UiButton button = createMenuButton(
                        item,
                        label,
                        UiButtonAppearance.MENU_SUBMENU);
                button.setTooltip(
                        label + "\nOpen submenu");
                applyIcon(button, item);
                boolean available = target != null
                        && menus.hasMenu(target);

                button.setEnabled(available);

                if (available) {
                    button.setOnClick(
                            withClickFeedback(
                                    () -> navigate(target)));
                }

                scroll.content().add(button);

            } else if ("action".equals(type)) {

                UiButton button = createMenuButton(
                        item,
                        label,
                        UiButtonAppearance.MENU_ACTION);

                final String constructionTooltip = resolveTooltip(item, label);

                button.setTooltip(constructionTooltip);

                applyIcon(button, item);

                availabilityTracker.track(
                        button,
                        () -> {
                            UiActionAvailability availability = actionBinding.availability(item);

                            button.setTooltip(
                                    tooltipWithAvailability(
                                            constructionTooltip,
                                            availability));

                            return availability.canExecute();
                        },
                        exception -> {
                            button.setTooltip(
                                    tooltipWithAvailability(
                                            constructionTooltip,
                                            UiActionAvailability.unknown(
                                                    "Availability check failed. "
                                                            + "See mod errors (F8).")));

                            ModUiErrors.report(
                                    "open-towns",
                                    "Gameplay action availability",
                                    "Failed to check " + item.getId(),
                                    exception);
                        });

                button.setOnClick(withClickFeedback(() -> {

                    try {

                        // Recheck immediately before execution.
                        // The state may have changed since the
                        // previous frame.
                        if (!updateActionAvailability(
                                button,
                                item,
                                constructionTooltip)) {

                            button.setEnabled(false);
                            return;
                        }

                        actionBinding.execute(item);

                    } catch (RuntimeException exception) {

                        ModUiErrors.report(
                                "open-towns",
                                "Gameplay menu action",
                                "Failed to execute "
                                        + item.getId()
                                        + " ("
                                        + item.getCommand()
                                        + ")",
                                exception);
                    }
                }));

                scroll.content().add(button);
            } else {
                UiButton unsupported = createMenuButton(
                        item,
                        label,
                        UiButtonAppearance.MENU_ACTION);

                unsupported.setTooltip(
                        label + "\nUnsupported menu entry");

                unsupported.setEnabled(false);

                scroll.content().add(unsupported);
            }
        }
        // Provide navigation for mod-created menus
        // that do not include an explicit Back item.
        if (!history.isEmpty() && !addedBack) {

            UiButton back = new UiButton("<", metrics);

            back.setAppearance(
                    UiButtonAppearance.MENU_ACTION);

            back.setMinimumSize(
                    new UiSize(BUTTON_SIZE, BUTTON_SIZE));

            back.setPreferredSize(
                    new UiSize(BUTTON_SIZE, BUTTON_SIZE));

            back.setPadding(UiInsets.NONE);
            back.setTooltip("Back");

            back.setOnClick(
                    withClickFeedback(this::goBack));

            scroll.content().add(back);
        }

    }

    /**
     * Recheck visible gameplay actions without
     * rebuilding the menu.
     *
     * Preserves navigation, button instances,
     * tooltips and scroll position.
     */
    public void refreshActionAvailability() {

        if (!window.isOpen()) {
            return;
        }

        availabilityTracker.refresh();
    }

    /**
     * Turns the XML menu window into a fixed
     * right-hand gameplay panel.
     *
     * The insets leave space for other gameplay
     * elements, including the minimap.
     */
    public void dockRight(UiInsets insets) {

        Objects.requireNonNull(insets, "insets");

        window.setCentered(false);

        window.setAnchor(UiWindowAnchor.RIGHT);
        window.setAnchorInsets(insets);

        window.setDockMode(
                UiWindowDockMode.FILL_HEIGHT);

        // This panel is positioned by the framework,
        // not by player dragging or resizing.
        window.setDraggable(false);
        window.setResizable(false);
    }

    private String menuTitle(MenuDefinition menu) {

        String key = menu.getTitleKey();

        if (hasText(key)) {

            String translated = localize.apply(key);

            if (hasText(translated)) {
                return translated;
            }
        }

        if (hasText(menu.getTitle())) {
            return menu.getTitle();
        }

        return menu.getId();
    }

    private String resolveTitle(MenuItemDefinition item) {

        try {
            return resolver.resolve(item).title();

        } catch (RuntimeException exception) {

            ModUiErrors.report(
                    "open-towns",
                    "Gameplay menu preview",
                    "Unable to resolve menu item: "
                            + item.getId(),
                    exception);

            return item.getId();
        }
    }

    private String resolveTooltip(
            MenuItemDefinition item,
            String title) {

        try {

            String tooltip = tooltipResolver.apply(item, title);

            return tooltip == null || tooltip.isBlank()
                    ? title
                    : tooltip;

        } catch (RuntimeException exception) {

            ModUiErrors.report(
                    "open-towns",
                    "Gameplay tooltip resolution",
                    "Could not resolve tooltip for "
                            + item.getId(),
                    exception);

            return title;
        }
    }

    private boolean canExecute(
            MenuItemDefinition item) {

        try {
            return actionBinding.isAvailable(item);

        } catch (RuntimeException exception) {

            ModUiErrors.report(
                    "open-towns",
                    "Gameplay action availability",
                    "Failed to check " + item.getId(),
                    exception);

            return false;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Creates a fixed-size icon button for gameplay
     * menus.
     *
     * The active theme determines its background
     * appearance.
     */
    private UiButton createMenuButton(
            MenuItemDefinition item,
            String label,
            UiButtonAppearance appearance) {

        UiButton button = new UiButton("", metrics);

        button.setAppearance(appearance);

        button.setMinimumSize(
                new UiSize(BUTTON_SIZE, BUTTON_SIZE));

        button.setPreferredSize(
                new UiSize(BUTTON_SIZE, BUTTON_SIZE));

        // The icon is centred within the circle.
        // Its maximum size is 52x52.
        button.setPadding(UiInsets.NONE);

        applyIcon(button, item);

        // Do not make actions disappear when a
        // mod forgets to provide an icon.
        if (button.icon() == null) {
            button.setText("?");
        }

        button.setTooltip(label);

        return button;
    }

    /**
     * Attaches an icon from the menu definition
     * or its resolved gameplay definition.
     *
     * Icons fit within 32 x 32 UI units while
     * preserving their original aspect ratio.
     */
    private void applyIcon(
            UiButton button,
            MenuItemDefinition item) {

        try {

            var presentation = resolver.resolve(item);

            if (presentation.icon() == null) {
                return;
            }

            UiSprite sprite = iconResolver.apply(
                    presentation.icon());

            if (sprite == null) {
                return;
            }

            button.setIcon(sprite);

            int sourceWidth = sprite.source().width();
            int sourceHeight = sprite.source().height();

            int largest = Math.max(
                    sourceWidth, sourceHeight);

            int iconWidth = Math.max(
                    1,
                    sourceWidth * ICON_SIZE / largest);

            int iconHeight = Math.max(
                    1,
                    sourceHeight * ICON_SIZE / largest);

            button.setIconSize(
                    new UiSize(iconWidth, iconHeight));

        } catch (RuntimeException exception) {

            ModUiErrors.report(
                    "open-towns",
                    "Gameplay icon resolution",
                    "Failed to resolve icon for "
                            + item.getId(),
                    exception);
        }
    }

    /**
     * Combines ordinary construction information
     * with the current availability result.
     *
     * Blocking reasons appear first so they remain
     * visible even if a long tooltip is truncated.
     */
    static String tooltipWithAvailability(
            String constructionTooltip,
            UiActionAvailability availability) {

        Objects.requireNonNull(
                availability, "availability");

        String base = constructionTooltip == null
                ? ""
                : constructionTooltip;

        if (availability.canExecute()) {
            return base;
        }

        String heading = switch (availability.status()) {

            case UNAVAILABLE -> "Unavailable: ";

            case UNKNOWN -> "Availability unknown: ";

            case AVAILABLE -> "";
        };

        String explanation = heading + availability.reason();

        if (base.isBlank()) {
            return explanation;
        }

        return explanation + "\n\n" + base;
    }

    /**
     * Updates one action's tooltip and returns
     * whether it can currently be executed.
     *
     * Does not rebuild the button or its layout.
     */
    private boolean updateActionAvailability(
            UiButton button,
            MenuItemDefinition item,
            String constructionTooltip) {

        UiActionAvailability availability = Objects.requireNonNull(
                actionBinding.availability(item),
                "Action binding returned null availability");

        String updatedTooltip = tooltipWithAvailability(
                constructionTooltip,
                availability);

        // Avoid changing the tooltip unnecessarily
        // on every availability refresh.
        if (!Objects.equals(
                button.tooltip(),
                updatedTooltip)) {

            button.setTooltip(updatedTooltip);
        }

        return availability.canExecute();
    }

    /**
     * Installs click feedback for gameplay menu buttons.
     *
     * The UI framework does not depend directly
     * on the game's sound implementation.
     */
    public void setClickFeedback(Runnable feedback) {

        clickFeedback = Objects.requireNonNull(
                feedback, "feedback");
    }

    /**
     * Wraps an activated menu button's action
     * with its click feedback.
     */
    private Runnable withClickFeedback(Runnable action) {

        Objects.requireNonNull(action, "action");

        return () -> {
            clickFeedback.run();
            action.run();
        };
    }

}
