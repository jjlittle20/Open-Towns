
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuManager;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiSize;

class RightMenuControllerTest {

    private static final UiInsets INSETS = new UiInsets(190, 8, 80, 8);

    private static MenuManager menus() {

        MenuManager menus = new MenuManager();

        menus.registerMenu(
                new MenuDefinition(
                        UiRightMenuPreview.ROOT_MENU,
                        null,
                        "Construction"));

        return menus;
    }

    private static RightMenuController controller(
            UiSystem ui) {

        return RightMenuController.forTesting(
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
    }

    @Test
    void opensAndDocksWhenEnteringGameplay() {

        UiSystem ui = new UiSystem();
        RightMenuController controller = controller(ui);

        MenuManager menus = menus();

        controller.update(menus, true, INSETS);
        ui.updateLayout(new UiSize(1280, 720));

        assertTrue(controller.isConfiguredFor(menus));
        assertTrue(controller.isOpen());

        assertEquals(
                190,
                controller.preview().window().bounds().y());

        assertEquals(
                controller.preview().window().bounds().x(),
                controller.openLeftEdge());
    }

    @Test
    void respectsManualClosing() {

        UiSystem ui = new UiSystem();
        RightMenuController controller = controller(ui);

        MenuManager menus = menus();

        controller.update(menus, true, INSETS);

        controller.toggle();
        assertFalse(controller.isOpen());

        // An ordinary update must not reopen it.
        controller.update(menus, true, INSETS);
        assertFalse(controller.isOpen());

        // The original toggle can reopen it.
        controller.toggle();
        assertTrue(controller.isOpen());
    }

    @Test
    void reopensAfterLeavingAndReenteringGame() {

        UiSystem ui = new UiSystem();
        RightMenuController controller = controller(ui);

        MenuManager menus = menus();

        controller.update(menus, true, INSETS);
        controller.update(menus, false, UiInsets.NONE);

        assertFalse(controller.isOpen());

        controller.update(menus, true, INSETS);

        assertTrue(controller.isOpen());
    }

    @Test
    void adoptsNewMenuRegistryWithoutNewWindow() {

        UiSystem ui = new UiSystem();
        RightMenuController controller = controller(ui);

        MenuManager first = menus();
        MenuManager second = menus();

        controller.update(first, true, INSETS);

        UiRightMenuPreview original = controller.preview();

        controller.update(second, true, INSETS);

        assertSame(original, controller.preview());
        assertSame(second, original.menus());
    }

    @Test
    void updatesDockingInsetsWithoutReopening() {

        UiSystem ui = new UiSystem();
        RightMenuController controller = controller(ui);

        MenuManager menus = menus();

        controller.update(menus, true, INSETS);
        controller.toggle();

        UiInsets changed = new UiInsets(220, 12, 100, 8);

        controller.update(menus, true, changed);

        assertEquals(
                changed,
                controller.preview().window()
                        .anchorInsets());

        assertFalse(controller.isOpen());
    }
}
