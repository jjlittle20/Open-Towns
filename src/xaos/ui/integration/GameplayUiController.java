
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.Function;
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

    private final Function<MenuManager, LeftProductionController> leftFactory;

    private LeftProductionController leftProduction;

    public GameplayUiController(
            RightMenuController rightMenu,
            BottomToolbarController bottomToolbar) {

        this(rightMenu, bottomToolbar, null);
    }

    public GameplayUiController(
            RightMenuController rightMenu,
            BottomToolbarController bottomToolbar,
            Function<MenuManager, LeftProductionController> leftFactory) {

        this.rightMenu = Objects.requireNonNull(rightMenu);
        this.bottomToolbar = Objects.requireNonNull(bottomToolbar);
        this.leftFactory = leftFactory;
    }

    /**
     * Original method signature.
     *
     * Preserved for existing callers and tests.
     */
    public void update(
            MenuManager menus,
            boolean inGameplay,
            boolean rightEnabled,
            boolean legacyRightOverrides,
            boolean bottomEnabled,
            boolean bottomVisible,
            Supplier<UiInsets> rightInsets) {

        update(
                menus,
                inGameplay,
                rightEnabled,
                legacyRightOverrides,
                bottomEnabled,
                bottomVisible,
                false,
                rightInsets);
    }

    /**
     * Compatibility overload with left-panel support.
     *
     * Uses fallback margins until Game supplies
     * the actual gameplay HUD layout.
     */
    public void update(
            MenuManager menus,
            boolean inGameplay,
            boolean rightEnabled,
            boolean legacyRightOverrides,
            boolean bottomEnabled,
            boolean bottomVisible,
            boolean leftEnabled,
            Supplier<UiInsets> rightInsets) {

        update(
                menus,
                inGameplay,
                rightEnabled,
                legacyRightOverrides,
                bottomEnabled,
                bottomVisible,
                leftEnabled,
                rightInsets,
                () -> new UiInsets(96, 8, 96, 48));
    }

    /**
     * Full gameplay UI coordination.
     *
     * Update order:
     * 1. Right panel
     * 2. Bottom toolbar
     * 3. Left production panel
     */
    public void update(
            MenuManager menus,
            boolean inGameplay,
            boolean rightEnabled,
            boolean legacyRightOverrides,
            boolean bottomEnabled,
            boolean bottomVisible,
            boolean leftEnabled,
            Supplier<UiInsets> rightInsets,
            Supplier<UiInsets> leftInsets) {

        Objects.requireNonNull(rightInsets);
        Objects.requireNonNull(leftInsets);

        // --------------------------------------------------
        // Right panel
        // --------------------------------------------------

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

        // --------------------------------------------------
        // Bottom toolbar
        // --------------------------------------------------

        if (bottomEnabled) {

            int rightPanelX = useRightMenu
                    && rightMenu.isOpen()
                            ? rightMenu.openLeftEdge()
                            : -1;

            bottomToolbar.update(
                    menus,
                    inGameplay,
                    bottomVisible,
                    rightPanelX);
        }

        // --------------------------------------------------
        // Left production panel
        // --------------------------------------------------

        boolean useLeft = leftEnabled
                && inGameplay
                && menus != null
                && menus.hasMenu(
                        UiProductionMenuModel.ROOT_MENU);

        if (useLeft
                && leftProduction == null
                && leftFactory != null) {

            leftProduction = Objects.requireNonNull(
                    leftFactory.apply(menus),
                    "left production controller");
        }

        if (leftProduction != null) {

            leftProduction.update(
                    menus,
                    useLeft,
                    useLeft
                            ? leftInsets.get()
                            : UiInsets.NONE);

            if (useLeft) {
                leftProduction.refreshCounts();
            }
        }
    }

    /**
     * Whether the modern production panel is open.
     */
    public boolean isLeftProductionOpen() {

        return leftProduction != null
                && leftProduction.isOpen();
    }

    /**
     * Toggle the modern left production panel.
     */
    public void toggleLeftProduction() {

        if (leftProduction != null) {
            leftProduction.toggle();
        }
    }

    /**
     * Called after mods change the menu registry.
     */
    public void refreshMenus(MenuManager menus) {

        rightMenu.refreshIfUsing(menus);
        bottomToolbar.refreshIfUsing(menus);

        if (leftProduction != null) {
            leftProduction.refreshMenus(menus);
        }
    }

    /**
     * Refresh dynamic action availability.
     */
    public void refreshRightActionAvailability() {

        rightMenu.refreshActionAvailability();
    }
}
