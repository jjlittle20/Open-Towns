
package xaos.ui.integration;

import xaos.ui.layout.UiInsets;

/**
 * Compatibility layout calculations for
 * original Towns gameplay UI elements.
 *
 * Keeps legacy panel geometry outside
 * Game.java and the reusable UI framework.
 */
public final class LegacyGameplayUiLayout {

    private LegacyGameplayUiLayout() {
    }

    /**
     * Calculates the right-hand gameplay panel
     * margins while reserving space for:
     *
     * - The minimap above
     * - The bottom toolbar below
     * - The original right-edge toggle
     *
     * All dimensions are supplied by the game.
     */
    public static UiInsets rightPanelInsets(
            int minimapY,
            int minimapHeight,
            int bottomPanelHeight,
            int viewportHeight,
            int toggleWidth) {

        int top = Math.max(
                8,
                minimapY + minimapHeight + 12);

        int bottom = bottomPanelHeight + 24;

        int availableHeight = Math.max(
                0,
                viewportHeight);

        // Keep the panel usable in smaller windows.
        bottom = Math.min(
                bottom,
                Math.max(0, availableHeight - 100));

        top = Math.min(
                top,
                Math.max(
                        0,
                        availableHeight - bottom - 100));

        return new UiInsets(
                top,
                toggleWidth,
                bottom,
                8);
    }
}
