
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiButtonAppearance;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiWindowAnchor;
import xaos.ui.layout.UiWindowDockMode;
import xaos.ui.rendering.UiTextMetrics;

class UiRightMenuPreviewTest {

        @TempDir
        Path tempDir;

        private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

        @Test
        void navigatesXmlMenusWithoutExecutingActions()
                        throws Exception {

                Path menusFolder = Files.createDirectory(
                                tempDir.resolve("menus"));

                Files.writeString(
                                menusFolder.resolve("root.xml"),
                                """
                                                <menu id="game.right.root"
                                                      title="Construction">

                                                    <item id="game.right.walls"
                                                          type="submenu"
                                                          title="Walls"
                                                          targetMenu="game.right.walls" />

                                                </menu>
                                                """);

                Files.writeString(
                                menusFolder.resolve("walls.xml"),
                                """
                                                <menu id="game.right.walls"
                                                      title="Walls">

                                                    <item id="game.right.walls.log"
                                                          type="action"
                                                          command="QUEUEANDPLACEAREA"
                                                          parameter="qlogwall" />

                                                    <item id="game.right.walls.back"
                                                          type="back" />

                                                </menu>
                                                """);

                MenuManager manager = new MenuManager();
                manager.loadMenus(menusFolder.toFile());

                UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                                key -> key,
                                (command, parameter) -> new UiGameplayMenuResolver.DefinitionInfo(
                                                "Resolved Log Wall",
                                                "log_wall"));

                UiRightMenuPreview preview = new UiRightMenuPreview(
                                new UiSystem(),
                                manager,
                                resolver,
                                FONT,
                                key -> key);
                AtomicInteger clicks = new AtomicInteger();

                preview.setClickFeedback(
                                clicks::incrementAndGet);

                assertFalse(
                                preview.window().titleBar().visible());
                assertEquals(
                                "game.right.root",
                                preview.currentMenuId());

                // Content order:
                // 0 = Back button
                // 1 = Preview notice
                // 2 = Scroll container
                UiScrollContainer scroll = (UiScrollContainer) preview.window()
                                .content().children().get(0);

                UiButton walls = (UiButton) scroll.content()
                                .children().get(0);

                assertTrue(walls.enabled());
                assertEquals(
                                UiButtonAppearance.MENU_SUBMENU,
                                walls.appearance());

                assertEquals(
                                "Walls\nOpen submenu",
                                walls.tooltip());

                walls.activate();
                assertEquals(1, clicks.get());
                assertEquals(
                                "game.right.walls",
                                preview.currentMenuId());

                UiButton logWall = (UiButton) scroll.content()
                                .children().get(0);
                assertTrue(
                                logWall.tooltip().contains("Resolved Log Wall"));

                // Gameplay actions cannot yet execute.
                assertFalse(logWall.enabled());

                UiButton back = (UiButton) scroll.content()
                                .children().get(1);

                // The Walls submenu contains Log Wall and Back.
                assertEquals(
                                2,
                                scroll.content().children().size());

                // Navigate back to the root menu.
                back.activate();
                assertEquals(2, clicks.get());
                assertEquals(
                                "game.right.root",
                                preview.currentMenuId());

                // The root menu contains only Walls.
                assertEquals(
                                1,
                                scroll.content().children().size());

                // Confirm we returned to the correct button.
                UiButton rootButton = (UiButton) scroll.content()
                                .children().get(0);

                assertEquals(
                                UiButtonAppearance.MENU_SUBMENU,
                                rootButton.appearance());

