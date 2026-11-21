
package xaos.panels.menus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import xaos.panels.menus.MenuExtensionDefinition.AddedTab;
import xaos.panels.menus.MenuExtensionDefinition.Contribution;
import xaos.panels.menus.MenuExtensionDefinition.TabItem;

/**
 * XML parser for additions to existing menus
 * and tab groups.
 */
public final class MenuExtensionXmlLoader {

        private MenuExtensionXmlLoader() {
        }

        public static MenuExtensionDefinition load(File file) {

                Objects.requireNonNull(file, "file");

                try {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

                        factory.setExpandEntityReferences(false);
                        factory.setXIncludeAware(false);

                        factory.setFeature(
                                        "http://apache.org/xml/features/disallow-doctype-decl",
                                        true);

                        factory.setFeature(
                                        "http://xml.org/sax/features/external-general-entities",
                                        false);

                        factory.setFeature(
                                        "http://xml.org/sax/features/external-parameter-entities",
                                        false);

                        factory.setFeature(
                                        "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                        false);

                        Element root = factory
                                        .newDocumentBuilder()
                                        .parse(file)
                                        .getDocumentElement();

                        if (!"menu-extension".equals(root.getTagName())) {
                                throw new IllegalArgumentException(
                                                "Expected <menu-extension>: "
                                                                + file.getPath());
                        }

                        String id = required(root, "id");
                        String targetMenu = required(root, "targetMenu");

                        List<Contribution> contributions = new ArrayList<>();

                        List<AddedTab> addedTabs = new ArrayList<>();

                        List<TabItem> tabItems = new ArrayList<>();

                        for (Element child : children(root)) {

                                switch (child.getTagName()) {

                                        // Existing top-level item support.
                                        case "item" -> contributions.add(
                                                        new Contribution(
                                                                        MenuXmlLoader.parseItem(
                                                                                        child, file),
                                                                        optional(child, "before"),
                                                                        optional(child, "after")));

                                        // Create a new tab.
                                        case "tab" -> {

                                                List<MenuItemDefinition> items = new ArrayList<>();

                                                for (Element entry : children(child)) {

                                                        if (!"item".equals(
                                                                        entry.getTagName())) {

                                                                throw new IllegalArgumentException(
                                                                                "Only <item> is allowed "
                                                                                                + "inside an added tab");
                                                        }

                                                        if (optional(entry, "before") != null
                                                                        || optional(entry, "after") != null) {

                                                                throw new IllegalArgumentException(
                                                                                "Items inside a new tab "
                                                                                                + "cannot specify before/after");
                                                        }

                                                        items.add(
                                                                        MenuXmlLoader.parseItem(
                                                                                        entry, file));
                                                }

                                                MenuTabDefinition tab = new MenuTabDefinition(
                                                                required(child, "id"),
                                                                optional(child, "titleKey"),
                                                                optional(child, "title"),
                                                                items);

                                                addedTabs.add(new AddedTab(
                                                                required(child, "targetTabs"),
                                                                tab,
                                                                optional(child, "before"),
                                                                optional(child, "after")));
                                        }

                                        // Extend an existing tab.
                                        case "tab-extension" -> {

                                                String targetTabs = required(child, "targetTabs");

                                                String targetTab = required(child, "targetTab");

                                                for (Element entry : children(child)) {

                                                        if (!"item".equals(
                                                                        entry.getTagName())) {

                                                                throw new IllegalArgumentException(
                                                                                "Only <item> is allowed "
                                                                                                + "inside <tab-extension>");
                                                        }

                                                        tabItems.add(new TabItem(
                                                                        targetTabs,
                                                                        targetTab,
                                                                        MenuXmlLoader.parseItem(
                                                                                        entry, file),
                                                                        optional(entry, "before"),
                                                                        optional(entry, "after")));
                                                }
                                        }

                                        default -> throw new IllegalArgumentException(
                                                        "Unsupported extension element: "
                                                                        + child.getTagName());
                                }
                        }

                        return new MenuExtensionDefinition(
                                        id,
                                        targetMenu,
                                        contributions,
                                        addedTabs,
                                        tabItems);

                } catch (Exception exception) {

                        throw new IllegalArgumentException(
                                        "Failed to load menu extension: "
                                                        + file.getPath(),
                                        exception);
                }
        }

        private static List<Element> children(Element parent) {

                List<Element> elements = new ArrayList<>();

                NodeList nodes = parent.getChildNodes();

                for (int i = 0; i < nodes.getLength(); i++) {

                        Node node = nodes.item(i);

                        if (node instanceof Element element) {
                                elements.add(element);
                        }
                }

                return elements;
        }

        private static String required(
                        Element element,
                        String attribute) {

                String value = optional(element, attribute);

                if (value == null) {
                        throw new IllegalArgumentException(
                                        "Missing attribute: " + attribute);
                }

                return value;
        }

        private static String optional(
                        Element element,
                        String attribute) {

                String value = element.getAttribute(attribute);

                return value == null || value.isBlank()
                                ? null
                                : value.trim();
        }
}
