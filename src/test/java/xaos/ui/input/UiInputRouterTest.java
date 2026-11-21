
package xaos.ui.input;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiScrollBar;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiSlider;
import xaos.ui.components.UiTextField;
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

        @Test
        void keyboardNavigationRequiresUiInteraction() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                UiButton first = button("First");
                UiButton disabled = button("Disabled");
                UiButton second = button("Second");

                disabled.setEnabled(false);

                window.content().add(first);
                window.content().add(disabled);
                window.content().add(second);

                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                UiFocusManager focus = input.focusManager();

                // Before clicking the UI, keyboard navigation
                // must remain available to the original game.
                assertFalse(input.keyPressed(UiKey.TAB, false));
                assertNull(focus.focusedElement());

                // Click the first button.
                assertTrue(input.mousePressed(
                                x(first), y(first)));

                input.mouseReleased(x(first), y(first));

                assertSame(first, focus.focusedElement());

                // Tab skips the disabled button.
                assertTrue(input.keyPressed(UiKey.TAB, false));

                assertSame(second, focus.focusedElement());

                // Release Tab before pressing it again.
                input.keyReleased(UiKey.TAB);

                // Shift+Tab navigates backwards.
                assertTrue(input.keyPressed(UiKey.TAB, true));

                assertSame(first, focus.focusedElement());

                input.keyReleased(UiKey.TAB);

                // Clicking outside the UI releases keyboard
                // navigation back to the original game.
                input.mousePressed(700, 500);
                input.mouseReleased(700, 500);

                assertFalse(input.keyPressed(UiKey.TAB, false));
        }

        @Test
        void keyboardActivatesFocusedButtonOnlyOncePerPress() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                UiButton button = button("Action");

                AtomicInteger activations = new AtomicInteger();

                button.setOnClick(
                                activations::incrementAndGet);

                window.content().add(button);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                // Mouse click establishes keyboard focus.
                input.mousePressed(x(button), y(button));
                input.mouseReleased(x(button), y(button));

                assertEquals(1, activations.get());

                // Enter activates the focused button.
                assertTrue(input.keyPressed(UiKey.ENTER, false));

                assertEquals(2, activations.get());

                // A repeated key-down must not activate again.
                assertTrue(input.keyPressed(UiKey.ENTER, false));

                assertEquals(2, activations.get());

                // Release and press again.
                input.keyReleased(UiKey.ENTER);

                assertTrue(input.keyPressed(UiKey.ENTER, false));

                assertEquals(3, activations.get());

                input.keyReleased(UiKey.ENTER);

                // Space uses the same button action.
                assertTrue(input.keyPressed(UiKey.SPACE, false));

                assertEquals(4, activations.get());

                input.keyReleased(UiKey.SPACE);

                // Closing the window invalidates focus.
                window.close();

                assertNull(input.focusManager().focusedElement());

                // The hidden button must not activate.
                assertFalse(input.keyPressed(UiKey.ENTER, false));

                assertEquals(4, activations.get());
        }

        @Test
        void tabNavigationRevealsItemsInsideScrollContainer() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                UiScrollContainer scroll = new UiScrollContainer();

                UiButton[] items = new UiButton[10];

                for (int i = 0; i < items.length; i++) {

                        items[i] = button("Item " + i);

                        scroll.content().add(items[i]);
                }

                window.content().add(scroll);
                desktop.add(window);

                UiRoot root = root(desktop);
                UiInputRouter input = new UiInputRouter(root);

                // Give the first item focus.
                input.mousePressed(x(items[0]), y(items[0]));
                input.mouseReleased(x(items[0]), y(items[0]));

                assertSame(
                                items[0],
                                input.focusManager().focusedElement());

                // Navigate through the rest of the list.
                for (int i = 1; i < items.length; i++) {

                        assertTrue(input.keyPressed(
                                        UiKey.TAB, false));

                        input.keyReleased(UiKey.TAB);
                }

                assertSame(
                                items[9],
                                input.focusManager().focusedElement());

                // The container must have scrolled down
                // to reveal the final button.
                assertTrue(scroll.scrollY() > 0);

                UiRect itemBounds = items[9].bounds();
                UiRect viewport = scroll.bounds();

                assertTrue(itemBounds.y() >= viewport.y());

                assertTrue(
                                itemBounds.y() + itemBounds.height() <= viewport.y() + viewport.height());
        }

        @Test
        void clickingTextFieldFocusesItAndPositionsCaret() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                UiTextField field = new UiTextField(
                                "Stone",
                                text -> new UiSize(text.length() * 7, 16));

                window.content().add(field);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                // The text begins after the field's left padding.
                int textX = field.bounds().x()
                                + field.padding().left();

                int mouseY = field.bounds().y() + 12;

                // Click just after the second character.
                assertTrue(input.mousePressed(
                                textX + 14, mouseY));

                assertSame(
                                field,
                                input.focusManager().focusedElement());

                assertEquals(2, field.cursorPosition());

                input.mouseReleased(textX + 14, mouseY);

                // Clicking elsewhere in the text moves the caret.
                assertTrue(input.mousePressed(
                                textX + 35, mouseY));

                assertEquals(5, field.cursorPosition());

                input.mouseReleased(textX + 35, mouseY);
        }

        @Test
        void textFieldReceivesUnicodeCharacterInput() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                UiTextField field = new UiTextField(
                                "",
                                text -> new UiSize(text.length() * 7, 16));

                window.content().add(field);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                int mouseX = field.bounds().x()
                                + field.bounds().width() / 2;

                int mouseY = field.bounds().y()
                                + field.bounds().height() / 2;

                input.mousePressed(mouseX, mouseY);
                input.mouseReleased(mouseX, mouseY);

                assertTrue(input.textInputActive());

                assertTrue(input.characterTyped('A'));
                assertTrue(input.characterTyped(0x1F48E));
                assertTrue(input.characterTyped('B'));

                assertEquals("A💎B", field.text());
                assertEquals(3, field.cursorPosition());

                // Reject line breaks and control characters.
                assertTrue(input.characterTyped('\n'));

                assertEquals("A💎B", field.text());

                // Clicking outside returns input to the game.
                input.mousePressed(700, 500);
                input.mouseReleased(700, 500);

                assertFalse(input.textInputActive());
                assertFalse(input.characterTyped('X'));

                assertEquals("A💎B", field.text());
        }

        @Test
        void editingKeysModifyFocusedTextField() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                UiTextField field = new UiTextField(
                                "Stone",
                                text -> new UiSize(text.length() * 7, 16));

                window.content().add(field);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                int mouseX = field.bounds().x()
                                + field.bounds().width() / 2;

                int mouseY = field.bounds().y()
                                + field.bounds().height() / 2;

                input.mousePressed(mouseX, mouseY);
                input.mouseReleased(mouseX, mouseY);

                assertTrue(input.textInputActive());

                // Home moves to the beginning.
                assertTrue(input.keyPressed(UiKey.HOME, false));
                assertEquals(0, field.cursorPosition());
                input.keyReleased(UiKey.HOME);

                // Right moves forward one character.
                assertTrue(input.keyPressed(UiKey.RIGHT, false));
                assertEquals(1, field.cursorPosition());
                input.keyReleased(UiKey.RIGHT);

                // Insert a character at the cursor.
                assertTrue(input.characterTyped('X'));
                assertEquals("SXtone", field.text());

                // Backspace removes the inserted character.
                assertTrue(input.keyPressed(
                                UiKey.BACKSPACE, false));

                assertEquals("Stone", field.text());

                input.keyReleased(UiKey.BACKSPACE);

                // Delete removes the character after the cursor.
                assertTrue(input.keyPressed(
                                UiKey.DELETE, false));

                assertEquals("Sone", field.text());

                input.keyReleased(UiKey.DELETE);

                // End moves to the end of the text.
                assertTrue(input.keyPressed(UiKey.END, false));

                assertEquals(4, field.cursorPosition());

                input.keyReleased(UiKey.END);
        }

        @Test
        void textInputOwnershipFollowsFocusAndWindowState() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                UiTextField field = new UiTextField(
                                "",
                                text -> new UiSize(text.length() * 7, 16));

                UiButton button = button("Action");

                window.content().add(field);
                window.content().add(button);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                int fieldX = field.bounds().x() + 10;
                int fieldY = field.bounds().y() + 10;

                // Initially, gameplay owns keyboard input.
                assertFalse(input.textInputActive());

                // Focusing the text field transfers ownership.
                input.mousePressed(fieldX, fieldY);
                input.mouseReleased(fieldX, fieldY);

                assertTrue(input.textInputActive());

                // Another control takes focus.
                int buttonX = button.bounds().x()
                                + button.bounds().width() / 2;

                int buttonY = button.bounds().y()
                                + button.bounds().height() / 2;

                input.mousePressed(buttonX, buttonY);
                input.mouseReleased(buttonX, buttonY);

                assertFalse(input.textInputActive());

                // Return focus to the text field.
                input.mousePressed(fieldX, fieldY);
                input.mouseReleased(fieldX, fieldY);

                assertTrue(input.textInputActive());

                // Disabling the field invalidates text ownership.
                field.setEnabled(false);

                assertFalse(input.textInputActive());

                // Re-enable and refocus.
                field.setEnabled(true);

                input.mousePressed(fieldX, fieldY);
                input.mouseReleased(fieldX, fieldY);

                assertTrue(input.textInputActive());

                // Closing the window releases ownership.
                window.close();

                assertFalse(input.textInputActive());
        }

        @Test
        void cancellingInputClearsTextFocus() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                UiTextField field = new UiTextField(
                                "",
                                text -> new UiSize(text.length() * 7, 16));

                window.content().add(field);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                int x = field.bounds().x() + 10;
                int y = field.bounds().y() + 10;

                input.mousePressed(x, y);
                input.mouseReleased(x, y);

                assertTrue(input.textInputActive());

                input.characterTyped('A');
                assertEquals("A", field.text());

                // Simulate losing application focus.
                input.cancelInput();

                assertFalse(input.textInputActive());
                assertNull(input.focusManager().focusedElement());
                assertNull(input.focusManager().activeWindow());

                // Text is preserved, but no further typing is accepted.
                assertEquals("A", field.text());
                assertFalse(input.characterTyped('B'));

                // Clicking again restores text ownership.
                input.mousePressed(x, y);
                input.mouseReleased(x, y);

                assertTrue(input.textInputActive());
        }

        @Test
        void cancellingInputReleasesPressedButton() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                UiButton button = button("Action");

                java.util.concurrent.atomic.AtomicInteger activations = new java.util.concurrent.atomic.AtomicInteger();

                button.setOnClick(activations::incrementAndGet);

                window.content().add(button);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                int x = x(button);
                int y = y(button);

                input.mousePressed(x, y);

                assertTrue(button.pressed());

                // Simulate Alt+Tab before mouse release.
                input.cancelInput();

                assertFalse(button.pressed());
                assertFalse(button.hovered());

                // Releasing after cancellation must not click.
                input.mouseReleased(x, y);

                assertEquals(0, activations.get());
        }

        @Test
        void cancellingInputStopsWindowDragging() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                int x = window.titleBar().bounds().x() + 8;
                int y = window.titleBar().bounds().y() + 8;

                input.mousePressed(x, y);

                // Cancel while dragging.
                input.cancelInput();

                UiPoint position = window.position();

                // Later mouse movement must not move the window.
                input.mouseMoved(650, 450);

                assertEquals(position, window.position());
        }

        @Test
        void cancellingInputClearsHeldNavigationKeys() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                UiButton first = button("First");
                UiButton second = button("Second");

                window.content().add(first);
                window.content().add(second);

                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                // Activate keyboard navigation.
                input.mousePressed(x(first), y(first));
                input.mouseReleased(x(first), y(first));

                assertTrue(input.keyPressed(UiKey.TAB, false));

                assertSame(
                                second,
                                input.focusManager().focusedElement());

                // Cancel without receiving a Tab key-release event.
                input.cancelInput();

                assertNull(input.focusManager().focusedElement());

                // Keyboard input is no longer consumed.
                assertFalse(input.keyPressed(UiKey.TAB, false));

                // Re-enable navigation by clicking the UI.
                input.mousePressed(x(first), y(first));
                input.mouseReleased(x(first), y(first));

                // Tab must work again, despite the missing release.
                assertTrue(input.keyPressed(UiKey.TAB, false));

                assertSame(
                                second,
                                input.focusManager().focusedElement());
        }

        @Test
        void nonDraggableWindowStaysInPlace() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                window.setDraggable(false);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                UiPoint originalPosition = window.position();

                int x = window.titleBar().bounds().x() + 8;
                int y = window.titleBar().bounds().y() + 8;

                // Attempt to drag the title bar.
                assertTrue(input.mousePressed(x, y));

                input.mouseMoved(x + 120, y + 90);
                input.mouseReleased(x + 120, y + 90);

                assertEquals(
                                originalPosition,
                                window.position());
        }

        @Test
        void modalWindowBlocksBackgroundButtons() {

                UiContainer desktop = desktop();

                UiWindow background = window();

                UiButton backgroundButton = button("Behind");

                AtomicInteger clicks = new AtomicInteger();

                backgroundButton.setOnClick(
                                clicks::incrementAndGet);

                background.content().add(backgroundButton);

                UiWindow modal = new UiWindow("Credits");

                modal.setPosition(new UiPoint(350, 150));
                modal.setPreferredSize(new UiSize(220, 160));
                modal.setModal(true);

                desktop.add(background);
                desktop.add(modal);

                UiInputRouter input = new UiInputRouter(root(desktop));

                // Click the background button outside the modal.
                assertTrue(input.mousePressed(
                                x(backgroundButton),
                                y(backgroundButton)));

                assertTrue(input.mouseReleased(
                                x(backgroundButton),
                                y(backgroundButton)));

                assertEquals(0, clicks.get());

                // Closing the modal restores background input.
                modal.close();

                input.mousePressed(
                                x(backgroundButton),
                                y(backgroundButton));

                input.mouseReleased(
                                x(backgroundButton),
                                y(backgroundButton));

                assertEquals(1, clicks.get());
        }

        @Test
        void draggingSliderThroughInputRouterDoesNotClickButton() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                AtomicInteger value = new AtomicInteger(5);
                AtomicInteger commits = new AtomicInteger();

                UiSlider slider = new UiSlider(
                                "Music Volume",
                                FONT,
                                1, 10, 1,
                                value::get,
                                value::set,
                                commits::incrementAndGet,
                                current -> (current * 10) + "%");

                window.content().add(slider);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                UiRect track = slider.trackBounds();

                int startX = track.x() + 1;
                int endX = track.x() + track.width() - 2;
                int y = track.y() + track.height() / 2;

                // Begin dragging near the minimum.
                assertTrue(input.mousePressed(startX, y));
                assertTrue(slider.dragging());

                // Move towards maximum without releasing.
                assertTrue(input.mouseMoved(endX, y));

                assertEquals(10, value.get());

                // No settings save should occur during dragging.
                assertEquals(0, commits.get());

                // Releasing commits the drag.
                assertTrue(input.mouseReleased(endX, y));

                assertFalse(slider.dragging());
                assertEquals(10, value.get());

                // The generic UiButton click must NOT
                // execute a second action.
                assertEquals(1, commits.get());
        }

        @Test
        void focusedSliderSupportsArrowKeys() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                AtomicInteger value = new AtomicInteger(5);
                AtomicInteger commits = new AtomicInteger();

                UiSlider slider = new UiSlider(
                                "Music Volume",
                                FONT,
                                1, 10, 1,
                                value::get,
                                value::set,
                                commits::incrementAndGet,
                                current -> (current * 10) + "%");

                window.content().add(slider);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                // Mouse interaction establishes keyboard focus.
                UiRect track = slider.trackBounds();

                int x = track.x() + track.width() / 2;
                int y = track.y() + track.height() / 2;

                input.mousePressed(x, y);
                input.mouseReleased(x, y);

                assertSame(
                                slider,
                                input.focusManager().focusedElement());

                // Reset test state after focusing by mouse.
                value.set(5);
                commits.set(0);
                slider.refresh();

                // Right increases volume by one step.
                assertTrue(input.keyPressed(UiKey.RIGHT, false));

                assertEquals(6, value.get());
                assertEquals(1, commits.get());

                // Left/Right may repeat while held.
                assertTrue(input.keyPressed(UiKey.RIGHT, false));

                assertEquals(7, value.get());
                assertEquals(2, commits.get());

                input.keyReleased(UiKey.RIGHT);

                // Left decreases volume.
                assertTrue(input.keyPressed(UiKey.LEFT, false));

                assertEquals(6, value.get());
                assertEquals(3, commits.get());

                input.keyReleased(UiKey.LEFT);

                assertEquals("Music Volume: 60%", slider.text());

                // Closing the window removes focus.
                window.close();

                assertFalse(input.keyPressed(UiKey.RIGHT, false));

                assertEquals(6, value.get());
                assertEquals(3, commits.get());
        }

        @Test
        void capturesPhysicalKeyAndConsumesItsRelease() {

                UiInputRouter input = new UiInputRouter(root(desktop()));

                AtomicInteger captured = new AtomicInteger(-1);
                AtomicInteger cancellations = new AtomicInteger();

                // GLFW values used as example physical codes.
                int escape = 256;
                int keyR = 82;

                input.beginPhysicalKeyCapture(
                                escape,
                                captured::set,
                                cancellations::incrementAndGet);

                assertTrue(input.isCapturingPhysicalKey());

                // First key press completes capture.
                assertTrue(input.physicalKeyEvent(keyR, true));

                assertEquals(keyR, captured.get());
                assertEquals(0, cancellations.get());

                assertFalse(input.isCapturingPhysicalKey());

                // Repeated presses and release are swallowed.
                assertTrue(input.physicalKeyEvent(keyR, true));
                assertTrue(input.physicalKeyEvent(keyR, false));

                // A later, separate press is no longer captured.
                assertFalse(input.physicalKeyEvent(keyR, true));

                assertEquals(keyR, captured.get());
        }

        @Test
        void escapeCancelsPhysicalKeyCapture() {

                UiInputRouter input = new UiInputRouter(root(desktop()));

                AtomicInteger captured = new AtomicInteger(-1);
                AtomicInteger cancellations = new AtomicInteger();

                int escape = 256;

                input.beginPhysicalKeyCapture(
                                escape,
                                captured::set,
                                cancellations::incrementAndGet);

                assertTrue(input.physicalKeyEvent(escape, true));

                assertFalse(input.isCapturingPhysicalKey());
                assertEquals(-1, captured.get());
                assertEquals(1, cancellations.get());

                // Consume the cancelling Escape key's release.
                assertTrue(input.physicalKeyEvent(escape, false));
        }

        @Test
        void keyboardActivationDoesNotCaptureEnter() {

                UiContainer desktop = desktop();
                UiWindow window = window();

                // Use the same modal keyboard activation
                // mechanism as the real Options menus.
                window.setModal(true);

                UiButton button = button("Set Key");

                window.content().add(button);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                AtomicInteger captured = new AtomicInteger(-1);

                int escape = 256;
                int enter = 257;
                int keyR = 82;

                // Enable keyboard navigation without clicking.
                input.activateModalKeyboardNavigation(window);

                // Explicitly focus the capture button.
                assertTrue(
                                input.focusManager().requestFocus(button));

                assertSame(
                                button,
                                input.focusManager().focusedElement());

                assertFalse(input.isCapturingPhysicalKey());

                // Register capture AFTER focus is established.
                button.setOnClick(() -> input.beginPhysicalKeyCapture(
                                escape,
                                captured::set,
                                () -> {
                                }));

                // Enter activates the focused button.
                assertTrue(input.keyPressed(UiKey.ENTER, false));

                assertTrue(input.isCapturingPhysicalKey());

                // The initiating Enter press must be ignored.
                assertTrue(input.physicalKeyEvent(enter, true));

                assertEquals(-1, captured.get());

                // Release Enter.
                input.keyReleased(UiKey.ENTER);

                assertTrue(input.physicalKeyEvent(enter, false));

                assertTrue(input.isCapturingPhysicalKey());

                // Now capture R.
                assertTrue(input.physicalKeyEvent(keyR, true));

                assertEquals(keyR, captured.get());
                assertFalse(input.isCapturingPhysicalKey());

                // The captured key's release is consumed.
                assertTrue(input.physicalKeyEvent(keyR, false));
        }

        @Test
        void cancellingUiInputCancelsKeyCapture() {

                UiInputRouter input = new UiInputRouter(root(desktop()));

                AtomicInteger captured = new AtomicInteger(-1);
                AtomicInteger cancellations = new AtomicInteger();

                input.beginPhysicalKeyCapture(
                                256,
                                captured::set,
                                cancellations::incrementAndGet);

                input.cancelInput();

                assertFalse(input.isCapturingPhysicalKey());
                assertEquals(1, cancellations.get());

                // No key can now be assigned by that capture.
                assertFalse(input.physicalKeyEvent(82, true));
                assertEquals(-1, captured.get());
        }

        @Test
        void unrelatedKeyReleaseDoesNotFinishActivationWait() {

                UiContainer desktop = desktop();
                UiWindow window = window();
                window.setModal(true);

                UiButton button = button("Set Key");
                window.content().add(button);
                desktop.add(window);

                UiInputRouter input = new UiInputRouter(root(desktop));

                input.activateModalKeyboardNavigation(window);

                assertTrue(
                                input.focusManager().requestFocus(button));

                AtomicInteger captured = new AtomicInteger(-1);

                button.setOnClick(() -> input.beginPhysicalKeyCapture(
                                256,
                                captured::set,
                                () -> {
                                }));

                // Begin capture using Enter.
                assertTrue(input.keyPressed(UiKey.ENTER, false));

                assertTrue(input.isCapturingPhysicalKey());

                // An unrelated key release must not
                // make capture ready.
                input.keyReleased(UiKey.LEFT);

                assertTrue(input.physicalKeyEvent(263, false));

                // This press occurs while Enter is still held.
                // It must not be accepted.
                assertTrue(input.physicalKeyEvent(75, true));

                assertEquals(-1, captured.get());

                // Now release the actual activating key.
                input.keyReleased(UiKey.ENTER);

                assertTrue(input.physicalKeyEvent(257, false));

                // The next physical press can be captured.
                assertTrue(input.physicalKeyEvent(82, true));

                assertEquals(82, captured.get());

                assertFalse(input.isCapturingPhysicalKey());
        }

        @Test
        void lockedWindowCannotBeDraggedOrResized() {

                UiContainer desktop = desktop();

                UiWindow window = window();
                window.setPreferredSize(new UiSize(300, 200));
                window.setCloseAction(FONT, window::close);

                UiButton contentButton = button("Action");
                AtomicInteger actions = new AtomicInteger();

                contentButton.setOnClick(actions::incrementAndGet);
                window.content().add(contentButton);

                desktop.add(window);

                UiRoot root = root(desktop);
                UiInputRouter input = new UiInputRouter(root);

                UiButton lock = window.lockButton().orElseThrow();

                // Lock using the actual mouse input router.
                assertTrue(input.mousePressed(x(lock), y(lock)));
                assertTrue(input.mouseReleased(x(lock), y(lock)));

                assertTrue(window.locked());

                root.updateLayout(new UiSize(800, 600));

                UiPoint originalPosition = window.position();

                int titleX = window.titleBar().bounds().x() + 8;
                int titleY = window.titleBar().bounds().y() + 8;

                // Attempt to drag the locked window.
                input.mousePressed(titleX, titleY);
                input.mouseMoved(titleX + 80, titleY + 50);
                input.mouseReleased(titleX + 80, titleY + 50);

                assertEquals(originalPosition, window.position());

                // Attempt to resize the locked window.
                UiRect handle = window.resizeHandleBounds();

                int resizeX = handle.x() + handle.width() - 3;
                int resizeY = handle.y() + handle.height() - 3;

                input.mousePressed(resizeX, resizeY);
                input.mouseMoved(resizeX + 60, resizeY + 40);
                input.mouseReleased(resizeX + 60, resizeY + 40);

                assertTrue(window.userSize().isEmpty());

                // Contents remain interactive while locked.
                input.mousePressed(
                                x(contentButton), y(contentButton));

                input.mouseReleased(
                                x(contentButton), y(contentButton));

                assertEquals(1, actions.get());

                // Unlock using the same button.
                input.mousePressed(x(lock), y(lock));
                input.mouseReleased(x(lock), y(lock));

                assertFalse(window.locked());

                root.updateLayout(new UiSize(800, 600));

                // Dragging should now work again.
                titleX = window.titleBar().bounds().x() + 8;
                titleY = window.titleBar().bounds().y() + 8;

                input.mousePressed(titleX, titleY);
                input.mouseMoved(titleX + 60, titleY + 30);
                input.mouseReleased(titleX + 60, titleY + 30);

                assertEquals(
                                new UiPoint(
                                                originalPosition.x() + 60,
                                                originalPosition.y() + 30),
                                window.position());
        }

}
