
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class MenuProductionParityTest {

    private static final String BASE_ID = "game.left.production";

    private int menuCount;
    private int actionCount;
    private int backCount;

    @Test
    void entireProductionTreeMatchesOriginal()
            throws Exception {

        URL rootResource = getClass()
                .getClassLoader()
                .getResource(
                        "data/menus/left/production/root.xml");

        assertNotNull(rootResource);

        MenuManager manager = new MenuManager();

        manager.loadMenus(
                Path.of(rootResource.toURI())
                        .getParent()
                        .toFile());

        // Checks missing targets, duplicate item IDs
        // and circular submenu references.
        assertDoesNotThrow(
                () -> manager.validateMenuTree(
                        BASE_ID + ".root"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setExpandEntityReferences(false);

        factory.setFeature(
                "http://apache.org/xml/features/"
                        + "disallow-doctype-decl",
                true);

        Element legacyRoot;

        try (InputStream stream = getClass()
                .getClassLoader()
                .getResourceAsStream(
                        "data/menu_production.xml")) {

            assertNotNull(stream);

            legacyRoot = factory
                    .newDocumentBuilder()
                    .parse(stream)
                    .getDocumentElement();
        }

        MenuDefinition modernRoot = manager.requireMenu(
                BASE_ID + ".root");

        compareMenu(
                legacyRoot,
                modernRoot,
                manager,
                "");

        assertEquals(
                42,
                menuCount,
                "Production menu count changed");

        assertEquals(
                214,
                actionCount,
                "Production action count changed");

        assertEquals(
                41,
                backCount,
                "Production Back control count changed");
    }

    private void compareMenu(
            Element legacy,
            MenuDefinition modern,
            MenuManager manager,
            String legacyPath) {

        menuCount++;

        String expectedTitle = legacyPath.isEmpty()
                ? "Production"
                : legacy.getAttribute("name");

        assertEquals(
                expectedTitle,
                modern.getTitle(),
                "Menu title mismatch: " + modern.getId());

        List<Element> originalChildren = childElements(legacy);

        List<MenuItemDefinition> convertedChildren = modern.getItems();

        assertEquals(
                originalChildren.size(),
                convertedChildren.size(),
                "Item count mismatch: " + modern.getId());

        // IDs must be unique within this menu.
        Set<String> seenIds = new HashSet<>();

        for (MenuItemDefinition item : convertedChildren) {
            assertTrue(
                    seenIds.add(item.getId()),
                    "Duplicate item ID: " + item.getId());
        }

        for (int i = 0; i < originalChildren.size(); i++) {

            Element original = originalChildren.get(i);

            MenuItemDefinition converted = convertedChildren.get(i);

            if ("item".equals(original.getTagName())) {

                compareAction(
                        original,
                        converted,
                        modern.getId());

                continue;
            }

            // Original XML uses nested elements
            // such as <food>, <armor>, <wooden>.
            //
            // The new XML uses explicit submenu IDs.
            String childLegacyPath = legacyPath.isEmpty()
                    ? original.getTagName()
                    : legacyPath + "."
                            + original.getTagName();

            String newSegment = renamedCategory(childLegacyPath);

            String targetId = modern.getId().equals(BASE_ID + ".root")
                    ? BASE_ID + "." + newSegment
                    : modern.getId() + "." + newSegment;

            assertEquals(
                    "submenu",
                    converted.getType(),
                    "Expected submenu: " + targetId);

            assertEquals(
                    targetId,
                    converted.getId());

            assertEquals(
                    targetId,
                    converted.getTargetMenu());

            assertEquals(
                    original.getAttribute("name"),
                    converted.getTitle());

            assertEquals(
                    optional(original, "icon"),
                    converted.getIcon());

            MenuDefinition submenu = manager.resolveSubmenu(converted);

            assertEquals(targetId, submenu.getId());

            // Recursively compare every nested menu.
            compareMenu(
                    original,
                    submenu,
                    manager,
                    childLegacyPath);
        }
    }

    private void compareAction(
            Element original,
            MenuItemDefinition converted,
            String menuId) {

        String originalId = original.getAttribute("id");

        String originalCommand = original.getAttribute("code");

        assertEquals(
                menuId + "." + originalId,
                converted.getId());

        if ("BACK".equals(originalCommand)) {

            backCount++;

            assertEquals(
                    "back",
                    converted.getType());

            assertNull(converted.getCommand());

        } else {

            actionCount++;

            assertEquals(
                    "action",
                    converted.getType());

            assertEquals(
                    originalCommand,
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

    private static String renamedCategory(
            String originalPath) {

        return switch (originalPath) {

            case "militaries.armor.spiderbane" ->
                "spiderite";

            case "militaries.armor.horde" ->
                "goblinite";

            case "militaries.armor.yellowlick" ->
                "hobgoblinite";

            case "militaries.armor.mangy" ->
                "direite";

            case "militaries.armor.stealthy" ->
                "snickerite";

            case "militaries.armor.necrotic" ->
                "zombite";

            case "militaries.armor.ghastly" ->
                "ghoulite";

            case "militaries.armor.beastly" ->
                "ogreite";

            case "militaries.armor.gold" ->
                "golden";

            default -> {
                int lastDot = originalPath.lastIndexOf('.');

                yield originalPath.substring(
                        lastDot + 1);
            }
        };
    }

    private static String optional(
            Element element,
            String attribute) {

        String value = element.getAttribute(attribute);

        return value.isBlank() ? null : value;
    }

    private static List<Element> childElements(
            Element parent) {

        java.util.ArrayList<Element> result = new java.util.ArrayList<>();

        NodeList nodes = parent.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {

            Node node = nodes.item(i);

            if (node instanceof Element element) {
                result.add(element);
            }
        }

        return result;
    }
}
