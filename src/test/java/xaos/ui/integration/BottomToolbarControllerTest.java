
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuManager;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiSize;

class BottomToolbarControllerTest {

    private static MenuManager menus() {

        MenuManager menus = new MenuManager();

        menus.registerMenu(
                new MenuDefinition(
                        UiBottomMenuPreview.ROOT_MENU,
                        null,
                        "Actions"));

        return menus;
    }

    private static BottomToolbarController controller(
            UiSystem ui) {

        return new BottomToolbarController(
                ui,
                (MenuManager menus) -> new UiBottomMenuPreview(
                        ui,
                        menus,
                        new UiGameplayMenuResolver(
                                key -> key,
                                (command, parameter) -> null),
                        text -> new UiSize(
                                text.length() * 8,
                                16)));
    }

    @Test
    void createsAndShowsToolbarDuringGameplay() {

        UiSystem ui = new UiSystem();

        BottomToolbarController controller = controller(ui);

        MenuManager menus = menus();

        controller.update(
                menus, true, true, -1);

        ui.updateLayout(new UiSize(1400, 900));

        assertTrue(controller.isConfiguredFor(menus));
        assertTrue(controller.preview().window().isOpen());
    }

    @Test
    void followsLegacyVisibilityState() {

        UiSystem ui = new UiSystem();

        BottomToolbarController controller = controller(ui);

        MenuManager menus = menus();

        controller.update(
                menus, true, true, -1);

        UiBottomMenuPreview original = controller.preview();

        controller.update(
                menus, true, false, -1);

        assertFalse(original.window().isOpen());

        controller.update(
                menus, true, true, -1);

        assertTrue(original.window().isOpen());

        // No duplicate toolbar was created.
        assertSame(original, controller.preview());

        controller.update(
                menus, false, false, -1);

        assertFalse(original.window().isOpen());
    }

    @Test
    void adoptsReloadedMenuRegistry() {

        UiSystem ui = new UiSystem();

        BottomToolbarController controller = controller(ui);

        MenuManager first = menus();
        MenuManager second = menus();

        controller.update(
                first, true, true, -1);

        UiBottomMenuPreview original = controller.preview();

        controller.update(
                second, true, true, -1);

        assertSame(original, controller.preview());
        assertSame(second, original.menus());
    }
}
