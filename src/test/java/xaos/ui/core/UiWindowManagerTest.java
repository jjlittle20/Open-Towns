
package xaos.ui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import xaos.ui.components.UiWindow;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;

class UiWindowManagerTest {

    @Test
    void registeringWindowsPreservesTheirOrder() {
        UiWindowManager manager = new UiWindowManager();

        UiWindow first = new UiWindow("First");
        UiWindow second = new UiWindow("Second");

        manager.register(first);
        manager.register(second);

        assertEquals(
                List.of(first, second),
                manager.windows());

        assertTrue(manager.isManaged(first));
        assertTrue(manager.isManaged(second));

        assertSame(
                manager.root().content(),
                first.parent());
    }

    @Test
    void closingWindowPreservesItsContents() {
        UiWindowManager manager = new UiWindowManager();

        UiWindow window = new UiWindow("Inventory");
        UiElement item = new UiElement();

        window.content().add(item);
        manager.register(window);

        manager.close(window);

        assertFalse(window.isOpen());
        assertTrue(manager.isManaged(window));
        assertTrue(manager.windows().contains(window));

        assertSame(
                window.content(),
                item.parent());

        manager.open(window);

        assertTrue(window.isOpen());
        assertSame(window.content(), item.parent());
    }

    @Test
    void reopeningWindowBringsItToFront() {
        UiWindowManager manager = new UiWindowManager();

        UiWindow first = new UiWindow("First");
        UiWindow second = new UiWindow("Second");

        manager.register(first);
        manager.register(second);

        manager.close(first);
        manager.open(first);

        assertEquals(
                List.of(second, first),
                manager.windows());

        assertTrue(first.isOpen());
    }

    @Test
    void bringToFrontRespectsWindowVisibility() {
        UiWindowManager manager = new UiWindowManager();

        UiWindow first = new UiWindow("First");
        UiWindow second = new UiWindow("Second");

        manager.register(first);
        manager.register(second);

        assertTrue(manager.bringToFront(first));

        assertEquals(
                List.of(second, first),
                manager.windows());

        // Already in front.
        assertFalse(manager.bringToFront(first));

        manager.close(first);

        // Closed windows cannot be raised directly.
        assertFalse(manager.bringToFront(first));
        assertFalse(first.isOpen());
    }

    @Test
    void reopeningPreservesWindowPosition() {
        UiWindowManager manager = new UiWindowManager();

        UiWindow window = new UiWindow("Character");

        window.setPosition(new UiPoint(150, 120));
        window.setPreferredSize(new UiSize(300, 200));

        manager.register(window);

        UiRoot root = manager.root();
        root.updateLayout(new UiSize(800, 600));

        manager.close(window);
        root.updateLayout(new UiSize(800, 600));

        manager.open(window);
        root.updateLayout(new UiSize(800, 600));

        assertEquals(
                new UiPoint(150, 120),
                window.position());

        assertEquals(
                new UiRect(150, 120, 300, 200),
                window.bounds());
    }

    @Test
    void invalidWindowOperationsAreRejected() {
        UiWindowManager manager = new UiWindowManager();
        UiWindow other = new UiWindow("Unknown");

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.open(other));

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.close(other));

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.bringToFront(other));

        manager.register(other);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.register(other));

        UiWindowManager anotherManager = new UiWindowManager();

        assertThrows(
                IllegalArgumentException.class,
                () -> anotherManager.register(other));
    }

    @Test
    void windowsStayInsideViewport() {
        UiWindowManager manager = new UiWindowManager();

        UiWindow window = new UiWindow("Inventory");
        window.setPreferredSize(new UiSize(300, 200));
        window.setPosition(new UiPoint(750, 550));

        manager.register(window);

        manager.updateLayout(new UiSize(800, 600));

        assertEquals(
                new UiPoint(500, 400),
                window.position());

        assertEquals(
                new UiRect(500, 400, 300, 200),
                window.bounds());
    }

    @Test
    void shrinkingViewportRepositionsOversizedWindow() {
        UiWindowManager manager = new UiWindowManager();

        UiWindow window = new UiWindow("Inventory");
        window.setPreferredSize(new UiSize(300, 200));
        window.setPosition(new UiPoint(500, 300));

        manager.register(window);

        manager.updateLayout(new UiSize(800, 600));

        // Shrink the viewport below the window's size.
        manager.updateLayout(new UiSize(240, 150));

        assertEquals(
                new UiPoint(0, 118),
                window.position());

        // The complete 24-unit title bar stays visible.
        assertEquals(
                150,
                window.titleBar().bounds().y()
                        + window.titleBar().bounds().height());
    }

    @Test
    void unchangedViewportDoesNotRecalculateLayout() {
        UiWindowManager manager = new UiWindowManager();

        UiWindow window = new UiWindow("Inventory");

        manager.register(window);

        assertTrue(
                manager.updateLayout(new UiSize(800, 600)));

        // No changes since the previous update.
        assertFalse(
                manager.updateLayout(new UiSize(800, 600)));
    }

}
