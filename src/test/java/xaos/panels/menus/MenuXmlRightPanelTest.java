
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class MenuXmlRightPanelTest {

    private static final String BASE = "data/menus/right/";

    private static MenuDefinition load(String path)
            throws Exception {

        URL resource = MenuXmlRightPanelTest.class
                .getClassLoader()
                .getResource(BASE + path);

        assertNotNull(
                resource,
                "Missing XML resource: " + path);

        return MenuXmlLoader.load(
                Path.of(resource.toURI()).toFile());
    }

    @Test
    void convertedMenusHaveStableIdsAndTargets()
            throws Exception {

        MenuDefinition root = load("root.xml");

        MenuDefinition walls = load("walls/root.xml");

        MenuDefinition log = load("walls/log.xml");

        assertEquals("game.right.root", root.getId());

        assertEquals(
                "game.right.walls",
                walls.getId());

        assertEquals(
                "game.right.walls.log",
                log.getId());

        // Root → Walls
        MenuItemDefinition wallsLink = root.getItems().get(0);

        assertEquals(
                "submenu",
                wallsLink.getType());

        assertEquals(
                walls.getId(),
                wallsLink.getTargetMenu());

        assertEquals(
                "ui_rwalls",
                wallsLink.getIcon());

        // Walls → Log Walls
        MenuItemDefinition logLink = walls.getItems().get(0);

        assertEquals(
                "submenu",
                logLink.getType());

        assertEquals(
                log.getId(),
                logLink.getTargetMenu());

        assertEquals(
                "ui_l_walls",
                logLink.getIcon());

        // Back navigation is a separate item.
        assertEquals(
                "back",
                walls.getItems().get(1).getType());

        assertEquals(
                "back",
                log.getItems().get(5).getType());
    }

    @Test
    void logWallActionsMatchOriginalGameplayXml()
            throws Exception {

        // Read the original legacy XML.
        InputStream stream = getClass()
                .getClassLoader()
                .getResourceAsStream(
                        "data/menu_right.xml");

        assertNotNull(stream);

        Document document;

        try (stream) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true);

            document = factory
                    .newDocumentBuilder()
                    .parse(stream);
        }

        Element walls = (Element) document
                .getDocumentElement()
                .getElementsByTagName("walls")
                .item(0);

        assertNotNull(walls);

        Element log = (Element) walls
                .getElementsByTagName("log")
                .item(0);

        assertNotNull(log);

        // Read actions in original declaration order.
        List<String> oldCommands = new ArrayList<>();
        List<String> oldParameters = new ArrayList<>();

        NodeList children = log.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {

            Node node = children.item(i);

            if (!(node instanceof Element element)
                    || !"item".equals(element.getTagName())) {
                continue;
            }

            String command = element.getAttribute("code");

            if ("BACK".equals(command)) {
                continue;
            }

            oldCommands.add(command);
            oldParameters.add(
                    element.getAttribute("parameter"));
        }

        // Read converted actions.
        MenuDefinition converted = load("walls/log.xml");

        List<MenuItemDefinition> actions = converted.getItems()
                .stream()
                .filter(item -> "action".equals(item.getType()))
                .toList();

        assertEquals(5, actions.size());

        // Commands and parameters must remain
        // identical to the original Towns XML.
        assertEquals(
                oldCommands,
                actions.stream()
                        .map(MenuItemDefinition::getCommand)
                        .toList());

        assertEquals(
                oldParameters,
                actions.stream()
                        .map(MenuItemDefinition::getParameter)
                        .toList());

        // The action IDs are new, stable,
        // menu-specific identifiers.
        assertEquals(
                List.of(
                        "game.right.walls.log.scaffold",
                        "game.right.walls.log.wall",
                        "game.right.walls.log.block",
                        "game.right.walls.log.block2",
                        "game.right.walls.log.diagonal-block"),
                actions.stream()
                        .map(MenuItemDefinition::getId)
                        .toList());
    }

    @Test
    void menuRegistryDiscoversConvertedBranch()
            throws Exception {

        URL resource = getClass()
                .getClassLoader()
                .getResource(BASE + "root.xml");

        assertNotNull(resource);

        Path directory = Path.of(
                resource.toURI()).getParent();

        MenuManager manager = new MenuManager();

        // Load this directory and its children.
        manager.loadMenus(directory.toFile());

        assertTrue(
                manager.hasMenu("game.right.root"));

        assertTrue(
                manager.hasMenu("game.right.walls"));

        assertTrue(
                manager.hasMenu("game.right.walls.log"));

        // Verify the leaf menu is available
        // through the common menu registry.
        MenuDefinition log = manager.getMenu(
                "game.right.walls.log");

        assertNotNull(log);
        assertEquals(6, log.getItems().size());
    }

    @Test
    void convertedRightPanelHasValidSubmenuReferences()
            throws Exception {

        URL resource = getClass()
                .getClassLoader()
                .getResource(BASE + "root.xml");

        assertNotNull(resource);

        Path directory = Path.of(resource.toURI()).getParent();

        MenuManager manager = new MenuManager();

        manager.loadMenus(directory.toFile());

        // Every reachable target must exist,
        // with no duplicate IDs or circular links.
        assertDoesNotThrow(() -> manager.validateMenuTree(
                "game.right.root"));

        MenuDefinition root = manager.requireMenu("game.right.root");

        MenuDefinition walls = manager.resolveSubmenu(
                root.getItems().get(0));

        assertEquals(
                "game.right.walls",
                walls.getId());

        MenuDefinition log = manager.resolveSubmenu(
                walls.getItems().get(0));

        assertEquals(
                "game.right.walls.log",
                log.getId());

        // Original five actions plus Back.
        assertEquals(6, log.getItems().size());

        assertEquals(
                5,
                log.getItems().stream()
                        .filter(item -> "action".equals(item.getType()))
                        .count());
    }

    @Test
    void validationRejectsMissingSubmenuTarget(
            @TempDir Path directory) throws Exception {

        Files.writeString(
                directory.resolve("root.xml"),
                """
                        <menu id="test.root" title="Root">

                            <item
                                id="test.missing"
                                type="submenu"
                                title="Missing"
                                targetMenu="test.not-found" />

                        </menu>
                        """);

        MenuManager manager = new MenuManager();

        manager.loadMenus(directory.toFile());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> manager.validateMenuTree(
                        "test.root"));

        assertTrue(
                exception.getMessage().contains(
                        "test.not-found"));

        assertTrue(
                exception.getMessage().contains(
                        "test.missing"));
    }

    @Test
    void validationRejectsCircularSubmenuReferences(
            @TempDir Path directory) throws Exception {

        Files.writeString(
                directory.resolve("root.xml"),
                """
                        <menu id="test.root" title="Root">

                            <item
                                id="test.root.to-child"
                                type="submenu"
                                title="Child"
                                targetMenu="test.child" />

                        </menu>
                        """);

        Files.writeString(
                directory.resolve("child.xml"),
                """
                        <menu id="test.child" title="Child">

                            <item
                                id="test.child.to-root"
                                type="submenu"
                                title="Root"
                                targetMenu="test.root" />

                        </menu>
                        """);

        MenuManager manager = new MenuManager();

        manager.loadMenus(directory.toFile());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> manager.validateMenuTree(
                        "test.root"));

        assertTrue(
                exception.getMessage().contains(
                        "Circular submenu reference"));
    }

    @Test
    void validationRejectsDuplicateItemIds(
            @TempDir Path directory) throws Exception {

        Files.writeString(
                directory.resolve("root.xml"),
                """
                        <menu id="test.root" title="Root">

                            <item
                                id="test.action"
                                type="action"
                                command="QUEUEANDPLACEAREA"
                                parameter="qlogwall" />

                            <item
                                id="test.action"
                                type="action"
                                command="QUEUEANDPLACEAREA"
                                parameter="qlogblock" />

                        </menu>
                        """);

        MenuManager manager = new MenuManager();

        manager.loadMenus(directory.toFile());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> manager.validateMenuTree(
                        "test.root"));

        assertTrue(
                exception.getMessage().contains(
                        "Duplicate item ID"));

        assertTrue(
                exception.getMessage().contains(
                        "test.action"));
    }

}
