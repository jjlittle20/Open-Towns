
package xaos.panels.menus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Maps IDs from the original menu_right.xml to the
 * stable IDs in the converted built-in menu tree.
 *
 * Does not apply overlays or modify the registry.
 */
public final class MenuLegacyIdResolver {

    public static final String ROOT_MENU = "game.right.root";

    private record Key(
            String parentMenuId,
            String legacyId) {
    }

    private final MenuManager baseMenus;

    private final Map<Key, String> itemIds = new HashMap<>();

    private final Map<Key, List<String>> submenuIds = new HashMap<>();

    private final Set<String> visitedMenus = new HashSet<>();

    private MenuLegacyIdResolver(
            MenuManager baseMenus) {

        this.baseMenus = Objects.requireNonNull(
                baseMenus, "baseMenus");
    }

    /**
     * Use a fresh built-in MenuManager before
     * applying any modern mod extensions.
     */
    public static MenuLegacyIdResolver fromBaseMenu(
            File legacyBaseFile,
            MenuManager builtInMenus) {

        Objects.requireNonNull(
                legacyBaseFile, "legacyBaseFile");

        MenuLegacyIdResolver resolver = new MenuLegacyIdResolver(builtInMenus);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setFeature(
                    "http://apache.org/xml/features/"
                            + "disallow-doctype-decl",
                    true);

            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);

            Element root = factory
                    .newDocumentBuilder()
                    .parse(legacyBaseFile)
                    .getDocumentElement();

            if (!"menu".equalsIgnoreCase(
                    root.getTagName())) {

                throw new IllegalArgumentException(
                        "Expected <menu> in "
                                + legacyBaseFile);
            }

            resolver.index(root, ROOT_MENU);
            return resolver;

        } catch (Exception exception) {

            throw new IllegalArgumentException(
                    "Cannot match legacy menu "
                            + "to converted menus: "
                            + legacyBaseFile,
                    exception);
        }
    }

    /**
     * Resolves a path such as ("walls", "log").
     */
    public String resolvePath(String... legacyTags) {

        String menuId = ROOT_MENU;

        for (String tag : legacyTags) {
            menuId = resolveSubmenu(menuId, tag);
        }

        return menuId;
    }

    /**
     * Resolves a legacy submenu tag relative
     * to its parent menu.
     */
    public String resolveSubmenu(
            String parentMenuId,
            String legacyTag) {

        List<String> matches = submenuIds.get(
                key(parentMenuId, legacyTag));

        if (matches == null || matches.isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown legacy submenu <"
                            + legacyTag
                            + "> in " + parentMenuId);
        }

        if (matches.size() != 1) {

            throw new IllegalArgumentException(
                    "Ambiguous legacy submenu <"
                            + legacyTag
                            + "> in " + parentMenuId
                            + ": " + matches);
        }

        return matches.get(0);
    }

    /**
     * Resolves an old item ID within a known
     * parent menu.
     */
    public String resolveItem(
            String parentMenuId,
            String legacyItemId) {

        String result = itemIds.get(
                key(parentMenuId, legacyItemId));

        if (result == null) {

            throw new IllegalArgumentException(
                    "Unknown legacy item '"
                            + legacyItemId
                            + "' in " + parentMenuId);
        }

        return result;
    }

    /**
     * Walk both trees in parallel, validating
     * their commands, parameters and positions.
     */
    private void index(
            Element legacyMenu,
            String modernMenuId) {

        if (!visitedMenus.add(modernMenuId)) {

            throw new IllegalArgumentException(
                    "Menu indexed more than once: "
                            + modernMenuId);
        }

        MenuDefinition modern = baseMenus.requireMenu(modernMenuId);

        List<Element> legacyChildren = childElements(legacyMenu);

        List<MenuItemDefinition> modernItems = modern.getItems();

        if (legacyChildren.size() != modernItems.size()) {

            throw new IllegalArgumentException(
                    "Menu item count differs in "
                            + modernMenuId
                            + ": legacy="
                            + legacyChildren.size()
                            + ", converted="
                            + modernItems.size());
        }

        for (int i = 0; i < legacyChildren.size(); i++) {

            Element old = legacyChildren.get(i);
            MenuItemDefinition current = modernItems.get(i);

            if ("item".equalsIgnoreCase(
                    old.getTagName())) {

                String code = required(old, "code");

                boolean back = "BACK".equalsIgnoreCase(code);

                String type = back
                        ? "back"
                        : "action";

                if (!type.equals(current.getType())
                        || (!back
                                && (!code.equalsIgnoreCase(
                                        current.getCommand())
                                        || !Objects.equals(
                                                attribute(old, "parameter"),
                                                current.getParameter())
                                        || !Objects.equals(
                                                attribute(old, "parameter2"),
                                                current.getParameter2())))) {

                    throw new IllegalArgumentException(
                            "Legacy action mismatch in "
                                    + modernMenuId
                                    + " at index " + i);
                }

                Key oldId = key(
                        modernMenuId,
                        required(old, "id"));

                if (itemIds.putIfAbsent(
                        oldId, current.getId()) != null) {

                    throw new IllegalArgumentException(
                            "Duplicate legacy item ID "
                                    + oldId);
                }

            } else {

                if (!"submenu".equals(
                        current.getType())
                        || current.getTargetMenu() == null) {

                    throw new IllegalArgumentException(
                            "Legacy submenu mismatch in "
                                    + modernMenuId
                                    + " at index " + i);
                }

                submenuIds.computeIfAbsent(
                        key(modernMenuId,
                                old.getTagName()),
                        ignored -> new ArrayList<>())
                        .add(current.getTargetMenu());

                index(old, current.getTargetMenu());
            }
        }
    }

    private static List<Element> childElements(
            Element parent) {

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

    private static Key key(
            String menuId,
            String legacyId) {

        return new Key(
                normalize(menuId),
                normalize(legacyId));
    }

    private static String normalize(String value) {

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException(
                    "Blank legacy menu identifier");
        }

        return value.trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String required(
            Element element,
            String name) {

        String value = attribute(element, name);

        if (value == null) {

            throw new IllegalArgumentException(
                    "Missing '" + name
                            + "' on <"
                            + element.getTagName() + ">");
        }

        return value;
    }

    private static String attribute(
            Element element,
            String name) {

        String value = element.getAttribute(name);
        return value.isEmpty() ? null : value;
    }
}
