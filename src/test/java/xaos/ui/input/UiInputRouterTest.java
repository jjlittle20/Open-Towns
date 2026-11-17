
package xaos.ui.input;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiRoot;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiInputRouterTest {

    private static final UiTextMetrics FONT =
            text -> new UiSize(text.length() * 7, 16);

    private static UiButton button(String text) {
        return new UiButton(text, FONT);
    }

    private static UiWindow window() {
        UiWindow window = new UiWindow("Test Window");
        window.setPosition(new UiPoint(20, 20));
        window.setPreferredSize(new UiSize(200, 140));
        return window;
    }

    private static UiContainer desktop() {
        UiContainer desktop = new UiContainer();
        desktop.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);
        return desktop;
    }

    private static UiRoot root(UiContainer desktop) {
        UiRoot root = new UiRoot(desktop);
        root.updateLayout(new UiSize(800, 600));
        return root;
    }

    private static int x(UiButton button) {
        return button.bounds().x() + 2;
    }

    private static int y(UiButton button) {
        return button.bounds().y() + 2;
    }

    @Test
    void clickingButtonActivatesIt() {
        UiContainer desktop = desktop();
        UiWindow window = window();

        UiButton button = button("Click");
        AtomicInteger clicks = new AtomicInteger();

        button.setOnClick(clicks::incrementAndGet);

        window.content().add(button);
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        assertTrue(input.mousePressed(x(button), y(button)));
        assertTrue(button.pressed());

        assertTrue(input.mouseReleased(x(button), y(button)));

        assertEquals(1, clicks.get());
        assertFalse(button.pressed());

        // Releasing again must not trigger another click.
        input.mouseReleased(x(button), y(button));
        assertEquals(1, clicks.get());
    }

    @Test
    void mouseMovementUpdatesHoverState() {
        UiContainer desktop = desktop();
        UiWindow window = window();
        UiButton button = button("Hover");

        window.content().add(button);
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        assertTrue(input.mouseMoved(x(button), y(button)));
        assertTrue(button.hovered());

        assertFalse(input.mouseMoved(500, 500));
        assertFalse(button.hovered());
    }

    @Test
    void releasingOutsideCancelsClick() {
        UiContainer desktop = desktop();
        UiWindow window = window();

        UiButton button = button("Click");
        AtomicInteger clicks = new AtomicInteger();

        button.setOnClick(clicks::incrementAndGet);

        window.content().add(button);
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        input.mousePressed(x(button), y(button));

        // Drag away from the button.
        assertTrue(input.mouseMoved(500, 500));
        assertFalse(button.pressed());

        // The release is still consumed by the UI.
        assertTrue(input.mouseReleased(500, 500));

        assertEquals(0, clicks.get());
    }

    @Test
    void releasingOnAnotherButtonDoesNotActivateEither() {
        UiContainer desktop = desktop();
        UiWindow window = window();

        UiButton first = button("First");
        UiButton second = button("Second");

        AtomicInteger firstClicks = new AtomicInteger();
        AtomicInteger secondClicks = new AtomicInteger();

        first.setOnClick(firstClicks::incrementAndGet);
        second.setOnClick(secondClicks::incrementAndGet);

        window.content().add(first);
        window.content().add(second);
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        input.mousePressed(x(first), y(first));
        input.mouseMoved(x(second), y(second));
        input.mouseReleased(x(second), y(second));

        assertEquals(0, firstClicks.get());
        assertEquals(0, secondClicks.get());
    }

    @Test
    void disabledButtonsConsumeClicksWithoutActivating() {
        UiContainer desktop = desktop();
        UiWindow window = window();

        UiButton button = button("Disabled");
        AtomicInteger clicks = new AtomicInteger();

        button.setOnClick(clicks::incrementAndGet);
        button.setEnabled(false);

        window.content().add(button);
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        assertTrue(input.mousePressed(x(button), y(button)));
        assertTrue(input.mouseReleased(x(button), y(button)));

        assertEquals(0, clicks.get());
        assertFalse(button.pressed());
        assertFalse(button.hovered());
    }

    @Test
    void frontWindowBlocksButtonsBehindIt() {
        UiContainer desktop = desktop();

        UiWindow back = window();
        UiButton backButton = button("Behind");

        AtomicInteger clicks = new AtomicInteger();
        backButton.setOnClick(clicks::incrementAndGet);

        back.content().add(backButton);

        UiWindow front = window();

        desktop.add(back);
        desktop.add(front);

        UiInputRouter input = new UiInputRouter(root(desktop));

        // Front window is empty but covers the back button.
        assertTrue(input.mousePressed(
                x(backButton), y(backButton)));

        assertTrue(input.mouseReleased(
                x(backButton), y(backButton)));

        assertEquals(0, clicks.get());
    }

    @Test
    void bringingWindowToFrontChangesInputPriority() {
        UiContainer desktop = desktop();

        UiWindow back = window();
        UiWindow front = window();

        UiButton backButton = button("Back");
        UiButton frontButton = button("Front");

        AtomicInteger backClicks = new AtomicInteger();
        AtomicInteger frontClicks = new AtomicInteger();

        backButton.setOnClick(backClicks::incrementAndGet);
        frontButton.setOnClick(frontClicks::incrementAndGet);

        back.content().add(backButton);
        front.content().add(frontButton);

        desktop.add(back);
        desktop.add(front);

        UiRoot root = root(desktop);
        UiInputRouter input = new UiInputRouter(root);

        input.mousePressed(x(frontButton), y(frontButton));
        input.mouseReleased(x(frontButton), y(frontButton));

        assertEquals(1, frontClicks.get());
        assertEquals(0, backClicks.get());

        // Move the other window to the top.
        desktop.bringToFront(back);
        root.updateLayout(new UiSize(800, 600));

        input.mousePressed(x(backButton), y(backButton));
        input.mouseReleased(x(backButton), y(backButton));

        assertEquals(1, backClicks.get());
        assertEquals(1, frontClicks.get());
    }

    @Test
    void hiddenWindowsDoNotReceiveInput() {
        UiContainer desktop = desktop();

        UiWindow back = window();
        UiButton backButton = button("Back");

        AtomicInteger clicks = new AtomicInteger();
        backButton.setOnClick(clicks::incrementAndGet);

        back.content().add(backButton);

        UiWindow front = window();

        desktop.add(back);
        desktop.add(front);

        UiRoot root = root(desktop);
        UiInputRouter input = new UiInputRouter(root);

        front.setVisible(false);
        root.updateLayout(new UiSize(800, 600));

        input.mousePressed(x(backButton), y(backButton));
        input.mouseReleased(x(backButton), y(backButton));

        assertEquals(1, clicks.get());
    }

    @Test
    void cancellationClearsPointerState() {
        UiContainer desktop = desktop();
        UiWindow window = window();

        UiButton button = button("Click");
        AtomicInteger clicks = new AtomicInteger();

        button.setOnClick(clicks::incrementAndGet);

        window.content().add(button);
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        input.mousePressed(x(button), y(button));

        assertTrue(button.pressed());
        assertTrue(button.hovered());

        input.cancelPointer();

        assertFalse(button.pressed());
        assertFalse(button.hovered());

        // No activation after cancellation.
        input.mouseReleased(x(button), y(button));

        assertEquals(0, clicks.get());
    }
}
