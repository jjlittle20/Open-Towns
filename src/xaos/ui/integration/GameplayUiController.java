
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.Supplier;

import xaos.panels.menus.MenuManager;
import xaos.ui.layout.UiInsets;

/**
 * Coordinates the XML gameplay panels.
 *
 * Game supplies current gameplay state.
 * Individual controllers own their windows,
 * bindings and UI behaviour.
 *
 * This is an internal integration layer,
 * not part of the public modding API.
 */
public final class GameplayUiController {

    private final RightMenuController rightMenu;
    private final BottomToolbarController bottomToolbar;

    public GameplayUiController(
            RightMenuController rightMenu,
            BottomToolbarController bottomToolbar) {

        this.rightMenu = Objects.requireNonNull(rightMenu);

        this.bottomToolbar = Objects.requireNonNull(bottomToolbar);
    }

    /**
     * Update gameplay panels in their existing order:
     *
     * 1. Right panel
     * 2. Bottom toolbar
     *
     * The bottom toolbar needs the right panel's
     * position to avoid overlapping its popups.
     */
    public void update(
            MenuManager menus,
            boolean inGameplay,
            boolean rightEnabled,
            boolean legacyRightOverrides,
            boolean bottomEnabled,
            boolean bottomVisible,
            Supplier<UiInsets> rightInsets) {

        Objects.requireNonNull(rightInsets);

        boolean useRightMenu = rightEnabled
                && inGameplay
                && !legacyRightOverrides
                && menus != null
                && menus.hasMenu(
                        UiRightMenuPreview.ROOT_MENU);

        if (rightEnabled) {

            rightMenu.update(
                    menus,
                    useRightMenu,
                    useRightMenu
                            ? rightInsets.get()
                            : UiInsets.NONE);
        }

        if (bottomEnabled) {

            int rightPanelX = useRightMenu && rightMenu.isOpen()
                    ? rightMenu.openLeftEdge()
                    : -1;

            bottomToolbar.update(
                    menus,
                    inGameplay,
                    bottomVisible,
                    rightPanelX);
        }
    }

    /**
     * Called after mods change the menu registry.
     */
    public void refreshMenus(MenuManager menus) {

        rightMenu.refreshIfUsing(menus);
        bottomToolbar.refreshIfUsing(menus);
    }

    /**
     * Refresh dynamic action availability.
     */
    public void refreshRightActionAvailability() {

        rightMenu.refreshActionAvailability();
    }
}
