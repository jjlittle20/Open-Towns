
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuManager;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiSize;

class LegacyGameplayUiBridgeTest {

    private static final UiInsets RIGHT_INSETS = new UiInsets(190, 8, 80, 8);

    private static class Fixture {

        final UiSystem ui = new UiSystem();

        final MenuManager menus = new MenuManager();

        final AtomicBoolean inGameplay = new AtomicBoolean(true);

        final AtomicBoolean legacyOverrides = new AtomicBoolean(false);

        final RightMenuController right;

        final BottomToolbarController bottom;

        final LegacyGameplayUiBridge bridge;

        Fixture() {

            menus.registerMenu(
                    new MenuDefinition(
                            UiRightMenuPreview.ROOT_MENU,
                            null,
                            "Construction"));

            menus.registerMenu(
                    new MenuDefinition(
                            UiBottomMenuPreview.ROOT_MENU,
                            null,
                            "Actions"));

            right = RightMenuController.forTesting(
                    ui,
                    manager -> new UiRightMenuPreview(
                            ui,
                            manager,
                            new UiGameplayMenuResolver(
                                    key -> key,
                                    (command, parameter) -> null),
                            text -> new UiSize(
                                    text.length() * 8, 16),
                            key -> key));

            bottom = new BottomToolbarController(
                    ui,
                    (MenuManager manager) -> new UiBottomMenuPreview(
                            ui,
                            manager,
                            new UiGameplayMenuResolver(
                                    key -> key,
                                    (command, parameter) -> null),
                            text -> new UiSize(
                                    text.length() * 8, 16)));

            bridge = new LegacyGameplayUiBridge(
                    right,
                    bottom,
                    true,
                    true,
                    legacyOverrides::get,
                    inGameplay::get,
                    () -> menus);

            right.update(
                    menus, true, RIGHT_INSETS);

            bottom.update(
                    menus, true, true, -1);
        }
    }

    @Test
    void bothPanelsOwnTheirLegacyAreas() {

        Fixture fixture = new Fixture();

        assertTrue(
                fixture.bridge.isXmlRightPanelMode());

        assertTrue(
                fixture.bridge.isXmlBottomPanelMode());

        assertTrue(
                fixture.bridge.isXmlRightPanelActive());
    }

    @Test
    void hiddenRightPanelStillOwnsItsArea() {

        Fixture fixture = new Fixture();

        fixture.bridge.toggleXmlRightPanel();

        assertFalse(
                fixture.bridge.isXmlRightPanelActive());

        // The legacy right panel must not
        // appear merely because XML is hidden.
        assertTrue(
                fixture.bridge.isXmlRightPanelMode());

        fixture.bridge.toggleXmlRightPanel();

        assertTrue(
                fixture.bridge.isXmlRightPanelActive());
    }

    @Test
    void legacyOverridesDisableOnlyRightPanel() {

        Fixture fixture = new Fixture();

        fixture.legacyOverrides.set(true);

        assertFalse(
                fixture.bridge.isXmlRightPanelMode());

        assertTrue(
                fixture.bridge.isXmlBottomPanelMode());

        // The original right toggle should not
        // affect an XML panel in legacy mode.
        fixture.bridge.toggleXmlRightPanel();

        assertTrue(fixture.right.isOpen());
    }

    @Test
    void noGameplayDisablesCompatibilityModes() {

        Fixture fixture = new Fixture();

        fixture.inGameplay.set(false);

        assertFalse(
                fixture.bridge.isXmlRightPanelMode());

        assertFalse(
                fixture.bridge.isXmlBottomPanelMode());

        assertFalse(
                fixture.bridge.isXmlRightPanelActive());

        assertFalse(
                fixture.bridge.isXmlBottomPopupOpen());
    }
}
