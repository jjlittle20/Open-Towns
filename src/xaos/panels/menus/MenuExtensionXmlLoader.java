
package xaos.panels.menus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import xaos.panels.menus.MenuExtensionDefinition.Contribution;

/**
 * Loads declarative contributions to an existing menu.
 *
 * This does not modify the menu registry.
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

            NodeList children = root.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {

                Node node = children.item(i);

                if (!(node instanceof Element element)) {
                    continue;
                }

                // Only item contributions are supported
                // in this first version.
                if (!"item".equals(element.getTagName())) {
                    throw new IllegalArgumentException(
                            "Unsupported extension element: "
                                    + element.getTagName());
                }

                MenuItemDefinition item = MenuXmlLoader.parseItem(
                        element, file);

                contributions.add(
                        new Contribution(
                                item,
                                optional(element, "before"),
                                optional(element, "after")));
            }

            return new MenuExtensionDefinition(
                    id, targetMenu, contributions);

        } catch (Exception exception) {

            throw new IllegalArgumentException(
                    "Failed to load menu extension: "
                            + file.getPath(),
                    exception);
        }
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
