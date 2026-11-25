
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;

import xaos.ui.components.UiButton;
import xaos.ui.components.UiButtonAppearance;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiRect;
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

    private static MenuItemDefinition popupItem(
            String id,
            String type,
            String target,
            String command,
            String parameter) {

        return new MenuItemDefinition(
                id,
                type,
                null,
                target,
                command,
                false,
                true,
                null,
                null,
                null,
                null,
                true,
                id,
                null,
                parameter,
                null);
    }

    private static MenuManager popupMenus() {

        MenuManager manager = new MenuManager();

        MenuDefinition root = new MenuDefinition(
                "game.bottom.root",
                null,
                "Actions");

        root.addItem(popupItem(
                "game.bottom.stockpiles",
                "submenu",
                "game.bottom.stockpiles",
                null,
                null));

        root.addItem(popupItem(
                "game.bottom.zones",
                "submenu",
                "game.bottom.zones",
                null,
                null));

        manager.registerMenu(root);

        MenuDefinition stockpiles = new MenuDefinition(
                "game.bottom.stockpiles",
                null,
                "Stockpiles");

        stockpiles.addItem(popupItem(
                "game.bottom.stockpiles.rawmaterials",
                "action",
                null,
                "STOCKPILE",
                "rawmaterials"));

        stockpiles.addItem(popupItem(
                "game.bottom.stockpiles.back",
                "back",
                null,
                null,
                null));

        manager.registerMenu(stockpiles);

        MenuDefinition zones = new MenuDefinition(
                "game.bottom.zones",
                null,
                "Zones");

        zones.addItem(popupItem(
                "game.bottom.zones.zcarpentry",
                "action",
                null,
                "CREATEZONE",
                "zcarpentry"));

        zones.addItem(popupItem(
                "game.bottom.zones.back",
                "back",
                null,
                null,
                null));

        manager.registerMenu(zones);

        return manager;
    }

    private static UiBottomMenuPreview popupPreview(
            UiSystem ui,
            MenuManager menus) {

        return new UiBottomMenuPreview(
                ui,
                menus,
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
                new UiSize(960, 64),
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
                220,
                preview.window().bounds().x());

        assertEquals(
                804,
                preview.window().bounds().y());

        assertEquals(
                960,
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

    @Test
    void bottomActionExecutesThroughInjectedBinding() {

        UiBottomMenuPreview preview = preview(new UiSystem(), 3);

        AtomicReference<String> executed = new AtomicReference<>();

        AtomicInteger clicks = new AtomicInteger();

        preview.setClickFeedback(
                clicks::incrementAndGet);

        preview.setActionBinding(
                new UiGameplayActionBinding() {

                    @Override
                    public boolean isAvailable(
                            MenuItemDefinition item) {

                        return "DIG".equals(
                                item.getCommand());
                    }

                    @Override
                    public void execute(
                            MenuItemDefinition item) {

                        executed.set(item.getId());
                    }
                });

        UiButton button = (UiButton) preview
                .buttonRow().children().get(0);

        assertTrue(button.enabled());

        button.activate();

        assertEquals(
                "game.bottom.test.0",
                executed.get());

        assertEquals(1, clicks.get());

        // Executing does not change the
        // toolbar's current scroll position.
        assertEquals(
                0,
                preview.firstVisibleIndex());
    }

    @Test
    void availabilityChangesWithoutRebuildingToolbar() {

        UiBottomMenuPreview preview = preview(new UiSystem(), 1);

        AtomicBoolean available = new AtomicBoolean(false);

        AtomicInteger executions = new AtomicInteger();

        preview.setActionBinding(
                new UiGameplayActionBinding() {

                    @Override
                    public boolean isAvailable(
                            MenuItemDefinition item) {

                        return available.get();
                    }

                    @Override
                    public void execute(
                            MenuItemDefinition item) {

                        executions.incrementAndGet();
                    }
                });

        UiButton originalButton = (UiButton) preview
                .buttonRow().children().get(0);

        assertFalse(originalButton.enabled());

        available.set(true);

        preview.refreshActionAvailability();

        assertTrue(originalButton.enabled());

        // The exact same button remains.
        assertSame(
                originalButton,
                preview.buttonRow()
                        .children().get(0));

        originalButton.activate();

        assertEquals(1, executions.get());

        available.set(false);

        preview.refreshActionAvailability();

        assertFalse(originalButton.enabled());

        assertSame(
                originalButton,
                preview.buttonRow()
                        .children().get(0));

        // Disabled buttons cannot execute.
        originalButton.activate();

        assertEquals(1, executions.get());
    }

    @Test
    void stockpilesOpenAboveToolbarAndExecute() {

        UiSystem ui = new UiSystem();

        UiBottomMenuPreview preview = popupPreview(ui, popupMenus());

        AtomicReference<String> executed = new AtomicReference<>();

        preview.setActionBinding(
                new UiGameplayActionBinding() {

                    @Override
                    public boolean isAvailable(
                            MenuItemDefinition item) {
                        return true;
                    }

                    @Override
                    public void execute(
                            MenuItemDefinition item) {
                        executed.set(item.getParameter());
                    }
                });

        preview.open();

        ui.updateLayout(new UiSize(1400, 900));

        UiButton stockpiles = (UiButton) preview.buttonRow().children().get(0);

        assertTrue(stockpiles.enabled());

        stockpiles.activate();

        preview.updatePopupPlacement();
        ui.updateLayout(new UiSize(1400, 900));

        UiBottomSubmenuPopup popup = preview.popup();

        assertTrue(popup.isOpen());

        assertEquals(
                "game.bottom.stockpiles",
                popup.currentMenuId());

        assertFalse(
                popup.window().titleBar().visible());

        // The popup should be above the toolbar,
        // separated by 16 logical UI units.
        assertEquals(
                preview.window().bounds().y(),
                popup.window().bounds().y()
                        + popup.window().bounds().height()
                        + 16);

        UiButton rawMaterials = (UiButton) popup.scroll().content().children().get(0);

        assertTrue(rawMaterials.enabled());

        rawMaterials.activate();

        assertEquals(
                "rawmaterials",
                executed.get());

        assertFalse(popup.isOpen());
    }

    @Test
    void zonesPopupBackReturnsToToolbar() {

        UiSystem ui = new UiSystem();

        UiBottomMenuPreview preview = popupPreview(ui, popupMenus());

        preview.open();
        ui.updateLayout(new UiSize(1400, 900));

        UiButton zones = (UiButton) preview.buttonRow().children().get(1);

        zones.activate();

        assertTrue(preview.popup().isOpen());

        assertEquals(
                "game.bottom.zones",
                preview.popup().currentMenuId());

        UiButton back = (UiButton) preview.popup().scroll()
                .content().children().get(1);

        back.activate();

        assertFalse(preview.popup().isOpen());

        // The main toolbar remains open.
        assertTrue(preview.window().isOpen());
    }

    @Test
    void popupRefreshesAfterModAddsMenuItem() {

        UiSystem ui = new UiSystem();

        MenuManager menus = popupMenus();

        UiBottomMenuPreview preview = popupPreview(ui, menus);

        preview.open();
        ui.updateLayout(new UiSize(1400, 900));

        ((UiButton) preview.buttonRow()
                .children().get(0)).activate();

        assertEquals(
                2,
                preview.popup().scroll()
                        .content().children().size());

        // Simulate a Java mod contributing an
        // additional stockpile button.
        menus.addRuntimeItem(
                "game.bottom.stockpiles",
                popupItem(
                        "testmod:custom-stockpile",
                        "action",
                        null,
                        "MOD_ACTION",
                        "testmod:custom-stockpile"));

        preview.refresh();

        assertTrue(preview.popup().isOpen());

        assertEquals(
                3,
                preview.popup().scroll()
                        .content().children().size());
    }

    @Test
    void closingToolbarAlsoClosesPopup() {

        UiSystem ui = new UiSystem();

        UiBottomMenuPreview preview = popupPreview(ui, popupMenus());

        preview.open();
        ui.updateLayout(new UiSize(1400, 900));

        ((UiButton) preview.buttonRow()
                .children().get(0)).activate();

        assertTrue(preview.popup().isOpen());

        preview.close();

        assertFalse(preview.window().isOpen());
        assertFalse(preview.popup().isOpen());
    }

    @Test
    void scrollArrowsOccupyClassicToolbarEdges() {

        UiSystem ui = new UiSystem();

        UiBottomMenuPreview preview = preview(ui, 12);

        preview.open();

        ui.updateLayout(new UiSize(1400, 900));

        UiButton left = (UiButton) preview
                .window().content()
                .children().get(0);

        UiButton right = (UiButton) preview
                .window().content()
                .children().get(2);

        assertEquals(
                new UiRect(220, 804, 32, 64),
                left.bounds());

        assertEquals(
                new UiRect(1148, 804, 32, 64),
                right.bounds());

        // The first menu item uses the same
        // leading spacing as original Towns.
        assertEquals(
                249,
                preview.buttonRow()
                        .children().get(0)
                        .bounds().x());
    }

    @Test
    void selectedActionUpdatesWithoutRebuilding() {

        UiBottomMenuPreview preview = preview(new UiSystem(), 3);

        AtomicBoolean selecting = new AtomicBoolean(false);

        preview.setActionBinding(
                new UiGameplayActionBinding() {

                    @Override
                    public boolean isAvailable(
                            MenuItemDefinition item) {
                        return true;
                    }

                    @Override
                    public void execute(
                            MenuItemDefinition item) {
                    }
                });

        preview.setSelectedActionPredicate(
                item -> selecting.get()
                        && item.getId().equals(
                                "game.bottom.test.0"));

        UiButton original = (UiButton) preview.buttonRow()
                .children().get(0);

        assertFalse(original.selected());

        selecting.set(true);
        preview.refreshActionAvailability();

        assertTrue(original.selected());

        assertSame(
                original,
                preview.buttonRow()
                        .children().get(0));

        selecting.set(false);
        preview.refreshActionAvailability();

        assertFalse(original.selected());
    }

}
