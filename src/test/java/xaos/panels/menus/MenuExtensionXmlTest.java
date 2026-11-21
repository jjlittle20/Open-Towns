
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuExtensionXmlTest {

    @TempDir
    Path tempDir;

    private MenuManager rightMenus() throws Exception {

        URL resource = getClass()
                .getClassLoader()
                .getResource("data/menus/right/root.xml");

        assertNotNull(resource);

        MenuManager manager = new MenuManager();

        manager.loadMenus(
                Path.of(resource.toURI())
                        .getParent()
                        .toFile());

        return manager;
    }

    private Path extension(String xml) throws Exception {

        Path file = tempDir.resolve("extension.xml");

        Files.writeString(file, xml);

        return file;
    }

    @Test
    void modCanExtendExistingMenuWithStableItemIds()
            throws Exception {

        MenuManager manager = rightMenus();

        Path file = extension("""
                <menu-extension
                    id="examplemod:log-walls"
                    targetMenu="game.right.walls.log">

                    <item
                        id="examplemod:reinforced-wall"
                        type="action"
                        title="Reinforced Wall"
                        command="QUEUEANDPLACEAREA"
                        parameter="qreinforcedwall"
                        after="game.right.walls.log.wall" />

                    <item
                        id="examplemod:extra-block"
                        type="action"
                        title="Extra Block"
                        command="QUEUEANDPLACEAREA"
                        parameter="qextrablock"
                        before="game.right.walls.log.block2" />

                    <item
                        id="examplemod:extra-scaffold"
                        type="action"
                        title="Extra Scaffold"
                        command="QUEUEANDPLACEAREA"
                        parameter="qextrascaffold" />

                </menu-extension>
                """);

        manager.applyExtension(file.toFile());

        MenuDefinition log = manager.requireMenu(
                "game.right.walls.log");

        List<String> ids = log.getItems()
                .stream()
                .map(MenuItemDefinition::getId)
                .toList();

        assertEquals(
                List.of(
                        "game.right.walls.log.scaffold",
                        "game.right.walls.log.wall",
                        "examplemod:reinforced-wall",
                        "game.right.walls.log.block",
                        "examplemod:extra-block",
                        "game.right.walls.log.block2",
                        "game.right.walls.log.diagonal-block",
                        "examplemod:extra-scaffold",
                        "game.right.walls.log.back"),
                ids);

        // Structured and flattened views agree
        // for a menu without tabs.
        assertEquals(
                log.getContent().size(),
                log.getItems().size());

        // The original submenu relationships
        // remain valid after extension.
        assertDoesNotThrow(() -> manager.validateMenuTree(
                "game.right.root"));
    }

    @Test
    void duplicateItemDoesNotPartiallyChangeMenu()
            throws Exception {

        MenuManager manager = rightMenus();

        MenuDefinition original = manager.requireMenu(
                "game.right.walls.log");

        Path file = extension("""
                <menu-extension
                    id="examplemod:duplicate-test"
                    targetMenu="game.right.walls.log">

                    <item
                        id="examplemod:valid"
                        type="action"
                        command="QUEUEANDPLACEAREA"
                        parameter="qvalid" />

                    <item
                        id="game.right.walls.log.wall"
                        type="action"
                        command="QUEUEANDPLACEAREA"
                        parameter="qduplicate" />

                </menu-extension>
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.applyExtension(file.toFile()));

        // The first valid item was not committed
        // before the second item failed.
        assertSame(
                original,
                manager.requireMenu(
                        "game.right.walls.log"));

        assertEquals(6, original.getItems().size());
    }

    @Test
    void missingPlacementTargetDoesNotChangeMenu()
            throws Exception {

        MenuManager manager = rightMenus();

        MenuDefinition original = manager.requireMenu(
                "game.right.walls.log");

        Path file = extension("""
                <menu-extension
                    id="examplemod:invalid-placement"
                    targetMenu="game.right.walls.log">

                    <item
                        id="examplemod:new-wall"
                        type="action"
                        command="QUEUEANDPLACEAREA"
                        parameter="qnewwall"
                        after="game.right.nonexistent" />

                </menu-extension>
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.applyExtension(file.toFile()));

        assertSame(
                original,
                manager.requireMenu(
                        "game.right.walls.log"));
    }

    @Test
    void sameExtensionCannotBeAppliedTwice()
            throws Exception {

        MenuManager manager = rightMenus();

        Path file = extension("""
                <menu-extension
                    id="examplemod:once"
                    targetMenu="game.right.walls.log">

                    <item
                        id="examplemod:new-log-wall"
                        type="action"
                        command="QUEUEANDPLACEAREA"
                        parameter="qnewlogwall" />

                </menu-extension>
                """);

        manager.applyExtension(file.toFile());

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.applyExtension(file.toFile()));

        assertEquals(
                7,
                manager.requireMenu(
                        "game.right.walls.log")
                        .getItems().size());
    }

    @Test
    void extensionPreservesExistingXmlTabs()
            throws Exception {

        Path menuFolder = Files.createDirectory(
                tempDir.resolve("menus"));

        Files.writeString(
                menuFolder.resolve("settings.xml"),
                """
                        <menu id="test.settings" title="Settings">

                            <tabs id="test.tabs">
                                <tab id="general" title="General">
                                    <item id="test.label"
                                          type="text"
                                          title="Label" />
                                </tab>
                            </tabs>

                            <item id="test.back"
                                  type="back" />

                        </menu>
                        """);

        MenuManager manager = new MenuManager();
        manager.loadMenus(menuFolder.toFile());

        manager.applyExtension(extension("""
                <menu-extension
                    id="examplemod:settings"
                    targetMenu="test.settings">

                    <item id="examplemod:button"
                          type="action"
                          title="New Action"
                          command="CUSTOMACTION"
                          parameter="examplemod:do-something" />

                </menu-extension>
                """).toFile());

        MenuDefinition menu = manager.requireMenu("test.settings");

        assertInstanceOf(
                MenuTabsDefinition.class,
                menu.getContent().get(0));

        assertEquals(
                List.of(
                        "test.label",
                        "examplemod:button",
                        "test.back"),
                menu.getItems().stream()
                        .map(MenuItemDefinition::getId)
                        .toList());
    }
}
