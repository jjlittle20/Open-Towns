
package xaos.ui.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiSizePolicy;

class UiRootTest {

    private UiElement element(int width, int height) {
        UiElement element = new UiElement();
        element.setPreferredSize(new UiSize(width, height));
        return element;
    }

    @Test
    void initialLayoutUsesViewportSize() {
        UiContainer container = new UiContainer();

        UiElement child = element(100, 30);
        container.add(child);

        UiRoot root = new UiRoot(container);

        assertTrue(root.updateLayout(new UiSize(800, 600)));

        assertEquals(
                new UiRect(0, 0, 800, 600),
                container.bounds());

        assertEquals(
                new UiRect(0, 0, 100, 30),
                child.bounds());
    }

    @Test
    void unchangedLayoutIsSkipped() {
        UiContainer container = new UiContainer();
        UiRoot root = new UiRoot(container);

        assertTrue(root.updateLayout(new UiSize(800, 600)));

        // No changes since the previous layout.
        assertFalse(root.updateLayout(new UiSize(800, 600)));
    }

    @Test
    void childChangesTriggerRelayout() {
        UiContainer container = new UiContainer();

        UiElement child = element(100, 30);
        container.add(child);

        UiRoot root = new UiRoot(container);

        root.updateLayout(new UiSize(800, 600));

        child.setPreferredSize(new UiSize(150, 40));

        assertTrue(container.isLayoutDirty());

        assertTrue(root.updateLayout(new UiSize(800, 600)));

        assertEquals(
                new UiRect(0, 0, 150, 40),
                child.bounds());

        assertFalse(container.isLayoutDirty());
    }

    @Test
    void viewportResizeUpdatesFlexibleElements() {
        UiContainer container = new UiContainer();

        UiElement content = element(100, 30);

        content.setHorizontalSizePolicy(UiSizePolicy.FILL);
        content.setVerticalSizePolicy(UiSizePolicy.FILL);

        container.add(content);

        UiRoot root = new UiRoot(container);

        root.updateLayout(new UiSize(400, 300));

        assertEquals(
                new UiRect(0, 0, 400, 300),
                content.bounds());

        // Simulate resizing the game window.
        assertTrue(root.updateLayout(new UiSize(800, 600)));

        assertEquals(
                new UiRect(0, 0, 800, 600),
                content.bounds());

        assertEquals(
                new UiSize(800, 600),
                root.viewportSize());
    }

    @Test
    void addingElementsTriggersRelayout() {
        UiContainer container = new UiContainer();
        UiRoot root = new UiRoot(container);

        root.updateLayout(new UiSize(800, 600));

        UiElement button = element(120, 30);

        container.add(button);

        assertTrue(root.updateLayout(new UiSize(800, 600)));

        assertEquals(
                new UiRect(0, 0, 120, 30),
                button.bounds());
    }

    @Test
    void rootRejectsContentWithExistingParent() {
        UiContainer parent = new UiContainer();
        UiElement child = element(100, 30);

        parent.add(child);

        assertThrows(
                IllegalArgumentException.class,
                () -> new UiRoot(child));
    }
}
