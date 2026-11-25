
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import xaos.panels.menus.MenuManager;

/**
 * Compatibility bridge between the original
 * Towns UI and the new gameplay UI controllers.
 *
 * Keeps legacy panel state checks and toggle
 * behaviour outside Game.java.
 *
 * Temporary until legacy panels have been
 * fully migrated to the new framework.
 */
public final class LegacyGameplayUiBridge {

    private final RightMenuController rightMenu;
    private final BottomToolbarController bottomToolbar;

    private final boolean rightEnabled;
    private final boolean bottomEnabled;

    private final BooleanSupplier legacyRightOverrides;
    private final BooleanSupplier inGameplay;

    private final Supplier<MenuManager> menus;

    public LegacyGameplayUiBridge(
            RightMenuController rightMenu,
            BottomToolbarController bottomToolbar,
            boolean rightEnabled,
            boolean bottomEnabled,
            BooleanSupplier legacyRightOverrides,
            BooleanSupplier inGameplay,
            Supplier<MenuManager> menus) {

        this.rightMenu = Objects.requireNonNull(
                rightMenu);

        this.bottomToolbar = Objects.requireNonNull(
                bottomToolbar);

        this.rightEnabled = rightEnabled;
        this.bottomEnabled = bottomEnabled;

        this.legacyRightOverrides = Objects.requireNonNull(
                legacyRightOverrides);

        this.inGameplay = Objects.requireNonNull(inGameplay);

        this.menus = Objects.requireNonNull(menus);
    }

    /**
     * The XML right panel owns the gameplay
     * menu even when manually hidden.
     */
    public boolean isXmlRightPanelMode() {

        return rightEnabled
                && !legacyRightOverrides.getAsBoolean()
                && rightMenu.isConfiguredFor(menus.get())
                && inGameplay.getAsBoolean();
    }

    /**
     * The XML bottom toolbar owns the bottom
     * menu even when collapsed.
     */
    public boolean isXmlBottomPanelMode() {

        return bottomEnabled
                && bottomToolbar.isConfiguredFor(menus.get())
                && inGameplay.getAsBoolean();
    }

    public boolean isXmlRightPanelActive() {

        return isXmlRightPanelMode()
                && rightMenu.isOpen();
    }

    public boolean isXmlBottomPopupOpen() {

        return isXmlBottomPanelMode()
                && bottomToolbar.isPopupOpen();
    }

    /**
     * Called by the original right-edge toggle.
     *
     * The controller retains responsibility
     * for opening and closing its window.
     */
    public void toggleXmlRightPanel() {

        if (!isXmlRightPanelMode()) {
            return;
        }

        rightMenu.toggle();
    }
}
