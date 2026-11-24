
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;

import xaos.ui.components.UiButton;
import xaos.ui.components.UiButtonAppearance;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiWindowAnchor;
import xaos.ui.rendering.UiTextMetrics;

class UiBottomMenuPreviewTest {

    private static final UiTextMetrics FONT = text -> new UiSize(
            text.length() * 8, 16);

    private static MenuManager menus(int count) {

        MenuManager manager = new MenuManager();

        MenuDefinition menu = new MenuDefinition(
                UiBottomMenuPreview.ROOT_MENU,
                null,
                "Actions");

        for (int i = 0; i < count; i++) {

            menu.addItem(new MenuItemDefinition(
                    "game.bottom.test." + i,
                    "action",
                    null,
                    null,
                    "DIG",
                    false,
                    true,
                    null,
                    null,
                    null,
                    null,
                    true,
                    "Test " + i,
                    null,
                    null,
                    null));
        }

        manager.registerMenu(menu);

        return manager;
    }

    private static UiBottomMenuPreview preview(
            UiSystem ui,
            int count) {

        return new UiBottomMenuPreview(
                ui,
                menus(count),
                new UiGameplayMenuResolver(
                        key -> key,
                        (command, parameter) -> null),
                FONT);
    }

    @Test
    void createsHiddenClassicBottomToolbar() {

        UiSystem ui = new UiSystem();

        UiBottomMenuPreview preview = preview(ui, 10);

        assertFalse(preview.window().isOpen());

        assertEquals(
                UiWindowAnchor.BOTTOM,
                preview.window().anchor());

        assertFalse(
                preview.window().titleBar().visible());

        assertFalse(
                preview.window().draggable());

        assertFalse(
                preview.window().resizable());

        assertEquals(
                new UiSize(1024, 64),
                preview.window().userSize().orElseThrow());

        assertEquals(
                10,
                preview.buttonRow().children().size());
    }

    @Test
    void rootButtonsUseClassicActionAppearance() {

        UiBottomMenuPreview preview = preview(new UiSystem(), 3);

        UiButton first = (UiButton) preview
                .buttonRow().children().get(0);

        assertEquals(
                UiButtonAppearance.MENU_ACTION,
                first.appearance());

        assertEquals("Test 0", first.tooltip());

        // Gameplay execution is not connected yet.
        assertFalse(first.enabled());
    }

    @Test
    void arrowsScrollByOneItem() {

        UiBottomMenuPreview preview = preview(new UiSystem(), 12);

        UiButton left = (UiButton) preview
                .window().content()
                .children().get(0);

        UiButton right = (UiButton) preview
                .window().content()
                .children().get(2);

        assertFalse(left.enabled());
        assertTrue(right.enabled());

        right.activate();

        assertEquals(
                1,
                preview.firstVisibleIndex());

        assertTrue(left.enabled());
        assertTrue(right.enabled());

        right.activate();

        assertEquals(
                2,
                preview.firstVisibleIndex());

        assertFalse(right.enabled());

        left.activate();

        assertEquals(
                1,
                preview.firstVisibleIndex());
    }

    @Test
    void toolbarIsCentredAboveBottomToggle() {

        UiSystem ui = new UiSystem();

        UiBottomMenuPreview preview = preview(ui, 10);

        preview.open();

        ui.updateLayout(
                new UiSize(1400, 900));

        assertEquals(
                188,
                preview.window().bounds().x());

        assertEquals(
                804,
                preview.window().bounds().y());

        assertEquals(
                1024,
                preview.window().bounds().width());

        assertEquals(
                64,
                preview.window().bounds().height());
    }

    @Test
    void menuReloadResetsPaging() {

        UiBottomMenuPreview preview = preview(new UiSystem(), 12);

        UiButton right = (UiButton) preview
                .window().content()
                .children().get(2);

        right.activate();

        assertEquals(
                1,
                preview.firstVisibleIndex());

        preview.setMenus(menus(5));

        assertEquals(
                0,
                preview.firstVisibleIndex());

        assertEquals(
                5,
                preview.buttonRow().children().size());

        assertFalse(right.enabled());
    }
}
