
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class MenuBottomMigrationParityTest {

    private static final String ROOT = "game.bottom.root";

    private static File resource(String path)
            throws Exception {

        URL url = Objects.requireNonNull(
                MenuBottomMigrationParityTest.class
                        .getClassLoader()
                        .getResource(path),
                "Missing resource: " + path);

        return Path.of(url.toURI()).toFile();
    }

    private static MenuManager convertedMenus()
            throws Exception {

        File root = resource(
                "data/menus/bottom/root.xml");

        MenuManager manager = new MenuManager();

        manager.loadMenus(root.getParentFile());

        return manager;
    }

    @Test
    void convertedMenuTreeIsValid()
            throws Exception {

        MenuManager manager = convertedMenus();

        assertDoesNotThrow(
                () -> manager.validateMenuTree(ROOT));

        assertEquals(
                10,
                manager.requireMenu(ROOT)
                        .getItems().size());

        assertEquals(
                10,
                manager.requireMenu(
                        "game.bottom.stockpiles")
                        .getItems().size());

        assertEquals(
                16,
                manager.requireMenu(
                        "game.bottom.zones")
                        .getItems().size());
    }

    @Test
    void convertedMenusMatchOriginalMenuXml()
            throws Exception {

        var factory = DocumentBuilderFactory.newInstance();

        factory.setFeature(
                "http://apache.org/xml/features/"
                        + "disallow-doctype-decl",
                true);

        Element original = factory
                .newDocumentBuilder()
                .parse(resource("data/menu.xml"))
                .getDocumentElement();

        MenuManager converted = convertedMenus();

        compareMenu(original, converted, ROOT);
    }

    private static void compareMenu(
            Element legacy,
            MenuManager converted,
            String menuId) {
        // Root-level item IDs use game.bottom,
        // not game.bottom.root.
        String idPrefix = ROOT.equals(menuId)
                ? "game.bottom"
                : menuId;
        List<Element> legacyItems = childElements(legacy);

        MenuDefinition modern = converted.requireMenu(menuId);

        List<MenuItemDefinition> modernItems = modern.getItems();

        assertEquals(
                legacyItems.size(),
                modernItems.size(),
                "Entry count differs in " + menuId);

        for (int i = 0; i < legacyItems.size(); i++) {

            Element old = legacyItems.get(i);
            MenuItemDefinition current = modernItems.get(i);

            String tag = old.getTagName();

            if ("item".equals(tag)) {

                String originalId = old.getAttribute("id");

                String expectedId = idPrefix + "." + originalId;

                assertEquals(
                        expectedId,
                        current.getId());

                String command = old.getAttribute("code");

                if ("BACK".equalsIgnoreCase(
                        command)) {

                    assertEquals(
                            "back",
                            current.getType());

                } else {

                    assertEquals(
                            "action",
                            current.getType());

                    assertEquals(
                            command,
                            current.getCommand());

                    assertEquals(
                            optional(old, "parameter"),
                            current.getParameter());

                    assertEquals(
                            optional(old, "parameter2"),
                            current.getParameter2());
                }

                assertEquals(
                        optional(old, "name"),
                        current.getTitle());

                assertEquals(
                        optional(old, "icon"),
                        current.getIcon());

            } else {

                String expectedId = idPrefix + "." + tag;

                assertEquals(
                        "submenu",
                        current.getType());

                assertEquals(
                        expectedId,
                        current.getId());

                assertEquals(
                        expectedId,
                        current.getTargetMenu());

                assertEquals(
                        optional(old, "name"),
                        current.getTitle());

                assertEquals(
                        optional(old, "icon"),
                        current.getIcon());

                compareMenu(
                        old,
                        converted,
                        expectedId);
            }
        }
    }

    private static List<Element> childElements(
            Element parent) {

        List<Element> result = new ArrayList<>();

        NodeList nodes = parent.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {

            Node node = nodes.item(i);

            if (node instanceof Element element) {
                result.add(element);
            }
        }

        return result;
    }

    private static String optional(
            Element element,
            String attribute) {

        String value = element.getAttribute(attribute);

        return value.isBlank()
                ? null
                : value;
    }
}
