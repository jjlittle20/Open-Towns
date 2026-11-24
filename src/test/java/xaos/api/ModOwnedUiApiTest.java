
package xaos.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.api.ui.UiActionRegistration;

class ModOwnedUiApiTest {

    @Test
    void modReceivesItsOwnUiApi() {

        GameApi base = GameApiBootstrap.get();

        GameApi scoped = GameApiBootstrap.forMod("testmod");

        assertNotSame(base.ui(), scoped.ui());

        // Other game services are still shared.
        assertSame(base.world(), scoped.world());
        assertSame(base.tasks(), scoped.tasks());
        assertSame(
                base.definitions(),
                scoped.definitions());
    }

    @Test
    void modCanRegisterOwnAction() {

        GameApi api = GameApiBootstrap.forMod("testmod");

        UiActionRegistration registration = api.ui().registerGameAction(
                "testmod:hello",
                "Hello",
                () -> {
                });

        try {
            assertTrue(registration.isActive());
        } finally {
            registration.unregister();
        }
    }

    @Test
    void modCannotRegisterAnotherModsAction() {

        GameApi api = GameApiBootstrap.forMod("mod-a");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> api.ui().registerGameAction(
                        "mod-b:hello",
                        "Hello",
                        () -> {
                        }));

        assertTrue(
                error.getMessage().contains("mod-a"));
    }

    @Test
    void modCannotCreateAnotherModsWindow() {

        GameApi api = GameApiBootstrap.forMod("mod-a");

        assertThrows(
                IllegalArgumentException.class,
                () -> api.ui().createWindow(
                        "mod-b:window",
                        "Wrong Owner"));
    }

    @Test
    void modCannotContributeForeignMenuItem() {

        GameApi api = GameApiBootstrap.forMod("mod-a");

        assertThrows(
                IllegalArgumentException.class,
                () -> api.ui().addMenuAction(
                        "game.right.root",
                        "mod-b:button",
                        "Invalid",
                        null,
                        "mod-a:hello"));
    }

    @Test
    void modCannotReferenceForeignCallback() {

        GameApi api = GameApiBootstrap.forMod("mod-a");

        assertThrows(
                IllegalArgumentException.class,
                () -> api.ui().addMenuAction(
                        "game.right.root",
                        "mod-a:button",
                        "Invalid",
                        null,
                        "mod-b:hello"));
    }

    @Test
    void combinedRegistrationAlsoChecksOwnership() {

        GameApi api = GameApiBootstrap.forMod("mod-a");

        assertThrows(
                IllegalArgumentException.class,
                () -> api.ui().registerMenuButton(
                        "game.right.root",
                        "mod-b:hello",
                        "Wrong Owner",
                        () -> {
                        }));
    }
}
