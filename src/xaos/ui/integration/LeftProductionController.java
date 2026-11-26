
package xaos.ui.integration;

import java.util.Objects;

import xaos.panels.UI.UIPanelState;
import xaos.panels.menus.MenuManager;
import xaos.ui.layout.UiInsets;

/**
 * Manages the modern left production panel lifecycle.
 *
 * GameplayUiController owns this controller.
 * The controller owns the preview lifecycle.
 */
public final class LeftProductionController {

    private final UiLeftProductionPreview preview;

    private MenuManager currentMenus;
    private boolean inGameplay;

    public LeftProductionController(
            UiLeftProductionPreview preview) {

        this.preview = Objects.requireNonNull(
                preview, "preview");
    }

    /**
     * Called by the gameplay UI coordinator.
     *
     * Repeated calls must not reopen a window
     * that the player has manually closed.
     */
    public void update(
            MenuManager menus,
            boolean gameplayActive,
            UiInsets dockInsets) {

        if (!gameplayActive || menus == null) {
            leaveGameplay();
            return;
        }

        Objects.requireNonNull(dockInsets, "dockInsets");

        if (currentMenus != menus) {
            currentMenus = menus;
            preview.setMenus(menus);
        }

        preview.dockLeft(dockInsets);

        if (!inGameplay) {
            preview.open();
            inGameplay = true;
            preview.refreshCounts();
        }

        // Modern production retains ownership while in gameplay,
        // regardless of whether its window is open or closed.
        UIPanelState.setModernProductionPanelActive(
                true);
    }

    /**
     * Explicit menu reload, including reloads where
     * the MenuManager instance has not changed.
     */
    public void refreshMenus(MenuManager menus) {
        if (menus == null) {
            return;
        }

        currentMenus = menus;
        preview.setMenus(menus);
    }

    /**
     * Refresh live production counts.
     *
     * Does not rebuild the menu or change navigation.
     */
    public void refreshCounts() {
        if (inGameplay) {
            preview.refreshCounts();
        }
    }

    /**
     * Leave gameplay and restore legacy ownership.
     */
    public void leaveGameplay() {

        if (inGameplay) {
            preview.close();
            inGameplay = false;
        }

        currentMenus = null;

        UIPanelState.setModernProductionPanelActive(false);
    }

    public boolean isInGameplay() {
        return inGameplay;
    }

    public UiLeftProductionPreview preview() {
        return preview;
    }

    public boolean isOpen() {
        return inGameplay && preview.window().isOpen();
    }

    public void toggle() {
        if (!inGameplay) {
            return;
        }

        if (preview.window().isOpen()) {
            preview.close();
        } else {
            preview.open();
            preview.refreshCounts();
        }
    }

}
