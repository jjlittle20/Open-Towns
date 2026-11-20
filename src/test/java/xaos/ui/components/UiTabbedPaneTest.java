
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.ui.core.UiContainer;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiTabbedPaneTest {

    private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

    @Test
    void firstTabIsSelectedAutomatically() {

        UiTabbedPane tabs = new UiTabbedPane(FONT);

        UiContainer movement = tabs.addTab("movement", "Movement");

        UiContainer menus = tabs.addTab("menus", "Menus");

        assertEquals("movement", tabs.selectedTabId());

        assertTrue(movement.visible());
        assertFalse(menus.visible());

        assertTrue(tabs.tabButton("movement").selected());
        assertFalse(tabs.tabButton("menus").selected());
    }

    @Test
    void clickingTabChangesVisiblePage() {

        UiTabbedPane tabs = new UiTabbedPane(FONT);

        tabs.addTab("movement", "Movement");
        tabs.addTab("menus", "Menus");

        // Uses the same activation mechanism as
        // our existing mouse and keyboard input router.
        tabs.tabButton("menus").activate();

        assertEquals("menus", tabs.selectedTabId());

        assertFalse(tabs.page("movement").visible());
        assertTrue(tabs.page("menus").visible());

        assertTrue(tabs.tabButton("menus").selected());
    }

    @Test
    void switchingTabsPreservesContents() {

        UiTabbedPane tabs = new UiTabbedPane(FONT);

        UiContainer movement = tabs.addTab("movement", "Movement");

        tabs.addTab("menus", "Menus");

        UiButton binding = new UiButton("Move Up", FONT);

        movement.add(binding);

        tabs.selectTab("menus");
        tabs.selectTab("movement");

        assertSame(
                binding,
                movement.children().get(0));
    }

    @Test
    void supportsAllFourPlacements() {

        UiTabbedPane tabs = new UiTabbedPane(FONT);

        tabs.addTab("first", "First");
        tabs.addTab("second", "Second");

        for (UiTabbedPane.Placement placement : UiTabbedPane.Placement.values()) {

            tabs.setPlacement(placement);

            assertEquals(placement, tabs.placement());
            assertEquals(2, tabs.children().size());

            assertEquals("first", tabs.selectedTabId());
        }
    }

    @Test
    void duplicateTabIdsAreRejected() {

        UiTabbedPane tabs = new UiTabbedPane(FONT);

        tabs.addTab("movement", "Movement");

        assertThrows(
                IllegalArgumentException.class,
                () -> tabs.addTab("movement", "Duplicate"));
    }
}
