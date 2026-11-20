
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuXmlLoader;
import xaos.panels.menus.SmartMenu;

class UiLegacySubmenuBridgeTest {

    private static MenuDefinition loadOptions()
            throws Exception {

        URL resource = UiLegacySubmenuBridgeTest.class
                .getClassLoader()
                .getResource(
                        "data/menus/main/options/root.xml");

        assertNotNull(resource);

        return MenuXmlLoader.load(
                Path.of(resource.toURI()).toFile());
    }

    @Test
    void resolvesAllSixOptionsCategories()
            throws Exception {

        MenuDefinition definition = loadOptions();

        SmartMenu legacyOptions = new SmartMenu(
                SmartMenu.TYPE_MENU,
                "Options",
                null,
                null,
                null);

        Map<String, SmartMenu> expected = new HashMap<>();

        // Simulate the legacy tree with the
        // original XML item IDs.
        for (MenuItemDefinition item : definition.getItems()) {

            if (!"submenu".equals(item.getType())) {
                continue;
            }

            SmartMenu child = new SmartMenu(
                    SmartMenu.TYPE_MENU,
                    item.getId(),
                    legacyOptions,
                    null,
                    null);

            child.setID(item.getId());
            legacyOptions.addItem(child);

            expected.put(
                    item.getTargetMenu(),
                    child);
        }

        assertEquals(6, expected.size());

        for (var entry : expected.entrySet()) {

            assertSame(
                    entry.getValue(),
                    UiLegacySubmenuBridge.find(
                            definition,
                            legacyOptions,
                            entry.getKey()));
        }
    }

    @Test
    void unknownTargetIsRejected()
            throws Exception {

        MenuDefinition definition = loadOptions();

        SmartMenu legacyOptions = new SmartMenu(
                SmartMenu.TYPE_MENU,
                "Options",
                null,
                null,
                null);

        assertThrows(
                IllegalArgumentException.class,
                () -> UiLegacySubmenuBridge.find(
                        definition,
                        legacyOptions,
                        "main.options.unknown.root"));
    }

    @Test
    void missingLegacyChildIsNotIgnored()
            throws Exception {

        MenuDefinition definition = loadOptions();

        SmartMenu legacyOptions = new SmartMenu(
                SmartMenu.TYPE_MENU,
                "Options",
                null,
                null,
                null);

        assertThrows(
                IllegalStateException.class,
                () -> UiLegacySubmenuBridge.find(
                        definition,
                        legacyOptions,
                        "main.options.audio.root"));
    }
}
