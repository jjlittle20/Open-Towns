
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.ui.layout.UiInsets;

class LegacyGameplayUiLayoutTest {

    @Test
    void reservesSpaceForMinimapAndBottomToolbar() {

        UiInsets insets = LegacyGameplayUiLayout.rightPanelInsets(
                40, // Minimap Y
                128, // Minimap height
                64, // Bottom panel height
                720, // Viewport height
                32); // Right toggle width

        assertEquals(
                new UiInsets(180, 32, 88, 8),
                insets);
    }

    @Test
    void clampsMarginsInSmallViewport() {

        UiInsets insets = LegacyGameplayUiLayout.rightPanelInsets(
                40,
                128,
                64,
                200,
                32);

        assertEquals(
                new UiInsets(12, 32, 88, 8),
                insets);
    }

    @Test
    void handlesZeroHeightViewport() {

        UiInsets insets = LegacyGameplayUiLayout.rightPanelInsets(
                40,
                128,
                64,
                0,
                32);

        assertEquals(
                new UiInsets(0, 32, 0, 8),
                insets);
    }

    @Test
    void retainsScaledToggleWidth() {

        UiInsets insets = LegacyGameplayUiLayout.rightPanelInsets(
                40,
                128,
                64,
                720,
                48);

        assertEquals(48, insets.right());
    }
}
