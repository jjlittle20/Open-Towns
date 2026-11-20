
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuXmlLoader;

class UiMenuNavigationTest {

    private static MenuDefinition loadMainMenu()
            throws Exception {

        URL resource = UiMenuNavigationTest.class
                .getClassLoader()
                .getResource("data/menus/main/root.xml");

        assertNotNull(resource);

        return MenuXmlLoader.load(
                Path.of(resource.toURI()).toFile());
    }

    @Test
    void resolvesMigratedCreditsMenu()
            throws Exception {

        MenuDefinition root = loadMainMenu();

        UiMenuNavigation navigation = new UiMenuNavigation();

        AtomicInteger opens = new AtomicInteger();

        navigation.register(
                "main.credits.root",
                opens::incrementAndGet);

        Optional<Runnable> action = navigation.resolve(
                root, "main.credits");

        assertTrue(action.isPresent());

        // Resolution alone must not open the menu.
        assertEquals(0, opens.get());

        action.get().run();

        assertEquals(1, opens.get());
    }

    @Test
    void unmigratedSubmenusStayOnLegacySystem()
            throws Exception {

        MenuDefinition root = loadMainMenu();

        UiMenuNavigation navigation = new UiMenuNavigation();

        navigation.register(
                "main.credits.root",
                () -> {
                });

        // Options has not been migrated yet.
        assertTrue(
                navigation.resolve(
                        root, "main.options")
                        .isEmpty());

        // New Game has not been migrated either.
        assertTrue(
                navigation.resolve(
                        root, "main.new_game")
                        .isEmpty());
    }

    @Test
    void ordinaryCommandButtonsAreNotRedirected()
            throws Exception {

        MenuDefinition root = loadMainMenu();

        UiMenuNavigation navigation = new UiMenuNavigation();

        // Exit Game is a command button,
        // not submenu navigation.
        assertTrue(
                navigation.resolve(
                        root, "main.exit")
                        .isEmpty());
    }

    @Test
    void futureMenusCanBeRegistered()
            throws Exception {

        MenuDefinition root = loadMainMenu();

        UiMenuNavigation navigation = new UiMenuNavigation();

        AtomicInteger opens = new AtomicInteger();

        navigation.register(
                "main.options.root",
                opens::incrementAndGet);

        Optional<Runnable> action = navigation.resolve(
                root, "main.options");

        assertTrue(action.isPresent());

        action.get().run();

        assertEquals(1, opens.get());
    }

    @Test
    void duplicateRegistrationsAreRejected() {

        UiMenuNavigation navigation = new UiMenuNavigation();

        navigation.register(
                "main.credits.root",
                () -> {
                });

        assertThrows(
                IllegalArgumentException.class,
                () -> navigation.register(
                        "main.credits.root",
                        () -> {
                        }));
    }
}
