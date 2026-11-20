
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuXmlLoader;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiSystem;

class UiMenuWindowControllerTest {

        private static MenuDefinition loadMainMenu()
                        throws Exception {

                URL resource = UiMenuWindowControllerTest.class
                                .getClassLoader()
                                .getResource(
                                                "data/menus/main/root.xml");

                assertNotNull(resource);

                return MenuXmlLoader.load(
                                Path.of(resource.toURI()).toFile());
        }

        @Test
        void migratedMenuIsCreatedOnlyWhenOpened()
                        throws Exception {

                UiSystem ui = new UiSystem();

                UiMenuWindowController controller = new UiMenuWindowController(ui);

                AtomicInteger creations = new AtomicInteger();

                controller.register(
                                "main.credits.root",
                                () -> {
                                        creations.incrementAndGet();

                                        UiWindow window = new UiWindow("Credits");

                                        window.setModal(true);

                                        return window;
                                });

                assertEquals(0, creations.get());
                assertFalse(controller.isAnyOpen());
                assertTrue(ui.windows().windows().isEmpty());

                // Resolve navigation from real Towns XML.
                Optional<Runnable> action = controller.resolve(
                                loadMainMenu(),
                                "main.credits");

                assertTrue(action.isPresent());
                assertEquals(0, creations.get());

                action.get().run();

                assertEquals(1, creations.get());
                assertTrue(
                                controller.isOpen("main.credits.root"));

                assertEquals(
                                1, ui.windows().windows().size());

                assertNotNull(ui.windows().activeModal());
        }

        @Test
        void reopeningMenuReusesExistingWindow() {

                UiSystem ui = new UiSystem();

                UiMenuWindowController controller = new UiMenuWindowController(ui);

                AtomicInteger creations = new AtomicInteger();

                controller.register(
                                "main.credits.root",
                                () -> {
                                        creations.incrementAndGet();

                                        UiWindow window = new UiWindow("Credits");

                                        window.setModal(true);

                                        return window;
                                });

                controller.open("main.credits.root");

                UiWindow original = ui.windows().activeModal();

                controller.close("main.credits.root");

                assertFalse(
                                controller.isOpen("main.credits.root"));

                assertNull(ui.windows().activeModal());

                controller.open("main.credits.root");

                assertSame(
                                original,
                                ui.windows().activeModal());

                assertEquals(1, creations.get());

                // Registration happens once.
                assertEquals(
                                1, ui.windows().windows().size());
        }

        @Test
        void closingAllMenusRestoresNormalState() {

                UiSystem ui = new UiSystem();

                UiMenuWindowController controller = new UiMenuWindowController(ui);

                controller.register(
                                "main.options.root",
                                () -> new UiWindow("Options"));

                controller.register(
                                "main.credits.root",
                                () -> {
                                        UiWindow window = new UiWindow("Credits");

                                        window.setModal(true);

                                        return window;
                                });

                controller.open("main.options.root");
                controller.open("main.credits.root");

                assertTrue(controller.isAnyOpen());
                assertNotNull(ui.windows().activeModal());

                controller.closeAll();

                assertFalse(controller.isAnyOpen());
                assertNull(ui.windows().activeModal());

                // Windows remain registered for reuse.
                assertEquals(
                                2, ui.windows().windows().size());

                assertTrue(
                                ui.windows().windows().stream()
                                                .noneMatch(UiWindow::isOpen));
        }

        @Test
        void unmigratedMenusRemainLegacy()
                        throws Exception {

                UiSystem ui = new UiSystem();

                UiMenuWindowController controller = new UiMenuWindowController(ui);

                controller.register(
                                "main.credits.root",
                                () -> new UiWindow("Credits"));

                MenuDefinition root = loadMainMenu();

                // Options is not registered as migrated.
                assertTrue(
                                controller.resolve(
                                                root,
                                                "main.options")
                                                .isEmpty());

                // The Credits route is registered.
                assertTrue(
                                controller.resolve(
                                                root,
                                                "main.credits")
                                                .isPresent());
        }

