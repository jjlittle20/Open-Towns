
package xaos.ui.input;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiScrollBar;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiRoot;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiInputRouterTest {

    private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

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

    private static UiScrollContainer scrollList() {

        UiScrollContainer scroll = new UiScrollContainer();

        scroll.setPreferredSize(new UiSize(150, 80));

        // Enough content to exceed the viewport.
        for (int i = 0; i < 8; i++) {

            UiElement item = new UiElement();

            item.setPreferredSize(
                    new UiSize(100, 30));

            scroll.content().add(item);
        }

        return scroll;
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

    @Test
    void clickingWindowBringsItToFront() {
        UiContainer desktop = desktop();

        UiWindow back = window();
        UiWindow front = window();

        front.setPosition(new UiPoint(110, 80));

        desktop.add(back);
        desktop.add(front);

        UiRoot root = root(desktop);
        UiInputRouter input = new UiInputRouter(root);

        assertSame(front, desktop.children().get(1));

        // Click an exposed part of the back window's title.
        assertTrue(input.mousePressed(35, 35));

        assertSame(back, desktop.children().get(1));

        input.mouseReleased(35, 35);
    }

    @Test
    void draggingTitleBarMovesWindow() {
        UiContainer desktop = desktop();

        UiWindow window = window();
        desktop.add(window);

        UiRoot root = root(desktop);
        UiInputRouter input = new UiInputRouter(root);

        int startX = window.titleBar().bounds().x() + 8;
        int startY = window.titleBar().bounds().y() + 8;

        UiPoint original = window.position();

        assertTrue(input.mousePressed(startX, startY));

        assertTrue(input.mouseMoved(
                startX + 65,
                startY + 40));

        assertEquals(
                new UiPoint(
                        original.x() + 65,
                        original.y() + 40),
                window.position());

        assertEquals(
                new UiRect(85, 60, 200, 140),
                window.bounds());

        assertTrue(input.mouseReleased(
                startX + 65,
                startY + 40));

        // Further movement must not drag the released window.
        UiPoint finalPosition = window.position();

        input.mouseMoved(700, 500);

        assertEquals(finalPosition, window.position());
    }

    @Test
    void clickingWindowContentDoesNotDragIt() {
        UiContainer desktop = desktop();

        UiWindow window = window();
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        int x = window.content().bounds().x() + 15;
        int y = window.content().bounds().y() + 30;

        UiPoint original = window.position();

        assertTrue(input.mousePressed(x, y));

        input.mouseMoved(x + 100, y + 50);

        assertEquals(original, window.position());

        input.mouseReleased(x + 100, y + 50);

        assertEquals(original, window.position());
    }

    @Test
    void clickingButtonRaisesWindowWithoutDragging() {
        UiContainer desktop = desktop();

        UiWindow back = window();
        UiWindow front = window();

        front.setPosition(new UiPoint(110, 80));

        UiButton button = button("Click");

        AtomicInteger clicks = new AtomicInteger();

        button.setOnClick(clicks::incrementAndGet);

        back.content().add(button);

        desktop.add(back);
        desktop.add(front);

        UiRoot root = root(desktop);
        UiInputRouter input = new UiInputRouter(root);

        UiPoint original = back.position();

        input.mousePressed(x(button), y(button));

        assertSame(back, desktop.children().get(1));

        input.mouseReleased(x(button), y(button));

        assertEquals(1, clicks.get());
        assertEquals(original, back.position());
    }

    @Test
    void titleBarCloseButtonClosesWithoutDragging() {
        UiContainer desktop = desktop();

        UiWindow window = window();

        UiButton close = button("X");

        close.setMinimumSize(new UiSize(20, 20));
        close.setPreferredSize(new UiSize(20, 20));
        close.setPadding(xaos.ui.layout.UiInsets.NONE);

        close.setOnClick(window::close);

        window.titleBar().add(close);
        desktop.add(window);

        UiRoot root = root(desktop);
        UiInputRouter input = new UiInputRouter(root);

        UiPoint originalPosition = window.position();

        assertTrue(input.mousePressed(x(close), y(close)));

        assertTrue(close.pressed());

        assertTrue(input.mouseReleased(x(close), y(close)));

        assertFalse(window.isOpen());
        assertFalse(close.pressed());
        assertFalse(close.hovered());

        // Clicking close must not move the window.
        assertEquals(originalPosition, window.position());

        root.updateLayout(new UiSize(800, 600));

        // A closed window must not receive new clicks.
        assertFalse(input.mousePressed(x(close), y(close)));
    }

    @Test
    void draggingWindowRespectsViewportBoundaries() {
        UiContainer desktop = desktop();

        UiWindow window = window();
        desktop.add(window);

        UiRoot root = root(desktop);
        UiInputRouter input = new UiInputRouter(root);

        int startX = window.titleBar().bounds().x() + 8;
        int startY = window.titleBar().bounds().y() + 8;

        input.mousePressed(startX, startY);

        // Attempt to drag far beyond the bottom-right corner.
        input.mouseMoved(2000, 2000);

        assertEquals(
                new UiPoint(600, 460),
                window.position());

        // Attempt to drag beyond the top-left corner.
        input.mouseMoved(-500, -500);

        assertEquals(
                UiPoint.ZERO,
                window.position());

        input.mouseReleased(-500, -500);

        assertEquals(
                UiPoint.ZERO,
                window.position());
    }

    @Test
    void resizingWindowChangesDimensions() {
        UiContainer desktop = desktop();

        UiWindow window = window();
        desktop.add(window);

        UiRoot root = root(desktop);
        UiInputRouter input = new UiInputRouter(root);

        UiPoint originalPosition = window.position();

        UiRect handle = window.resizeHandleBounds();

        int startX = handle.x() + handle.width() - 3;
        int startY = handle.y() + handle.height() - 3;

        assertTrue(input.mousePressed(startX, startY));

        assertTrue(input.mouseMoved(
                startX + 60,
                startY + 35));

        assertEquals(
                new UiSize(260, 175),
                window.userSize().orElseThrow());

        assertEquals(
                new UiRect(20, 20, 260, 175),
                window.bounds());

        assertEquals(originalPosition, window.position());

        assertTrue(input.mouseReleased(
                startX + 60,
                startY + 35));
    }

    @Test
    void resizingRespectsMinimumDimensions() {
        UiContainer desktop = desktop();

        UiWindow window = window();
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        UiRect handle = window.resizeHandleBounds();

        int startX = handle.x() + handle.width() - 3;
        int startY = handle.y() + handle.height() - 3;

        input.mousePressed(startX, startY);

        input.mouseMoved(
                startX - 500,
                startY - 500);

        assertEquals(
                new UiSize(180, 80),
                window.userSize().orElseThrow());

        input.mouseReleased(
                startX - 500,
                startY - 500);
    }

    @Test
    void resizingStopsAfterMouseRelease() {
        UiContainer desktop = desktop();

        UiWindow window = window();
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        UiRect handle = window.resizeHandleBounds();

        int startX = handle.x() + handle.width() - 3;
        int startY = handle.y() + handle.height() - 3;

        input.mousePressed(startX, startY);

        input.mouseReleased(
                startX + 40,
                startY + 20);

        UiSize finalSize = window.userSize().orElseThrow();

        // Movement after release must not resize the window.
        input.mouseMoved(700, 500);

        assertEquals(
                finalSize,
                window.userSize().orElseThrow());
    }

    @Test
    void resizingRespectsViewportBoundaries() {
        UiContainer desktop = desktop();

        UiWindow window = window();
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        UiRect handle = window.resizeHandleBounds();

        int startX = handle.x() + handle.width() - 3;
        int startY = handle.y() + handle.height() - 3;

        input.mousePressed(startX, startY);

        // Attempt to resize far beyond the viewport.
        input.mouseMoved(2000, 2000);

        // Viewport: 800x600
        // Window position: 20,20
        assertEquals(
                new UiSize(780, 580),
                window.userSize().orElseThrow());

        input.mouseReleased(2000, 2000);
    }

    @Test
    void nonResizableWindowsIgnoreResizeDrag() {
        UiContainer desktop = desktop();

        UiWindow window = window();
        window.setResizable(false);

        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        UiRect originalBounds = window.bounds();
        UiRect handle = window.resizeHandleBounds();

        int startX = handle.x() + handle.width() - 3;
        int startY = handle.y() + handle.height() - 3;

        input.mousePressed(startX, startY);

        input.mouseMoved(
                startX + 100,
                startY + 100);

        input.mouseReleased(
                startX + 100,
                startY + 100);

        assertEquals(originalBounds, window.bounds());
        assertTrue(window.userSize().isEmpty());
    }

    @Test
    void mouseWheelScrollsContainerUnderCursor() {
        UiContainer desktop = desktop();
        UiWindow window = window();

        UiScrollContainer scroll = scrollList();

        window.content().add(scroll);
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        // Use the empty space to the right of the items.
        int x = scroll.bounds().x()
                + scroll.bounds().width() - 4;

        int y = scroll.bounds().y() + 5;

        assertTrue(input.mouseWheelMoved(x, y, -120));
        assertEquals(40, scroll.scrollY());

        assertTrue(input.mouseWheelMoved(x, y, -120));
        assertEquals(80, scroll.scrollY());

        assertTrue(input.mouseWheelMoved(x, y, 120));
        assertEquals(40, scroll.scrollY());

        assertTrue(input.mouseWheelMoved(x, y, 120));
        assertEquals(0, scroll.scrollY());
    }

    @Test
    void wheelEventsOverWindowsAreConsumed() {
        UiContainer desktop = desktop();

        UiWindow window = window();
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        int x = window.bounds().x() + 10;
        int y = window.bounds().y() + 10;

        // The window has no scroll container,
        // but it must still block the wheel event.
        assertTrue(input.mouseWheelMoved(x, y, -120));

        // Outside all UI, the event is not consumed.
        assertFalse(input.mouseWheelMoved(700, 500, -120));

        // A zero-delta event does nothing.
        assertFalse(input.mouseWheelMoved(x, y, 0));
    }

    @Test
    void wheelAtScrollLimitIsStillConsumed() {
        UiContainer desktop = desktop();
        UiWindow window = window();

        UiScrollContainer scroll = scrollList();

        window.content().add(scroll);
        desktop.add(window);

        UiRoot root = root(desktop);
        UiInputRouter input = new UiInputRouter(root);

        scroll.scrollTo(scroll.maxScrollY());
        root.updateLayout(new UiSize(800, 600));

        int x = scroll.bounds().x() + 5;
        int y = scroll.bounds().y() + 5;

        int bottom = scroll.maxScrollY();

        // The list cannot move further down,
        // but the event must not reach the world.
        assertTrue(input.mouseWheelMoved(x, y, -120));

        assertEquals(bottom, scroll.scrollY());
    }

    @Test
    void frontWindowBlocksScrollingBehindIt() {
        UiContainer desktop = desktop();

        UiWindow back = window();
        UiScrollContainer scroll = scrollList();

        back.content().add(scroll);

        UiWindow front = window();

        desktop.add(back);
        desktop.add(front);

        UiRoot root = root(desktop);
        UiInputRouter input = new UiInputRouter(root);

        int x = scroll.bounds().x() + 5;
        int y = scroll.bounds().y() + 5;

        // The front window covers the scroll container.
        assertTrue(input.mouseWheelMoved(x, y, -120));
        assertEquals(0, scroll.scrollY());

        // Move the covering window out of the way.
        front.setPosition(new UiPoint(350, 100));
        root.updateLayout(new UiSize(800, 600));

        assertTrue(input.mouseWheelMoved(x, y, -120));
        assertEquals(40, scroll.scrollY());
    }

    @Test
    void nestedScrollingFallsBackToOuterContainer() {
        UiContainer desktop = desktop();
        UiWindow window = window();

        UiScrollContainer outer = new UiScrollContainer();
        outer.setPreferredSize(new UiSize(150, 80));

        UiScrollContainer inner = scrollList();
        outer.content().add(inner);

        // Give the outer container additional content
        // so it has its own scrolling range.
        UiElement filler = new UiElement();
        filler.setPreferredSize(new UiSize(100, 250));

        outer.content().add(filler);

        window.content().add(outer);
        desktop.add(window);

        UiRoot root = root(desktop);
        UiInputRouter input = new UiInputRouter(root);

        int x = inner.bounds().x() + 5;
        int y = inner.bounds().y() + 5;

        assertTrue(inner.maxScrollY() > 0);
        assertTrue(outer.maxScrollY() > 0);

        // Inner container scrolls first.
        assertTrue(input.mouseWheelMoved(x, y, -120));

        assertEquals(40, inner.scrollY());
        assertEquals(0, outer.scrollY());

        // Move the inner container to its bottom.
        inner.scrollTo(inner.maxScrollY());
        root.updateLayout(new UiSize(800, 600));

        // Further scrolling moves the outer container.
        assertTrue(input.mouseWheelMoved(x, y, -120));

        assertEquals(40, outer.scrollY());
    }

    @Test
    void draggingScrollbarChangesScrollPosition() {
        UiContainer desktop = desktop();
        UiWindow window = window();

        UiScrollContainer scroll = scrollList();

        window.content().add(scroll);
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        UiScrollBar bar = scroll.scrollBar();
        UiRect thumb = bar.thumbBounds();

        int x = thumb.x() + thumb.width() / 2;
        int y = thumb.y() + thumb.height() / 2;

        UiPoint windowPosition = window.position();

        assertTrue(input.mousePressed(x, y));

        assertTrue(input.mouseMoved(x, y + 30));

        assertTrue(scroll.scrollY() > 0);

        // Dragging the thumb must not move its window.
        assertEquals(windowPosition, window.position());

        assertTrue(input.mouseReleased(x, y + 30));

        int finalScrollY = scroll.scrollY();

        // Mouse movement after release must not scroll.
        input.mouseMoved(700, 500);

        assertEquals(finalScrollY, scroll.scrollY());
    }

    @Test
    void clickingScrollbarTrackMovesThumb() {
        UiContainer desktop = desktop();
        UiWindow window = window();

        UiScrollContainer scroll = scrollList();

        window.content().add(scroll);
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        UiScrollBar bar = scroll.scrollBar();

        int x = bar.bounds().x()
                + bar.bounds().width() / 2;

        // Click three quarters of the way down the track.
        int y = bar.bounds().y()
                + bar.bounds().height() * 3 / 4;

        assertEquals(0, scroll.scrollY());

        assertTrue(input.mousePressed(x, y));

        // Track click should jump towards this position.
        assertTrue(scroll.scrollY() > 0);

        assertTrue(input.mouseReleased(x, y));
    }

    @Test
    void scrollbarDoesNotPreventWindowResizing() {
        UiContainer desktop = desktop();
        UiWindow window = window();

        UiScrollContainer scroll = scrollList();

        window.content().add(scroll);
        desktop.add(window);

        UiInputRouter input = new UiInputRouter(root(desktop));

        UiRect handle = window.resizeHandleBounds();

        // Choose a point inside the bottom-right resize grip.
        int x = handle.x() + 2;
        int y = handle.y() + 2;

        assertTrue(input.mousePressed(x, y));

        assertTrue(input.mouseMoved(x + 30, y + 20));

        assertTrue(window.userSize().isPresent());

        assertTrue(input.mouseReleased(x + 30, y + 20));
    }

    @Test
    void mouseClickChangesKeyboardFocus() {

        UiContainer desktop = desktop();
        UiWindow window = window();

        UiButton first = button("First");
        UiButton second = button("Second");
        UiButton disabled = button("Disabled");

        disabled.setEnabled(false);

        window.content().add(first);
        window.content().add(second);
        window.content().add(disabled);

        desktop.add(window);

        UiInputRouter input = new UiInputRouter(
                root(desktop));

        UiFocusManager focus = input.focusManager();

        assertNull(focus.focusedElement());

        // Pressing the first button establishes focus.
        assertTrue(input.mousePressed(x(first), y(first)));

        assertSame(first, focus.focusedElement());

        input.mouseReleased(x(first), y(first));

        // Moving the pointer does not change focus.
        input.mouseMoved(x(second), y(second));

        assertSame(first, focus.focusedElement());

        // Clicking another button transfers focus.
        assertTrue(input.mousePressed(
                x(second), y(second)));

        assertSame(second, focus.focusedElement());

        input.mouseReleased(x(second), y(second));

        // Disabled buttons cannot take focus.
        assertTrue(input.mousePressed(
                x(disabled), y(disabled)));

        assertNull(focus.focusedElement());

        input.mouseReleased(x(disabled), y(disabled));
    }

}
