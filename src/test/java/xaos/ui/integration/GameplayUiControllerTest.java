
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuManager;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiSize;

class GameplayUiControllerTest {

    private static final UiInsets RIGHT_INSETS = new UiInsets(190, 8, 80, 8);

    private static MenuManager menus() {

        MenuManager manager = new MenuManager();

        manager.registerMenu(
                new MenuDefinition(
                        UiRightMenuPreview.ROOT_MENU,
                        null,
                        "Construction"));

        manager.registerMenu(
                new MenuDefinition(
                        UiBottomMenuPreview.ROOT_MENU,
                        null,
                        "Actions"));

        return manager;
    }

    private record Fixture(
            GameplayUiController coordinator,
            RightMenuController right,
            BottomToolbarController bottom) {
    }

    private static Fixture fixture() {

        UiSystem ui = new UiSystem();

        RightMenuController right = RightMenuController.forTesting(
                ui,
                menus -> new UiRightMenuPreview(
                        ui,
                        menus,
                        new UiGameplayMenuResolver(
                                key -> key,
                                (command, parameter) -> null),
                        text -> new UiSize(
                                text.length() * 8, 16),
                        key -> key));

        BottomToolbarController bottom = new BottomToolbarController(
                ui,
                (MenuManager menus) -> new UiBottomMenuPreview(
                        ui,
                        menus,
                        new UiGameplayMenuResolver(
                                key -> key,
                                (command, parameter) -> null),
                        text -> new UiSize(
                                text.length() * 8, 16)));

        return new Fixture(
                new GameplayUiController(right, bottom),
                right,
                bottom);
    }

    @Test
    void activatesBothPanelsDuringGameplay() {

        Fixture fixture = fixture();

        fixture.coordinator().update(
                menus(),
                true,
                true,
                false,
                true,
                true,
                () -> RIGHT_INSETS);

        assertTrue(fixture.right().isOpen());

        assertTrue(
                fixture.bottom()
                        .preview()
                        .window()
                        .isOpen());
    }

    @Test
    void preservesManualRightPanelClosing() {

        Fixture fixture = fixture();

        MenuManager menus = menus();

        fixture.coordinator().update(
                menus, true, true, false,
                true, true, () -> RIGHT_INSETS);

        fixture.right().toggle();

        fixture.coordinator().update(
                menus, true, true, false,
                true, true, () -> RIGHT_INSETS);

        assertFalse(fixture.right().isOpen());

        assertTrue(
                fixture.bottom()
                        .preview()
                        .window()
                        .isOpen());
    }

    @Test
    void legacyOverridesDisableOnlyRightPanel() {

        Fixture fixture = fixture();

        MenuManager menus = menus();

        fixture.coordinator().update(
                menus, true, true, true,
                true, true, () -> RIGHT_INSETS);

        assertFalse(fixture.right().isOpen());

        assertTrue(
                fixture.bottom()
                        .preview()
                        .window()
                        .isOpen());
    }

    @Test
    void doesNotRequestInsetsOutsideGameplay() {

        Fixture fixture = fixture();

        fixture.coordinator().update(
                menus(),
                false,
                true,
                false,
                true,
                false,
                () -> {
                    throw new AssertionError(
                            "Insets must not be calculated");
                });

        assertFalse(fixture.right().isOpen());
    }
}
