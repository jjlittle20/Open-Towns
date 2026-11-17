
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiRoot;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;

class UiWindowTest {

    private UiElement element(int width, int height) {
        UiElement element = new UiElement();
        element.setPreferredSize(new UiSize(width, height));
        return element;
    }

    @Test
    void windowHasTitleAndContentArea() {
        UiWindow window = new UiWindow("Inventory");

        assertEquals("Inventory", window.title());
        assertNotNull(window.titleBar());
        assertNotNull(window.content());

        assertEquals(2, window.children().size());

        assertSame(window, window.titleBar().parent());
        assertSame(window, window.content().parent());
    }

    @Test
    void windowMeasuresItsContent() {
        UiWindow window = new UiWindow("Inventory");

        window.content().add(element(220, 70));

        UiSize measured = window.measure(
                new UiSize(800, 600));

        // Width: 220 + 8 + 8 = 236
        // Height: 24 + 4 + 70 + 8 + 8 = 114
        assertEquals(new UiSize(236, 114), measured);
    }

    @Test
    void windowPositionsTitleAndContent() {
        UiWindow window = new UiWindow("Inventory");

        UiElement item = element(100, 30);
        window.content().add(item);

        window.layout(new UiRect(50, 60, 300, 180));

        assertEquals(
                new UiRect(58, 68, 284, 24),
                window.titleBar().bounds());

        assertEquals(
                new UiRect(58, 96, 284, 136),
                window.content().bounds());

        assertEquals(
                new UiRect(58, 96, 100, 30),
                item.bounds());
    }

    @Test
    void windowCanBePositionedOnDesktop() {
        UiContainer desktop = new UiContainer();
        desktop.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);

        UiWindow window = new UiWindow("Character");

        window.setPosition(new UiPoint(100, 150));
        window.setPreferredSize(new UiSize(300, 200));

        desktop.add(window);

        UiRoot root = new UiRoot(desktop);
        root.updateLayout(new UiSize(800, 600));

        assertEquals(
                new UiRect(100, 150, 300, 200),
                window.bounds());
    }

    @Test
    void addingContentInvalidatesWindowAndRoot() {
        UiContainer desktop = new UiContainer();
        desktop.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);

        UiWindow window = new UiWindow("Inventory");
        desktop.add(window);

        UiRoot root = new UiRoot(desktop);
        root.updateLayout(new UiSize(800, 600));

        assertFalse(window.isLayoutDirty());
        assertFalse(desktop.isLayoutDirty());

        window.content().add(element(120, 30));

        assertTrue(window.content().isLayoutDirty());
        assertTrue(window.isLayoutDirty());
        assertTrue(desktop.isLayoutDirty());

        assertTrue(root.updateLayout(new UiSize(800, 600)));

        assertFalse(window.isLayoutDirty());
        assertFalse(desktop.isLayoutDirty());
    }

    @Test
    void changingTitleInvalidatesLayout() {
        UiWindow window = new UiWindow("Inventory");

        window.layout(new UiRect(0, 0, 300, 200));

        assertFalse(window.isLayoutDirty());

        window.setTitle("Stockpile");

        assertEquals("Stockpile", window.title());
        assertTrue(window.isLayoutDirty());

        window.layout(new UiRect(0, 0, 300, 200));

        assertFalse(window.isLayoutDirty());

        // Setting the same title should not invalidate.
        window.setTitle("Stockpile");

        assertFalse(window.isLayoutDirty());
    }
}
