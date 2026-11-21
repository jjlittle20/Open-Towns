
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuXmlGameplayTest {

    @TempDir
    Path tempDir;

    private MenuDefinition parse(String xml)
            throws Exception {

        Path file = tempDir.resolve("game-menu.xml");

        Files.writeString(file, xml);

        return MenuXmlLoader.load(file.toFile());
    }

    private static String messages(Throwable error) {

        StringBuilder result = new StringBuilder();

        while (error != null) {
            result.append(error.getMessage()).append(' ');
            error = error.getCause();
        }

        return result.toString();
    }

    @Test
    void parsesGameplayActionMetadata() throws Exception {

        MenuDefinition menu = parse("""
                <menu id="game.right.walls.log"
                      title="Log walls">

                    <item
                        id="game.right.walls.log.wall"
                        type="action"
                        title="Build Log Wall"
                        icon="ui_l_walls"
                        command="QUEUEANDPLACEAREA"
                        parameter="qlogwall"
                        parameter2="example" />

                </menu>
                """);

        assertEquals(
                "game.right.walls.log",
                menu.getId());

        assertEquals("Log walls", menu.getTitle());
        assertNull(menu.getTitleKey());

        assertEquals(1, menu.getItems().size());

        MenuItemDefinition item = menu.getItems().get(0);

        assertEquals(
                "game.right.walls.log.wall",
                item.getId());

        assertEquals("action", item.getType());

        assertEquals(
                "Build Log Wall",
                item.getTitle());

        assertEquals("ui_l_walls", item.getIcon());

        assertEquals(
                "QUEUEANDPLACEAREA",
                item.getCommand());

        assertEquals(
                "qlogwall",
                item.getParameter());

        assertEquals(
                "example",
                item.getParameter2());
    }

    @Test
    void parsesExplicitSubmenuTargets() throws Exception {

        MenuDefinition menu = parse("""
                <menu id="game.right.root"
                      title="Construction">

                    <item
                        id="game.right.walls"
                        type="submenu"
                        title="Walls"
                        icon="ui_rwalls"
                        targetMenu="game.right.walls" />

                </menu>
                """);

        MenuItemDefinition item = menu.getItems().get(0);

        assertEquals(
                "game.right.walls",
                item.getId());

        assertEquals(
                "game.right.walls",
                item.getTargetMenu());

        assertEquals(
                "ui_rwalls",
                item.getIcon());
    }

    @Test
    void originalMenuAttributesRemainSupported()
            throws Exception {

        MenuDefinition menu = parse("""
                <menu id="main.options.example"
                      titleKey="MainMenuPanel.16">

                    <item
                        id="main.options.example.toggle"
                        type="toggle"
                        titleKey="MainMenuPanel.17"
                        command="COMMAND_MM_TOGGLE_FULL_SCREEN"
                        maintainOpen="true" />

                </menu>
                """);

        assertEquals(
                "MainMenuPanel.16",
                menu.getTitleKey());

        assertNull(menu.getTitle());

        MenuItemDefinition toggle = menu.getItems().get(0);

        assertEquals(
                "toggle",
                toggle.getType());

        assertEquals(
                "MainMenuPanel.17",
                toggle.getTitleKey());

        assertNull(toggle.getIcon());
        assertNull(toggle.getParameter());
    }

    @Test
    void submenuRequiresExplicitTarget() {

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> parse("""
                        <menu id="test.root" title="Test">
                            <item id="test.sub"
                                  type="submenu"
                                  title="Submenu" />
                        </menu>
                        """));

        assertTrue(
                messages(error).contains(
                        "Submenu is missing targetMenu"));
    }

    @Test
    void gameplayActionRequiresCommand() {

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> parse("""
                        <menu id="test.root" title="Test">
                            <item id="test.action"
                                  type="action"
                                  parameter="qlogwall" />
                        </menu>
                        """));

        assertTrue(
                messages(error).contains(
                        "Action is missing command"));
    }

    @Test
    void menuRequiresTitleOrTitleKey() {

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> parse("""
                        <menu id="test.root">
                            <item id="test.back"
                                  type="back" />
                        </menu>
                        """));

        assertTrue(
                messages(error).contains(
                        "titleKey or title"));
    }
}
