
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
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiSystem;
import xaos.ui.integration.UiGameplayMenuResolver.IconReference;
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
 * Temporary preview of XML-defined gameplay menus.
 *
 * Reads the same menu registry that mods extend.
 *
 * Does not execute gameplay commands or replace
 * the legacy RightPanel.
 */
public final class UiRightMenuPreview {

    public static final String ROOT_MENU = "game.right.root";

    private final UiSystem ui;
    private final UiWindow window;
    private final UiButton backButton;
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

        window.setPosition(new UiPoint(30, 80));
        window.setUserSize(new UiSize(360, 440));

        window.setDraggable(true);
        window.setResizable(true);
        window.setModal(false);

        window.setCloseAction(
                metrics,
                () -> ui.windows().close(window));

        window.content().setPadding(UiInsets.all(6));
        window.content().setSpacing(6);

        backButton = new UiButton("Back", metrics);
        backButton.setOnClick(this::goBack);

        window.content().add(backButton);

        window.content().add(new UiLabel(
                "XML gameplay menu (in development)",
                metrics));

        scroll = new UiScrollContainer();
        scroll.content().setSpacing(4);

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

        scroll.content().clear();
        scroll.scrollTo(0);

        MenuDefinition menu = menus.requireMenu(currentMenuId);

        window.setTitle(
                "Actions: " + menuTitle(menu));

        backButton.setEnabled(!history.isEmpty());

        for (MenuItemDefinition item : menu.getItems()) {

            String type = item.getType();

            if ("back".equals(type)
                    || "spacer".equals(type)) {
                continue;
            }

            String label = resolveTitle(item);

            if ("submenu".equals(type)) {

                String target = item.getTargetMenu();

                UiButton button = new UiButton(
                        "> " + label, metrics);
                button.setTooltip(
                        label + "\nOpen submenu");
                applyIcon(button, item);
                boolean available = target != null
                        && menus.hasMenu(target);

                button.setEnabled(available);

                if (available) {
                    button.setOnClick(
                            () -> navigate(target));
                }

                scroll.content().add(button);

            } else if ("action".equals(type)) {

                UiButton button = new UiButton(label, metrics);
                button.setTooltip(
                        resolveTooltip(item, label));
                applyIcon(button, item);
                button.setEnabled(canExecute(item));

                button.setOnClick(() -> {

                    // Availability might have changed
                    // since the button was constructed.
                    if (!canExecute(item)) {
                        button.setEnabled(false);
                        return;
                    }

                    try {
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
                });

                scroll.content().add(button);

            } else {

                // Preserve visibility of unsupported
                // entries rather than losing them.
                scroll.content().add(
                        new UiLabel(label, metrics));
            }
        }
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
                    sourceWidth * 32 / largest);

            int iconHeight = Math.max(
                    1,
                    sourceHeight * 32 / largest);

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

}
