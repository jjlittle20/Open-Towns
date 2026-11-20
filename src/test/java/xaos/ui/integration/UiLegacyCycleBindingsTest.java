
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuXmlLoader;

class UiLegacyCycleBindingsTest {

    private static MenuDefinition loadGame()
            throws Exception {

        URL resource = UiLegacyCycleBindingsTest.class
                .getClassLoader()
                .getResource(
                        "data/menus/main/options/game_menu.xml");

        assertNotNull(resource);

        return MenuXmlLoader.load(
                Path.of(resource.toURI()).toFile());
    }

    @Test
    void bothGameCyclesResolveToExistingCommands()
            throws Exception {

        MenuDefinition game = loadGame();

        List<String> executed = new ArrayList<>();

        UiLegacyCycleBindings bindings = new UiLegacyCycleBindings(
                command -> "Current Value",
                executed::add);

        List<MenuItemDefinition> cycles = game.getItems().stream()
                .filter(item -> "cycle".equals(item.getType()))
                .toList();

        assertEquals(2, cycles.size());

        for (MenuItemDefinition item : cycles) {
            UiCycleBinding binding = bindings.forItem(item);

            assertEquals(
                    "Current Value",
                    binding.value().get());

            binding.action().run();
        }

        assertEquals(
                List.of(
                        CommandPanel.COMMAND_MM_SWITCH_AUTOSAVE_DAYS,
                        CommandPanel.COMMAND_MM_SWITCH_SIEGES),
                executed);
    }

    @Test
    void cycleValuesAreReadLive() throws Exception {

        MenuDefinition game = loadGame();

        MenuItemDefinition autosave = game.getItems().stream()
                .filter(item -> "main.options.game.autosave_days"
                        .equals(item.getId()))
                .findFirst()
                .orElseThrow();

        AtomicInteger days = new AtomicInteger(0);

        UiLegacyCycleBindings bindings = new UiLegacyCycleBindings(
                command -> Integer.toString(
                        days.get()),
                command -> days.incrementAndGet());

        UiCycleBinding binding = bindings.forItem(autosave);

        assertEquals("0", binding.value().get());

        binding.action().run();

        assertEquals("1", binding.value().get());
    }
}
