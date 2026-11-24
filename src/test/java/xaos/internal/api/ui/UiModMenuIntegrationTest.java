
package xaos.internal.api.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import xaos.api.ui.UiActionRegistration;
import xaos.api.ui.UiApi;
import xaos.api.ui.UiMenuButtonRegistration;
import xaos.panels.UI.UiDispatcher;
import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;
import xaos.ui.integration.UiGameplayActionBinding;
import xaos.ui.integration.UiRegisteredGameplayActionBinding;

class UiModMenuIntegrationTest {

    private static final String ROOT = "game.right.root";

    @TempDir
    Path tempDir;

    private final List<UiMenuButtonRegistration> buttons = new ArrayList<>();

    private final List<UiActionRegistration> actions = new ArrayList<>();

    /**
     * Creates a fresh menu registry.
     *
     * Simulates the built-in definitions plus
     * an optional XML contribution from Mod B.
     */
    private MenuManager menus(boolean includeXml)
            throws Exception {

        MenuManager manager = new MenuManager();

        MenuDefinition root = new MenuDefinition(
                ROOT, null, "Construction");

        root.addItem(new MenuItemDefinition(
                "game.right.builtin",
                "action",
                null,
                null,
                "BUILD",
                false,
                true,
                null,
                null,
                null,
                null,
                true));

        root.addItem(new MenuItemDefinition(
                ROOT + ".back",
                "back",
                null,
                null,
                null,
                false,
                true,
                null,
                null,
                null,
                null,
                true));

        manager.registerMenu(root);

        if (includeXml) {

            Path extension = tempDir.resolve("mod-b-menu.xml");

            Files.writeString(
                    extension,
                    """
                            <menu-extension
                                id="mod-b:extension"
                                targetMenu="game.right.root">

                                <item
                                    id="mod-b:xml-button"
                                    type="action"
                                    title="XML Button"
                                    command="MOD_ACTION"
                                    parameter="mod-b:xml-action"/>

                            </menu-extension>
                            """);

            manager.applyExtension(
                    extension.toFile());
        }

        return manager;
    }

    private static List<String> ids(
            MenuManager manager) {

        return manager.requireMenu(ROOT)
                .getItems()
                .stream()
                .map(MenuItemDefinition::getId)
                .toList();
    }

    private static MenuItemDefinition find(
            MenuManager manager,
            String id) {

        return manager.requireMenu(ROOT)
                .getItems()
                .stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElseThrow();
    }

    @AfterEach
    void cleanup() {

        for (UiMenuButtonRegistration button : buttons) {
            button.unregister();
        }

        for (UiActionRegistration action : actions) {
            action.unregister();
        }

        UiDispatcher.processPending();

        UiRuntimeMenuActions.bind(
                null, () -> {
                });

        UiDispatcher.processPending();
    }

    @Test
    void javaAndXmlModsSurviveReloadAndUnloadIndependently()
            throws Exception {

        MenuManager first = menus(true);

        AtomicInteger refreshes = new AtomicInteger();

        UiRuntimeMenuActions.bind(
                first,
                refreshes::incrementAndGet);

        // Two separate mod-owned API views.
        UiApi modA = new ModOwnedUiApi(
                "mod-a",
                new DefaultUiApi());

        UiApi modB = new ModOwnedUiApi(
                "mod-b",
                new DefaultUiApi());

        AtomicInteger aCalls = new AtomicInteger();
        AtomicInteger bCalls = new AtomicInteger();
        AtomicInteger xmlCalls = new AtomicInteger();

        // Mod A adds its own Java button.
        UiMenuButtonRegistration buttonA = modA.registerMenuButton(
                ROOT,
                "mod-a:java-button",
                "Mod A Button",
                aCalls::incrementAndGet);

        buttons.add(buttonA);

        // Mod B also adds a Java button.
        UiMenuButtonRegistration buttonB = modB.registerMenuButton(
                ROOT,
                "mod-b:java-button",
                "Mod B Button",
                bCalls::incrementAndGet);

        buttons.add(buttonB);

        // Mod B registers the callback used
        // by its declarative XML button.
        UiActionRegistration xmlAction = modB.registerGameAction(
                "mod-b:xml-action",
                "XML Action",
                xmlCalls::incrementAndGet);

        actions.add(xmlAction);

        UiDispatcher.processPending();

        // All three mod contributions coexist.
        assertEquals(
                List.of(
                        "game.right.builtin",
                        "mod-b:xml-button",
                        "mod-a:java-button",
                        "mod-b:java-button",
                        ROOT + ".back"),
                ids(first));

        assertTrue(refreshes.get() >= 2);

        // Exercise the same registered-action
        // adapter used by the right-hand panel.
        var binding = new UiRegisteredGameplayActionBinding(
                UiGameplayActionBinding.disabled());

        for (String id : List.of(
                "mod-a:java-button",
                "mod-b:java-button",
                "mod-b:xml-button")) {

            MenuItemDefinition item = find(first, id);

            assertTrue(
                    binding.availability(item)
                            .canExecute());

            binding.execute(item);
        }

        assertEquals(1, aCalls.get());
        assertEquals(1, bCalls.get());
        assertEquals(1, xmlCalls.get());

        // Simulate Game.reloadGameplayMenus().
        MenuManager second = menus(true);

        UiRuntimeMenuActions.bind(
                second, () -> {
                });

        UiDispatcher.processPending();

        // The new registry must contain every
        // contribution exactly once.
        assertEquals(
                List.of(
                        "game.right.builtin",
                        "mod-b:xml-button",
                        "mod-a:java-button",
                        "mod-b:java-button",
                        ROOT + ".back"),
                ids(second));

        // Runtime entries were detached from
        // the old registry.
        assertFalse(
                ids(first).contains(
                        "mod-a:java-button"));

        assertFalse(
                ids(first).contains(
                        "mod-b:java-button"));

        // Simulate Mod A unloading.
        buttonA.unregister();

        UiDispatcher.processPending();

        assertFalse(buttonA.isActive());

        assertFalse(
                UiActionRegistry.isRegistered(
                        "mod-a:java-button"));

        assertFalse(
                ids(second).contains(
                        "mod-a:java-button"));

        // Mod B remains fully functional.
        assertTrue(
                ids(second).contains(
                        "mod-b:java-button"));

        assertTrue(
                ids(second).contains(
                        "mod-b:xml-button"));

        binding.execute(
                find(second, "mod-b:java-button"));

        assertEquals(2, bCalls.get());

        // Simulate Mod B unloading its callbacks.
        buttonB.unregister();
        xmlAction.unregister();

        UiDispatcher.processPending();

        assertFalse(
                ids(second).contains(
                        "mod-b:java-button"));

        // XML is still present until the menu
        // definitions are reloaded without Mod B.
        MenuItemDefinition xmlButton = find(second, "mod-b:xml-button");

        assertFalse(
                binding.availability(xmlButton)
                        .canExecute());

        // Simulate disabling Mod B and rebuilding
        // the menu registry without its XML files.
        MenuManager third = menus(false);

        UiRuntimeMenuActions.bind(
                third, () -> {
                });

        UiDispatcher.processPending();

        assertEquals(
                List.of(
                        "game.right.builtin",
                        ROOT + ".back"),
                ids(third));
    }
}
