
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiRoot;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiInsets;
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

        @Test
        void closingWindowPreservesItsContents() {
                UiWindow window = new UiWindow("Inventory");

                UiElement item = element(100, 30);
                window.content().add(item);

                window.layout(new UiRect(0, 0, 300, 200));

                assertTrue(window.isOpen());

                window.close();

                assertFalse(window.isOpen());
                assertFalse(window.visible());
                assertTrue(window.isLayoutDirty());

                // The contents are still present.
                assertTrue(window.content().children().contains(item));
                assertSame(window.content(), item.parent());

                window.open();

                assertTrue(window.isOpen());
                assertTrue(window.content().children().contains(item));
        }

        @Test
        void openingAndClosingWindowInvalidatesRoot() {
                UiContainer desktop = new UiContainer();
                desktop.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);

                UiWindow window = new UiWindow("Inventory");
                desktop.add(window);

                UiRoot root = new UiRoot(desktop);
                root.updateLayout(new UiSize(800, 600));

                assertFalse(desktop.isLayoutDirty());

                window.close();

                assertTrue(desktop.isLayoutDirty());

                assertTrue(root.updateLayout(new UiSize(800, 600)));
                assertFalse(desktop.isLayoutDirty());

                window.open();

                assertTrue(desktop.isLayoutDirty());
                assertTrue(root.updateLayout(new UiSize(800, 600)));

                assertTrue(window.isOpen());
        }

        @Test
        void reopeningWindowPreservesItsPosition() {
                UiContainer desktop = new UiContainer();
                desktop.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);

                UiWindow window = new UiWindow("Character");

                window.setPosition(new UiPoint(150, 120));
                window.setPreferredSize(new UiSize(300, 200));

                desktop.add(window);

                UiRoot root = new UiRoot(desktop);
                root.updateLayout(new UiSize(800, 600));

                window.close();
                root.updateLayout(new UiSize(800, 600));

                window.open();
                root.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiPoint(150, 120),
                                window.position());

                assertEquals(
                                new UiRect(150, 120, 300, 200),
                                window.bounds());
        }

        @Test
        void titleBarPositionsControlsOnRight() {
                UiWindow window = new UiWindow("Inventory");

                UiButton close = new UiButton(
                                "X",
                                text -> new UiSize(8, 16));

                close.setMinimumSize(new UiSize(20, 20));
                close.setPreferredSize(new UiSize(20, 20));
                close.setPadding(UiInsets.NONE);

                window.titleBar().add(close);

                window.layout(new UiRect(50, 60, 300, 180));

                assertEquals(
                                new UiRect(58, 68, 284, 24),
                                window.titleBar().bounds());

                assertEquals(
                                new UiRect(320, 70, 20, 20),
                                close.bounds());

                // Adding a title-bar control must not
                // change the content area's position.
                assertEquals(
                                new UiRect(58, 96, 284, 136),
                                window.content().bounds());
        }

        @Test
        void userSizeOverridesAutomaticMeasurement() {
                UiWindow window = new UiWindow("Inventory");

                // The content would normally require a larger window.
                window.content().add(element(400, 250));

                UiSize automatic = window.measure(
                                new UiSize(800, 600));

                assertEquals(new UiSize(416, 294), automatic);

                // Explicit sizing overrides content measurement.
                window.setUserSize(new UiSize(260, 140));

                assertEquals(
                                new UiSize(260, 140),
                                window.measure(new UiSize(800, 600)));
        }

        @Test
        void userSizeRespectsMinimumDimensions() {
                UiWindow window = new UiWindow("Inventory");

                // The default minimum window size is 180 x 80.
                window.setUserSize(new UiSize(50, 30));

                assertEquals(
                                new UiSize(180, 80),
                                window.measure(new UiSize(800, 600)));

                assertEquals(
                                new UiSize(50, 30),
                                window.userSize().orElseThrow());
        }

        @Test
        void changingUserSizeInvalidatesAndResizesWindow() {
                UiContainer desktop = new UiContainer();
                desktop.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);

                UiWindow window = new UiWindow("Inventory");

                window.setPosition(new UiPoint(100, 90));
                window.setPreferredSize(new UiSize(320, 160));

                desktop.add(window);

                UiRoot root = new UiRoot(desktop);
                root.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiRect(100, 90, 320, 160),
                                window.bounds());

                window.setUserSize(new UiSize(400, 240));

                assertTrue(window.isLayoutDirty());
                assertTrue(desktop.isLayoutDirty());

                assertTrue(root.updateLayout(new UiSize(800, 600)));

                assertEquals(
                                new UiRect(100, 90, 400, 240),
                                window.bounds());

                // The content receives the resized inner area.
                assertEquals(
                                new UiRect(108, 126, 384, 196),
                                window.content().bounds());

                assertFalse(window.isLayoutDirty());
        }

        @Test
        void clearingUserSizeRestoresAutomaticSizing() {
                UiWindow window = new UiWindow("Inventory");

                window.setPreferredSize(new UiSize(300, 180));

                window.setUserSize(new UiSize(420, 240));

                assertTrue(window.userSize().isPresent());

                assertEquals(
                                new UiSize(420, 240),
                                window.measure(new UiSize(800, 600)));

                window.clearUserSize();

                assertTrue(window.userSize().isEmpty());

                assertEquals(
                                new UiSize(300, 180),
                                window.measure(new UiSize(800, 600)));

                window.layout(new UiRect(0, 0, 300, 180));

                // Clearing a size that is already cleared
                // should not invalidate the layout again.
                window.clearUserSize();

                assertFalse(window.isLayoutDirty());
        }

        @Test
        void overflowingButtonsDoNotExpandWindowContentBounds() {
                UiWindow window = new UiWindow("Inventory");

                window.content().setPadding(UiInsets.all(10));
                window.content().setSpacing(6);

                // More buttons than the window can display.
                for (int i = 0; i < 6; i++) {
                        window.content().add(new UiButton(
                                        "Button " + i,
                                        text -> new UiSize(text.length() * 7, 16)));
                }

                window.setUserSize(new UiSize(180, 80));

                window.layout(new UiRect(50, 60, 180, 80));

                // The window remains the requested size.
                assertEquals(
                                new UiRect(50, 60, 180, 80),
                                window.bounds());

                // Content must fit within the allocated area.
                assertEquals(
                                new UiRect(58, 96, 164, 36),
                                window.content().bounds());

                // The content clips its overflowing children.
                assertTrue(window.content().clipChildren());

                // Some buttons extend beyond the visible area,
                // but must not enlarge the clipping rectangle.
                UiElement lastButton = window.content()
                                .children()
                                .get(5);

                int contentBottom = window.content().bounds().y()
                                + window.content().bounds().height();

                assertTrue(lastButton.bounds().y() > contentBottom);
        }

}
