
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuXmlLoader;

class UiLegacyToggleBindingsTest {

    private static MenuDefinition loadMenu(
            String path) throws Exception {

        URL resource = UiLegacyToggleBindingsTest.class
                .getClassLoader()
                .getResource(path);

        assertNotNull(resource);

        return MenuXmlLoader.load(
                Path.of(resource.toURI()).toFile());
    }

    private static UiLegacyToggleBindings testBindings(List<String> executed) {

        return new UiLegacyToggleBindings(
                command -> false,
                executed::add);
    }

    @Test
    void resolvesXmlCommandToActualCommandValue()
            throws Exception {

        MenuDefinition game = loadMenu(
                "data/menus/main/options/game_menu.xml");

        MenuItemDefinition mouseScroll = game.getItems().stream()
                .filter(item -> "main.options.game.mouse_scroll"
                        .equals(item.getId()))
                .findFirst()
                .orElseThrow();

        List<String> executed = new ArrayList<>();

        UiLegacyToggleBindings bindings = testBindings(executed);

        UiToggleBinding binding = bindings.forItem(mouseScroll);

        // Creating the binding must not execute
        // or modify the actual setting.
        assertTrue(executed.isEmpty());

        binding.action().run();

        assertEquals(
                List.of(
                        CommandPanel.COMMAND_MM_SWITCH_MOUSE_SCROLL),
                executed);

        // It must NOT pass the literal XML field
        // name to the dispatcher.
        assertNotEquals(
                mouseScroll.getCommand(),
                executed.get(0));
    }

    @Test
    void bindingReadsLiveStateRatherThanCachingIt()
            throws Exception {

        MenuDefinition graphics = loadMenu(
                "data/menus/main/options/graphics_menu.xml");

        MenuItemDefinition fullscreen = graphics.getItems().stream()
                .filter(item -> "toggle".equals(item.getType()))
                .findFirst()
                .orElseThrow();

        AtomicBoolean enabled = new AtomicBoolean(false);

        List<String> executed = new ArrayList<>();

        UiLegacyToggleBindings bindings = new UiLegacyToggleBindings(
                command -> enabled.get(),
                executed::add);

        UiToggleBinding binding = bindings.forItem(fullscreen);

        assertFalse(binding.state().getAsBoolean());

        // Simulate a setting change elsewhere
        // in the game.
        enabled.set(true);

        assertTrue(binding.state().getAsBoolean());

        binding.action().run();

        assertEquals(
                List.of(
                        CommandPanel.COMMAND_MM_TOGGLE_FULL_SCREEN),
                executed);
    }

    @Test
    void supportsAllOriginalGameToggleSettings()
            throws Exception {

        MenuDefinition game = loadMenu(
                "data/menus/main/options/game_menu.xml");

        UiLegacyToggleBindings bindings = testBindings(new ArrayList<>());

        int toggles = 0;
        int cycles = 0;

        for (MenuItemDefinition item : game.getItems()) {

            if ("toggle".equals(item.getType())) {

                assertNotNull(
                        bindings.forItem(item));

                toggles++;

            } else if ("cycle".equals(item.getType())) {

                cycles++;
            }
        }

        assertEquals(7, toggles);
        assertEquals(2, cycles);
    }

    @Test
    void supportsGraphicsAndAudioToggles()
            throws Exception {

        UiLegacyToggleBindings bindings = testBindings(new ArrayList<>());

        MenuDefinition graphics = loadMenu(
                "data/menus/main/options/graphics_menu.xml");

        MenuDefinition audio = loadMenu(
                "data/menus/main/options/audio_menu.xml");

        int toggles = 0;

        for (MenuDefinition definition : List.of(graphics, audio)) {

            for (MenuItemDefinition item : definition.getItems()) {

                if ("toggle".equals(item.getType())) {

                    assertNotNull(
                            bindings.forItem(item));

                    toggles++;
                }
            }
        }

        // One Graphics toggle, two Audio toggles.
        assertEquals(3, toggles);
    }

    @Test
    void rejectsUnsupportedCommands()
            throws Exception {

        MenuDefinition game = loadMenu(
                "data/menus/main/options/game_menu.xml");

        UiLegacyToggleBindings bindings = testBindings(new ArrayList<>());

        MenuItemDefinition cycle = game.getItems().stream()
                .filter(item -> "cycle".equals(item.getType()))
                .findFirst()
                .orElseThrow();

        // A cycle cannot accidentally be
        // treated as a boolean toggle.
        assertThrows(
                IllegalArgumentException.class,
                () -> bindings.forItem(cycle));

        MenuItemDefinition unknown = new MenuItemDefinition(
                "test.unknown",
                "toggle",
                "Test",
                null,
                "COMMAND_DOES_NOT_EXIST",
                false,
                false,
                null,
                null,
                null,
                null,
                false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bindings.forItem(unknown));

        assertTrue(
                exception.getMessage().contains(
                        "COMMAND_DOES_NOT_EXIST"));
    }
}
