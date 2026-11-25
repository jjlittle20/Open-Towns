
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.Function;

import xaos.panels.menus.MenuManager;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiInsets;
import xaos.ui.rendering.opengl.TownsUiTextMetrics;
import xaos.utils.Messages;
import xaos.utils.UtilsAL;

/**
 * Owns the XML right-hand gameplay menu.
 *
 * Game supplies lifecycle and layout information.
 * The controller manages the actual UI window.
 */
public final class RightMenuController {

    private final UiSystem ui;

    private final Function<MenuManager, UiRightMenuPreview> previewFactory;

    private UiRightMenuPreview preview;

    private boolean wasInGameplay;

    private RightMenuController(
            UiSystem ui,
            Function<MenuManager, UiRightMenuPreview> factory) {

        this.ui = Objects.requireNonNull(ui);
        this.previewFactory = Objects.requireNonNull(factory);
    }

    /**
     * Production configuration.
     */
    public static RightMenuController forGame(UiSystem ui) {

        Objects.requireNonNull(ui);

        return new RightMenuController(
                ui,
                menus -> createGamePreview(ui, menus));
    }

    /**
     * Test constructor with an injected preview factory.
     */
    static RightMenuController forTesting(
            UiSystem ui,
            Function<MenuManager, UiRightMenuPreview> factory) {

        return new RightMenuController(ui, factory);
    }

    private static UiRightMenuPreview createGamePreview(
            UiSystem ui,
            MenuManager menus) {

        UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                Messages::getString,
                new UiLegacyGameplayDefinitionLookup());

        UiRightMenuPreview panel = new UiRightMenuPreview(
                ui,
                menus,
                resolver,
                TownsUiTextMetrics.INSTANCE,
                Messages::getString);

        panel.setActionBinding(
                new UiRegisteredGameplayActionBinding(
                        new UiLegacyGameplayActionBinding()));

        UiLegacyIconSprites icons = UiLegacyIconSprites.fromGraphicsIni();

        panel.setIconResolver(icons::resolve);

        panel.setTooltipResolver(
                new UiLegacyGameplayTooltipResolver());

        panel.setClickFeedback(
                () -> UtilsAL.play(
                        UtilsAL.SOURCE_FX_CLICK));

        return panel;
    }

    /**
     * Called once per game-loop iteration.
     *
     * inGameplay already accounts for any
     * legacy menu compatibility restrictions.
     */
    public void update(
            MenuManager menus,
            boolean inGameplay,
            UiInsets insets) {

        boolean available = inGameplay
                && menus != null
                && menus.hasMenu(
                        UiRightMenuPreview.ROOT_MENU);

        if (!available) {

            if (wasInGameplay && preview != null) {
                preview.close();
            }

            wasInGameplay = false;
            return;
        }

        Objects.requireNonNull(insets);

        if (preview == null) {

            preview = previewFactory.apply(menus);

            // Dock only after the panel is created
            // and its gameplay bindings are installed.
            preview.dockRight(insets);

        } else if (preview.menus() != menus) {

            preview.setMenus(menus);
        }

        // Keep docking margins current after
        // viewport or gameplay UI changes.
        if (!preview.window()
                .anchorInsets().equals(insets)) {

            preview.window().setAnchorInsets(insets);
        }

        // Only automatically open when entering
        // gameplay. Respect manual closing.
        if (!wasInGameplay) {
            preview.open();
        }

        wasInGameplay = true;
    }

    public boolean isConfiguredFor(MenuManager menus) {

        return preview != null
                && menus != null
                && menus.hasMenu(
                        UiRightMenuPreview.ROOT_MENU);
    }

    public boolean isOpen() {
        return preview != null
                && preview.window().isOpen();
    }

    /**
     * Returns -1 when the right panel is hidden.
     */
    public int openLeftEdge() {

        return isOpen()
                ? preview.window().bounds().x()
                : -1;
    }

    public void toggle() {

        if (preview == null) {
            return;
        }

        if (preview.window().isOpen()) {

            ui.cancelInput();
            preview.close();

        } else {

            preview.open();
        }
    }

    public void refreshIfUsing(MenuManager menus) {

        if (preview != null
                && preview.menus() == menus) {

            preview.refreshCurrentMenu();
        }
    }

    UiRightMenuPreview preview() {
        return preview;
    }

    /**
     * Refreshes the availability of visible
     * gameplay actions without rebuilding the menu.
     */
    public void refreshActionAvailability() {

        if (preview != null && preview.window().isOpen()) {
            preview.refreshActionAvailability();
        }
    }

}
