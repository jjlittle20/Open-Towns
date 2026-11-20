package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuXmlTabsTest {

        @TempDir
        Path tempDir;

        private MenuDefinition parse(String xml) throws IOException {
                Path path = tempDir.resolve("menu.xml");
                Files.writeString(path, xml);
                return MenuXmlLoader.load(path.toFile());
        }

        private static String messageOf(Throwable throwable) {
                StringBuilder text = new StringBuilder();
                while (throwable != null) {
                        text.append(throwable.getMessage()).append(' ');
                        throwable = throwable.getCause();
                }
                return text.toString();
        }

        @Test
        void parsesTabbedMenuAndPreservesLegacyFlatItemOrder() throws Exception {
                MenuDefinition menu = parse("""
                                <menu id="test.menu" titleKey="test.menu.title">
                                  <item id="intro" type="text" titleKey="intro.title" />
                                  <tabs id="settings.tabs" placement="left">
                                    <tab id="movement" titleKey="movement.title">
                                      <item id="move.up" type="keyboard" key="FN_UP" />
                                      <item id="move.down" type="keyboard" key="FN_DOWN" />
                                    </tab>
                                    <tab id="mod:tools" title="Mod Tools">
                                      <item id="mod:action" type="keyboard" key="FN_SCREENSHOT" />
                                    </tab>
                                  </tabs>
                                  <item id="back" type="back" />
                                </menu>
                                """);

                assertEquals("test.menu", menu.getId());
                assertEquals(3, menu.getContent().size());
                assertInstanceOf(MenuItemDefinition.class, menu.getContent().get(0));
                MenuTabsDefinition tabs = assertInstanceOf(
                                MenuTabsDefinition.class, menu.getContent().get(1));
                assertInstanceOf(MenuItemDefinition.class, menu.getContent().get(2));

                assertEquals("settings.tabs", tabs.getId());
                assertEquals(MenuTabsDefinition.Placement.LEFT, tabs.getPlacement());
                assertEquals(List.of("movement", "mod:tools"),
                                tabs.getTabs().stream().map(MenuTabDefinition::getId).toList());
                assertEquals("movement.title", tabs.getTabs().get(0).getTitleKey());
                assertEquals("Mod Tools", tabs.getTabs().get(1).getTitle());
                assertEquals("FN_UP", tabs.getTabs().get(0).getItems().get(0).getKey());

                // Existing MenuManager.buildSmartMenu() still sees all items.
                assertEquals(List.of("intro", "move.up", "move.down", "mod:action", "back"),
                                menu.getItems().stream().map(MenuItemDefinition::getId).toList());
        }

        @Test
        void placementDefaultsToTop() throws Exception {
                MenuDefinition menu = parse("""
                                <menu id="m" titleKey="m.title">
                                  <tabs id="t"><tab id="a" title="A" /></tabs>
                                </menu>
                                """);
                MenuTabsDefinition group = (MenuTabsDefinition) menu.getContent().get(0);
                assertEquals(MenuTabsDefinition.Placement.TOP, group.getPlacement());
                assertTrue(group.getTabs().get(0).getItems().isEmpty());
        }

        @Test
        void rejectsInvalidPlacementAndDuplicateTabIds() {
                RuntimeException badPlacement = assertThrows(RuntimeException.class,
                                () -> parse("""
                                                <menu id="m" titleKey="m.title"><tabs id="t" placement="middle">
                                                  <tab id="a" title="A" />
                                                </tabs></menu>
                                                """));
                assertTrue(messageOf(badPlacement).contains("Invalid tab placement"));

                RuntimeException duplicate = assertThrows(RuntimeException.class,
                                () -> parse("""
                                                <menu id="m" titleKey="m.title"><tabs id="t">
                                                  <tab id="a" title="A" />
                                                  <tab id="a" title="A Again" />
                                                </tabs></menu>
                                                """));
                assertTrue(messageOf(duplicate).contains("Duplicate tab id"));
        }

        @Test
        void rejectsMissingTitlesAndUnsupportedNestedElements() {
                RuntimeException noTitle = assertThrows(RuntimeException.class,
                                () -> parse("""
                                                <menu id="m" titleKey="m.title">
                                                  <tabs id="t"><tab id="a" /></tabs>
                                                </menu>
                                                """));
                assertTrue(messageOf(noTitle).contains("titleKey or title"));

                RuntimeException nested = assertThrows(RuntimeException.class,
                                () -> parse("""
                                                <menu id="m" titleKey="m.title">
                                                  <tabs id="t"><tab id="a" title="A">
                                                    <tabs id="nested" />
                                                  </tab></tabs>
                                                </menu>
                                                """));
                assertTrue(messageOf(nested).contains("Only <item> is allowed"));
        }

        @Test
        void originalControlsXmlUsesFiveTabs()
                        throws Exception {

                URL resource = getClass()
                                .getClassLoader()
                                .getResource(
                                                "data/menus/main/options/controls_menu.xml");

                assertNotNull(resource);

                MenuDefinition controls = MenuXmlLoader.load(
                                Path.of(resource.toURI()).toFile());

                assertEquals(
                                "main.options.controls.root",
                                controls.getId());

                // One tab group plus the shared Back item.
                assertEquals(2, controls.getContent().size());

                MenuTabsDefinition tabs = assertInstanceOf(
                                MenuTabsDefinition.class,
                                controls.getContent().get(0));

                assertEquals(
                                MenuTabsDefinition.Placement.TOP,
                                tabs.getPlacement());

                assertEquals(
                                List.of(
                                                "movement",
                                                "panels",
                                                "navigation",
                                                "misc",
                                                "bottom-menu"),
                                tabs.getTabs().stream()
                                                .map(MenuTabDefinition::getId)
                                                .toList());

                assertEquals(
                                List.of(8, 6, 8, 13, 13),
                                tabs.getTabs().stream()
                                                .map(tab -> tab.getItems().size())
                                                .toList());

                // The legacy SmartMenu must still receive
                // exactly the same flattened item count.
                assertEquals(49, controls.getItems().size());

                assertEquals(
                                37,
                                controls.getItems().stream()
                                                .filter(item -> "keyboard".equals(item.getType()))
                                                .count());

                // All five legacy headings remain available.
                assertEquals(
                                5,
                                controls.getItems().stream()
                                                .filter(item -> "text".equals(item.getType()))
                                                .count());

                // Check representative bindings.
                assertEquals(
                                "FN_UP",
                                tabs.getTabs().get(0)
                                                .getItems().get(2).getKey());

                assertEquals(
                                "FN_BOT_10",
                                tabs.getTabs().get(4)
                                                .getItems().get(11).getKey());

                // Back must remain outside the tabs.
                MenuItemDefinition back = assertInstanceOf(
                                MenuItemDefinition.class,
                                controls.getContent().get(1));

                assertEquals(
                                "main.options.controls.back",
                                back.getId());

                assertEquals("back", back.getType());
        }

}
