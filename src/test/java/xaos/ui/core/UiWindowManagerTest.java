
package xaos.ui.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import xaos.ui.components.UiWindow;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiWindowAnchor;
import xaos.ui.layout.UiWindowDockMode;

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

        @Test
        void centeredWindowTracksViewportSize() {

                UiWindowManager manager = new UiWindowManager();

                UiWindow window = new UiWindow("Credits");

                window.setUserSize(new UiSize(420, 260));
                window.setCentered(true);

                manager.register(window);

                manager.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiPoint(190, 170),
                                window.position());

                // Simulate changing the game resolution.
                manager.updateLayout(new UiSize(1000, 700));

                assertEquals(
                                new UiPoint(290, 220),
                                window.position());

                assertFalse(
                                manager.updateLayout(new UiSize(1000, 700)));
        }

        @Test
        void modalStaysAboveOrdinaryWindows() {

                UiWindowManager manager = new UiWindowManager();

                UiWindow normal = new UiWindow("Inventory");

                UiWindow modal = new UiWindow("Credits");
                modal.setModal(true);

                manager.register(normal);
                manager.register(modal);

                manager.open(modal);

                assertSame(modal, manager.activeModal());

                // Cannot raise a normal window above the modal.
                assertFalse(manager.bringToFront(normal));

                // Opening a normal window cannot cover it either.
                manager.open(normal);

                assertSame(
                                modal,
                                manager.windows()
                                                .get(manager.windows().size() - 1));

                manager.close(modal);

                assertNull(manager.activeModal());

                // Normal windows regain their usual behaviour.
                assertTrue(manager.bringToFront(normal)
                                || manager.windows().get(
                                                manager.windows().size() - 1) == normal);
        }

        @Test
        void rightAnchoredWindowTracksViewportChanges() {

                UiWindowManager manager = new UiWindowManager();

                UiWindow window = new UiWindow("Actions");

                window.setUserSize(new UiSize(250, 180));
                window.setAnchor(UiWindowAnchor.RIGHT);
                window.setAnchorInsets(UiInsets.all(12));

                manager.register(window);

                manager.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiPoint(538, 210),
                                window.position());

                // Changing resolution should move the window,
                // without changing its dimensions.
                manager.updateLayout(new UiSize(1000, 700));

                assertEquals(
                                new UiPoint(738, 260),
                                window.position());

                assertEquals(
                                new UiSize(250, 180),
                                new UiSize(
                                                window.bounds().width(),
                                                window.bounds().height()));

                // No unnecessary layout updates.
                assertFalse(
                                manager.updateLayout(new UiSize(1000, 700)));
        }

        @Test
        void bottomAnchorTracksViewportHeight() {

                UiWindowManager manager = new UiWindowManager();

                UiWindow window = new UiWindow("Toolbar");

                window.setUserSize(new UiSize(400, 100));
                window.setAnchor(UiWindowAnchor.BOTTOM);
                window.setAnchorInsets(UiInsets.all(10));

                manager.register(window);

                manager.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiPoint(200, 490),
                                window.position());

                manager.updateLayout(new UiSize(800, 800));

                assertEquals(
                                new UiPoint(200, 690),
                                window.position());
        }

        @Test
        void cornerAnchorHonoursMargins() {

                UiWindowManager manager = new UiWindowManager();

                UiWindow window = new UiWindow("Messages");

                window.setUserSize(new UiSize(250, 180));

                window.setAnchor(
                                UiWindowAnchor.TOP_LEFT);

                window.setAnchorInsets(
                                new UiInsets(8, 12, 16, 20));

                manager.register(window);

                manager.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiPoint(20, 8),
                                window.position());

                window.setAnchor(
                                UiWindowAnchor.BOTTOM_RIGHT);

                manager.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiPoint(538, 404),
                                window.position());
        }

        @Test
        void disablingAnchorRestoresFloatingPositioning() {

                UiWindowManager manager = new UiWindowManager();

                UiWindow window = new UiWindow("Inventory");

                window.setUserSize(new UiSize(250, 180));
                window.setPosition(new UiPoint(100, 80));

                manager.register(window);

                manager.updateLayout(new UiSize(800, 600));

                // Unanchored windows retain manual positions.
                assertEquals(
                                new UiPoint(100, 80),
                                window.position());

                window.setAnchor(UiWindowAnchor.RIGHT);

                manager.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiPoint(550, 210),
                                window.position());

                // Disable anchoring and choose another
                // ordinary floating position.
                window.setAnchor(UiWindowAnchor.NONE);
                window.setPosition(new UiPoint(120, 90));

                manager.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiPoint(120, 90),
                                window.position());
        }

        @Test
        void rightDockedPanelFillsAvailableHeight() {

                UiWindowManager manager = new UiWindowManager();

                UiWindow panel = new UiWindow("Actions");

                panel.setUserSize(new UiSize(250, 180));
                panel.setAnchor(UiWindowAnchor.RIGHT);

                panel.setAnchorInsets(
                                new UiInsets(8, 12, 20, 6));

                panel.setDockMode(
                                UiWindowDockMode.FILL_HEIGHT);

                manager.register(panel);

                manager.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiRect(538, 8, 250, 572),
                                panel.bounds());

                // Increase the viewport dimensions.
                manager.updateLayout(new UiSize(1000, 700));

                assertEquals(
                                new UiRect(738, 8, 250, 672),
                                panel.bounds());

                // Original user dimensions must survive.
                assertEquals(
                                new UiSize(250, 180),
                                panel.userSize().orElseThrow());

                assertFalse(
                                manager.updateLayout(new UiSize(1000, 700)));
        }

        @Test
        void bottomDockedPanelFillsAvailableWidth() {

                UiWindowManager manager = new UiWindowManager();

                UiWindow panel = new UiWindow("Toolbar");

                panel.setUserSize(new UiSize(400, 96));
                panel.setAnchor(UiWindowAnchor.BOTTOM);

                panel.setAnchorInsets(
                                new UiInsets(7, 16, 10, 24));

                panel.setDockMode(
                                UiWindowDockMode.FILL_WIDTH);

                manager.register(panel);

                manager.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiRect(24, 494, 760, 96),
                                panel.bounds());

                manager.updateLayout(new UiSize(1000, 700));

                assertEquals(
                                new UiRect(24, 594, 960, 96),
                                panel.bounds());
        }

        @Test
        void disablingDockingRestoresOriginalSize() {

                UiWindowManager manager = new UiWindowManager();

                UiWindow panel = new UiWindow("Inventory");

                panel.setUserSize(new UiSize(250, 180));
                panel.setAnchor(UiWindowAnchor.RIGHT);

                panel.setDockMode(
                                UiWindowDockMode.FILL_HEIGHT);

                manager.register(panel);

                manager.updateLayout(new UiSize(800, 600));

                assertEquals(600, panel.bounds().height());

                // Disable stretching but retain anchoring.
                panel.setDockMode(UiWindowDockMode.NONE);

                manager.updateLayout(new UiSize(800, 600));

                assertEquals(
                                new UiSize(250, 180),
                                new UiSize(
                                                panel.bounds().width(),
                                                panel.bounds().height()));

                assertEquals(
                                new UiPoint(550, 210),
                                panel.position());
        }

}
