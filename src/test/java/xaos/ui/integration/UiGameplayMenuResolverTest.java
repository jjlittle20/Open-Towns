
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuXmlLoader;

import xaos.ui.integration.UiGameplayMenuResolver.DefinitionInfo;
import xaos.ui.integration.UiGameplayMenuResolver.IconType;

class UiGameplayMenuResolverTest {

    private static MenuItemDefinition action(
            String id,
            String titleKey,
            String title,
            String icon,
            String command,
            String parameter) {

        return new MenuItemDefinition(
                id,
                "action",
                titleKey,
                null,
                command,
                false,
                true,
                null,
                null,
                null,
                null,
                true,
                title,
                icon,
                parameter,
                null);
    }

    @Test
    void resolvesDefinitionNameAndItemIcon() {

        UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                key -> key,
                (command, parameter) -> new DefinitionInfo(
                        "Log Wall",
                        "log_wall_item"));

        MenuItemDefinition item = action(
                "game.right.walls.log.wall",
                null,
                null,
                null,
                "QUEUEANDPLACEAREA",
                "qlogwall");

        var result = resolver.resolve(item);

        assertEquals("Log Wall", result.title());

        assertNotNull(result.icon());

        assertEquals(
                "log_wall_item",
                result.icon().id());

        assertEquals(
                IconType.ITEM,
                result.icon().type());
    }

    @Test
    void explicitXmlMetadataTakesPriority() {

        UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                key -> "Translated " + key,
                (command, parameter) -> new DefinitionInfo(
                        "Definition Name",
                        "generated_item"));

        MenuItemDefinition item = action(
                "test.action",
                "test.name",
                "Literal Name",
                "custom_ui_icon",
                "QUEUEANDPLACEAREA",
                "qlogwall");

        var result = resolver.resolve(item);

        assertEquals(
                "Translated test.name",
                result.title());

        assertEquals(
                "custom_ui_icon",
                result.icon().id());

        assertEquals(
                IconType.UI,
                result.icon().type());
    }

    @Test
    void unknownDefinitionPreservesAction() {

        UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                key -> key,
                (command, parameter) -> null);

        MenuItemDefinition item = action(
                "examplemod:unknown-action",
                null,
                null,
                null,
                "CUSTOMACTION",
                "examplemod:unknown");

        var result = resolver.resolve(item);

        assertEquals(
                "examplemod:unknown-action",
                result.title());

        assertNull(result.icon());
    }

    @Test
    void convertedLogWallsMenuResolvesAllActions()
            throws Exception {

        URL resource = getClass()
                .getClassLoader()
                .getResource(
                        "data/menus/right/walls/log.xml");

        assertNotNull(resource);

        MenuDefinition menu = MenuXmlLoader.load(
                Path.of(resource.toURI()).toFile());

        UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                key -> key,
                (command, parameter) -> {

                    assertEquals(
                            "QUEUEANDPLACEAREA",
                            command);

                    return new DefinitionInfo(
                            "Name for " + parameter,
                            "icon_" + parameter);
                });

        long resolved = menu.getItems()
                .stream()
                .filter(item -> "action".equals(item.getType()))
                .map(resolver::resolve)
                .peek(result -> {
                    assertNotNull(result.title());
                    assertNotNull(result.icon());
                    assertEquals(
                            IconType.ITEM,
                            result.icon().type());
                })
                .count();

        assertEquals(5, resolved);
    }

    @Test
    void unsupportedLegacyCommandsReturnNoDefinition() {

        UiLegacyGameplayDefinitionLookup lookup = new UiLegacyGameplayDefinitionLookup();

        assertNull(lookup.apply(
                "NOT_A_REAL_COMMAND",
                "example"));

        assertNull(lookup.apply(
                "QUEUEANDPLACEAREA",
                null));
    }
}
