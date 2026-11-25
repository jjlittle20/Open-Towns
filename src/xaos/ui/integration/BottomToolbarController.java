
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiInsets;
import xaos.ui.rendering.opengl.TownsUiTextMetrics;
import xaos.utils.Messages;
import xaos.utils.UtilsAL;

/**
 * Owns the lifecycle and gameplay configuration
 * of the XML bottom toolbar.
 *
 * Game supplies gameplay state; this controller
 * manages the corresponding UI components.
 */
public final class BottomToolbarController {

    private final UiSystem ui;

    private final Function<MenuManager, UiBottomMenuPreview> previewFactory;

    private UiBottomMenuPreview preview;

    public BottomToolbarController(
            UiSystem ui,
            Predicate<MenuItemDefinition> selectedAction) {

        this(
                ui,
                (Function<MenuManager, UiBottomMenuPreview>) menus -> createConfiguredPreview(
                        ui,
                        menus,
                        selectedAction));
    }

    // Package-private seam for isolated tests.
    BottomToolbarController(
            UiSystem ui,
            Function<MenuManager, UiBottomMenuPreview> factory) {

        this.ui = Objects.requireNonNull(ui);
        this.previewFactory = Objects.requireNonNull(factory);
    }

    private static UiBottomMenuPreview createConfiguredPreview(
            UiSystem ui,
            MenuManager menus,
            Predicate<MenuItemDefinition> selectedAction) {

        UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                Messages::getString,
                new UiLegacyGameplayDefinitionLookup());

        UiBottomMenuPreview toolbar = new UiBottomMenuPreview(
                ui,
                menus,
                resolver,
                TownsUiTextMetrics.INSTANCE);

        UiLegacyIconSprites icons = UiLegacyIconSprites.fromGraphicsIni();

        toolbar.setIconResolver(icons::resolve);

        toolbar.window().setAnchorInsets(
                new UiInsets(0, 0, 32, 0));

        toolbar.setActionBinding(
                new UiRegisteredGameplayActionBinding(
                        new UiLegacyBottomActionBinding()));

        toolbar.setSelectedActionPredicate(
                selectedAction);

        toolbar.setClickFeedback(
                () -> UtilsAL.play(
                        UtilsAL.SOURCE_FX_CLICK));

        return toolbar;
    }

    /**
     * Called once each game-loop iteration.
     *
     * rightPanelX is -1 when the right-hand
     * panel is not visible.
     */
    public void update(
            MenuManager menus,
            boolean inGameplay,
            boolean toolbarVisible,
            int rightPanelX) {

        boolean hasMenu = menus != null
                && menus.hasMenu(
                        UiBottomMenuPreview.ROOT_MENU);

        if (!inGameplay || !hasMenu) {

            if (preview != null
                    && preview.window().isOpen()) {

                preview.close();
            }

            return;
        }

        if (preview == null) {

            preview = previewFactory.apply(menus);

        } else if (preview.menus() != menus) {

            preview.setMenus(menus);
        }

        // Keep the original Towns toggle state.
        if (toolbarVisible) {

            if (!preview.window().isOpen()) {
                preview.open();
            }

        } else if (preview.window().isOpen()) {

            preview.close();
        }

        preview.refreshActionAvailability();

        // Keep popups clear of the right panel.
        int rightLimit = ui.root().viewportSize().width() - 8;

        if (rightPanelX >= 0) {

            rightLimit = Math.min(
                    rightLimit,
                    rightPanelX - 8);
        }

        preview.setPopupRightLimit(rightLimit);
        preview.updatePopupPlacement();
    }

    public boolean isConfiguredFor(MenuManager menus) {

        return preview != null
                && menus != null
                && menus.hasMenu(
                        UiBottomMenuPreview.ROOT_MENU);
    }

    public boolean isPopupOpen() {

        return preview != null
                && preview.popup().isOpen();
    }

    public void refreshIfUsing(MenuManager menus) {

        if (preview != null
                && preview.menus() == menus) {

            preview.refresh();
        }
    }

    // For tests and future integration work.
    UiBottomMenuPreview preview() {
        return preview;
    }
}