                assertEquals(
                                "Walls\nOpen submenu",
                                rootButton.tooltip());

        }

        @Test
        void gameplayActionsUseInjectedBinding()
                        throws Exception {

                Path menusFolder = Files.createDirectory(
                                tempDir.resolve("action-menus"));

                Files.writeString(
                                menusFolder.resolve("root.xml"),
                                """
                                                <menu id="game.right.root"
                                                      title="Construction">

                                                    <item
                                                        id="test.log-wall"
                                                        type="action"
                                                        title="Log Wall"
                                                        command="QUEUEANDPLACEAREA"
                                                        parameter="qlogwall" />

                                                </menu>
                                                """);

                MenuManager manager = new MenuManager();
                manager.loadMenus(menusFolder.toFile());

                UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                                key -> key,
                                (command, parameter) -> null);

                UiRightMenuPreview preview = new UiRightMenuPreview(
                                new UiSystem(),
                                manager,
                                resolver,
                                FONT,
                                key -> key);

                UiScrollContainer scroll = (UiScrollContainer) preview.window()
                                .content().children().get(0);

                UiButton button = (UiButton) scroll.content()
                                .children().get(0);

                // Existing preview behaviour:
                // actions are disabled by default.
                assertFalse(button.enabled());

                AtomicReference<String> executed = new AtomicReference<>();

                // Install an isolated fake executor.
                // No actual Towns world is required.
                preview.setActionBinding(
                                new UiGameplayActionBinding() {

                                        @Override
                                        public boolean isAvailable(
                                                        MenuItemDefinition item) {

                                                return "QUEUEANDPLACEAREA"
                                                                .equals(item.getCommand());
                                        }

                                        @Override
                                        public void execute(
                                                        MenuItemDefinition item) {

                                                executed.set(
                                                                item.getParameter());
                                        }
                                });

                // The menu was rebuilt with the new binding.
                button = (UiButton) scroll.content()
                                .children().get(0);

                assertTrue(button.enabled());

                button.activate();

                assertEquals(
                                "qlogwall",
                                executed.get());

                // Executing an action does not navigate
                // away from the current menu.
                assertEquals(
                                "game.right.root",
                                preview.currentMenuId());
        }

        @Test
        void xmlPanelDocksRightBelowMinimap()
                        throws Exception {

                MenuManager manager = new MenuManager();

                manager.registerMenu(
                                new MenuDefinition(
                                                "game.right.root",
                                                null,
                                                "Construction"));

                UiSystem ui = new UiSystem();

                UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                                key -> key,
                                (command, parameter) -> null);

                UiRightMenuPreview panel = new UiRightMenuPreview(
                                ui,
                                manager,
                                resolver,
                                FONT,
                                key -> key);

                panel.dockRight(
                                new UiInsets(190, 8, 80, 8));

                assertEquals(
                                UiWindowAnchor.RIGHT,
                                panel.window().anchor());

                assertEquals(
                                UiWindowDockMode.FILL_HEIGHT,
                                panel.window().dockMode());

                assertFalse(panel.window().draggable());
                assertFalse(panel.window().resizable());

                panel.open();

                ui.updateLayout(new UiSize(1280, 720));

                var bounds = panel.window().bounds();

                // The panel sits against the right edge,
                // leaving the requested 8-unit margin.
                assertEquals(
                                1272,
                                bounds.x() + bounds.width());

                // The minimap area is preserved.
                assertEquals(190, bounds.y());

                // Fill remaining height between margins.
                assertEquals(
                                720 - 190 - 80,
                                bounds.height());

                // A viewport resize must also resize the dock.
                ui.updateLayout(new UiSize(1400, 800));

                assertEquals(
                                1392,
                                panel.window().bounds().x()
                                                + panel.window().bounds().width());

                assertEquals(
                                800 - 190 - 80,
                                panel.window().bounds().height());
        }

        @Test
        void actionAvailabilityUpdatesWithoutRebuilding()
                        throws Exception {

                Path folder = Files.createDirectory(
                                tempDir.resolve("live-availability"));

                Files.writeString(
                                folder.resolve("root.xml"),
                                """
                                                <menu id="game.right.root"
                                                      title="Construction">

                                                    <item id="test.wall"
                                                          type="action"
                                                          title="Stone Wall"
                                                          command="QUEUEANDPLACEAREA"
                                                          parameter="qstonewall" />

                                                </menu>
                                                """);

                MenuManager manager = new MenuManager();
                manager.loadMenus(folder.toFile());

                UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                                key -> key,
                                (command, parameter) -> null);

                UiRightMenuPreview preview = new UiRightMenuPreview(
                                new UiSystem(),
                                manager,
                                resolver,
                                FONT,
                                key -> key);
                preview.setTooltipResolver(
                                (item, title) -> "Requires Stone");
                AtomicBoolean available = new AtomicBoolean(false);

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
                                        }
                                });

                preview.open();

                UiScrollContainer scroll = (UiScrollContainer) preview.window()
                                .content().children().get(0);

                UiButton originalButton = (UiButton) scroll.content()
                                .children().get(0);

                assertFalse(originalButton.enabled());

                // Simulate the action becoming available.
                available.set(true);

                preview.refreshActionAvailability();

                assertTrue(originalButton.enabled());

                // It must be the SAME button.
                assertSame(
                                originalButton,
                                scroll.content().children().get(0));

                assertEquals(
                                "Requires Stone",
                                originalButton.tooltip());

                // Simulate the action becoming unavailable.
                available.set(false);

                preview.refreshActionAvailability();

                assertFalse(originalButton.enabled());

                // Again, no reconstruction.
                assertSame(
                                originalButton,
                                scroll.content().children().get(0));

                assertEquals(
                                "game.right.root",
                                preview.currentMenuId());
        }

        @Test
        void unavailableReasonsUpdateWithoutRebuilding()
                        throws Exception {

                Path folder = Files.createDirectory(
                                tempDir.resolve("availability-reasons"));

                Files.writeString(
                                folder.resolve("root.xml"),
                                """
                                                <menu id="game.right.root"
                                                      title="Construction">
                                                    <item
                                                        id="test.wall"
                                                        type="action"
                                                        title="Stone Wall"
                                                        command="QUEUEANDPLACEAREA"
                                                        parameter="qstonewall" />
                                                </menu>
                                                """);

                MenuManager manager = new MenuManager();
                manager.loadMenus(folder.toFile());

                UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                                key -> key,
                                (command, parameter) -> null);

                UiRightMenuPreview preview = new UiRightMenuPreview(
                                new UiSystem(),
                                manager,
                                resolver,
                                FONT,
                                key -> key);

                preview.setTooltipResolver(
                                (item, title) -> title
                                                + "\nItem pickups:\n- Raw Stone ×3");

                AtomicReference<UiActionAvailability> state = new AtomicReference<>(
                                UiActionAvailability.blocked(
                                                "Building definition missing"));

                preview.setActionBinding(
                                new UiGameplayActionBinding() {

                                        @Override
                                        public boolean isAvailable(
                                                        MenuItemDefinition item) {

                                                return state.get().canExecute();
                                        }

                                        @Override
                                        public UiActionAvailability availability(
                                                        MenuItemDefinition item) {

                                                return state.get();
                                        }

                                        @Override
                                        public void execute(
                                                        MenuItemDefinition item) {
                                        }
                                });

                preview.open();

                UiScrollContainer scroll = (UiScrollContainer) preview.window()
                                .content().children().get(0);

                UiButton button = (UiButton) scroll.content()
                                .children().get(0);

                // Initially unavailable with an explanation.
                assertFalse(button.enabled());

                assertTrue(button.tooltip().contains(
                                "Unavailable: Building definition missing"));

                assertTrue(button.tooltip().contains(
                                "Raw Stone ×3"));

                // Change the reason, but remain unavailable.
                state.set(
                                UiActionAvailability.blocked(
                                                "Requires campaign unlock"));

                preview.refreshActionAvailability();

                assertFalse(button.enabled());

                assertTrue(button.tooltip().contains(
                                "Unavailable: Requires campaign unlock"));

                assertFalse(button.tooltip().contains(
                                "Building definition missing"));

                // Now make the action available.
                state.set(UiActionAvailability.allowed());

                preview.refreshActionAvailability();

                assertTrue(button.enabled());

                // The warning disappears, but construction
                // information remains unchanged.
                assertFalse(button.tooltip().contains(
                                "Unavailable:"));

                assertTrue(button.tooltip().contains(
                                "Raw Stone ×3"));

                // We haven't replaced the button or
                // changed the player's current menu.
                assertSame(
                                button,
                                scroll.content().children().get(0));

                assertEquals(
                                "game.right.root",
                                preview.currentMenuId());
        }

}