        @Test
        void duplicateMenuRegistrationIsRejected() {

                UiMenuWindowController controller = new UiMenuWindowController(
                                new UiSystem());

                controller.register(
                                "main.credits.root",
                                () -> new UiWindow("Credits"));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> controller.register(
                                                "main.credits.root",
                                                () -> new UiWindow("Duplicate")));
        }

        @Test
        void registeredMenusCanBeDiscoveredWithoutOpeningThem() {

                UiSystem ui = new UiSystem();

                UiMenuWindowController controller = new UiMenuWindowController(ui);

                AtomicInteger creations = new AtomicInteger();

                controller.register(
                                "main.options.graphics.root",
                                () -> {
                                        creations.incrementAndGet();
                                        return new UiWindow("Graphics");
                                });

                assertTrue(controller.isRegistered(
                                "main.options.graphics.root"));

                assertFalse(controller.isRegistered(
                                "main.options.audio.root"));

                assertFalse(controller.isRegistered(null));

                // Checking registration must not build a window.
                assertEquals(0, creations.get());

                assertTrue(ui.windows().windows().isEmpty());
        }

        @Test
        void switchingBetweenOptionsAndGraphicsReusesWindows() {

                UiSystem ui = new UiSystem();

                UiMenuWindowController controller = new UiMenuWindowController(ui);

                controller.register(
                                "main.options.root",
                                () -> {
                                        UiWindow window = new UiWindow("Options");
                                        window.setModal(true);
                                        return window;
                                });

                controller.register(
                                "main.options.graphics.root",
                                () -> {
                                        UiWindow window = new UiWindow("Graphics");
                                        window.setModal(true);
                                        return window;
                                });

                // Open Options.
                controller.open("main.options.root");

                UiWindow options = ui.windows().activeModal();

                assertNotNull(options);
                assertEquals("Options", options.title());

                // Navigate to Graphics.
                controller.close("main.options.root");
                controller.open("main.options.graphics.root");

                assertFalse(controller.isOpen("main.options.root"));
                assertTrue(controller.isOpen(
                                "main.options.graphics.root"));

                assertEquals(
                                "Graphics",
                                ui.windows().activeModal().title());

                // Press Back in Graphics.
                controller.close("main.options.graphics.root");
                controller.open("main.options.root");

                assertSame(options, ui.windows().activeModal());

                // Exactly two windows, each constructed once.
                assertEquals(2, ui.windows().windows().size());
        }

        @Test
        void gameSettingsCanBeOpenedFromOptionsXml()
                        throws Exception {

                URL resource = UiMenuWindowControllerTest.class
                                .getClassLoader()
                                .getResource(
                                                "data/menus/main/options/root.xml");

                assertNotNull(resource);

                MenuDefinition options = MenuXmlLoader.load(
                                Path.of(resource.toURI()).toFile());

                UiSystem ui = new UiSystem();

                UiMenuWindowController controller = new UiMenuWindowController(ui);

                controller.register(
                                "main.options.root",
                                () -> {
                                        UiWindow window = new UiWindow("Options");
                                        window.setModal(true);
                                        return window;
                                });

                controller.register(
                                "main.options.game.root",
                                () -> {
                                        UiWindow window = new UiWindow("Game Settings");

                                        window.setModal(true);
                                        return window;
                                });

                // Open the new Options root.
                controller.open("main.options.root");

                // Select Game using its original XML item ID.
                var action = controller.resolve(
                                options,
                                "main.options.game");

                assertTrue(action.isPresent());

                controller.close("main.options.root");
                action.orElseThrow().run();

                assertTrue(controller.isOpen(
                                "main.options.game.root"));

                assertFalse(controller.isOpen(
                                "main.options.root"));

                assertEquals(
                                "Game Settings",
                                ui.windows().activeModal().title());

                // Navigate back to Options.
                controller.close("main.options.game.root");
                controller.open("main.options.root");

                assertEquals(
                                "Options",
                                ui.windows().activeModal().title());

                // Both windows remain registered for reuse.
                assertEquals(2, ui.windows().windows().size());
        }

}
