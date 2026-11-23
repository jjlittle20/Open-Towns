
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class MenuRightMigrationParityTest {

    private int menuCount;
    private int actionCount;
    private int backCount;

    @Test
    void convertedHierarchyMatchesOriginalInOrder()
            throws Exception {

        URL rootFile = getClass().getClassLoader()
                .getResource(
                        "data/menus/right/root.xml");

        assertNotNull(rootFile);

        MenuManager manager = new MenuManager();

        manager.loadMenus(
                Path.of(rootFile.toURI())
                        .getParent()
                        .toFile());

        manager.validateMenuTree("game.right.root");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setFeature(
                "http://apache.org/xml/features/"
                        + "disallow-doctype-decl",
                true);

        Element legacyRoot;

        try (InputStream stream = getClass()
                .getClassLoader()
                .getResourceAsStream(
                        "data/menu_right.xml")) {

            assertNotNull(stream);

            legacyRoot = factory
                    .newDocumentBuilder()
                    .parse(stream)
                    .getDocumentElement();
        }

        compare(
                legacyRoot,
                manager.requireMenu("game.right.root"),
                manager);

        assertEquals(27, menuCount);
        assertEquals(301, actionCount);
        assertEquals(26, backCount);

        assertEquals(
                9,
                manager.requireMenu("game.right.root")
                        .getItems().size());

        // These two legacy <brick> nodes must
        // be independent menus with stable IDs.
        assertTrue(manager.hasMenu(
                "game.right.walls.stone.brick"));

        assertTrue(manager.hasMenu(
                "game.right.walls.stone.mayan"));
    }

    private void compare(
            Element legacy,
            MenuDefinition modern,
            MenuManager manager) {

        menuCount++;

        List<Element> children = new ArrayList<>();

        NodeList nodes = legacy.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node instanceof Element element) {
                children.add(element);
            }
        }

        List<MenuItemDefinition> items = modern.getItems();

        assertEquals(
                children.size(),
                items.size(),
                "Item count mismatch in "
                        + modern.getId());

        for (int i = 0; i < children.size(); i++) {

            Element original = children.get(i);
            MenuItemDefinition converted = items.get(i);

            if (!"item".equals(original.getTagName())) {

                assertEquals(
                        "submenu",
                        converted.getType());

                assertEquals(
                        original.getAttribute("name"),
                        converted.getTitle());

                assertEquals(
                        optional(original, "icon"),
                        converted.getIcon());

                MenuDefinition submenu = manager.resolveSubmenu(converted);

                assertEquals(
                        converted.getTargetMenu(),
                        submenu.getId());

                compare(original, submenu, manager);
                continue;
            }

            String code = original.getAttribute("code");

            if ("BACK".equals(code)) {

                backCount++;

                assertEquals(
                        "back",
                        converted.getType());

            } else {

                actionCount++;

                assertEquals(
                        "action",
                        converted.getType());

                assertEquals(
                        code,
                        converted.getCommand());
            }

            assertEquals(
                    optional(original, "parameter"),
                    converted.getParameter());

            assertEquals(
                    optional(original, "parameter2"),
                    converted.getParameter2());

            assertEquals(
                    optional(original, "icon"),
                    converted.getIcon());

            assertEquals(
                    optional(original, "name"),
                    converted.getTitle());
        }
    }

    private static String optional(
            Element element,
            String attribute) {

        String value = element.getAttribute(attribute);

        return value.isBlank() ? null : value;
    }
}
