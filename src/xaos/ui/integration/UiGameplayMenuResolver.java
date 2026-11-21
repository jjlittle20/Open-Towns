
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import xaos.panels.menus.MenuItemDefinition;

/**
 * Resolves display metadata for XML menu entries.
 *
 * Does not execute commands or load artwork.
 * The definition lookup can come from the original
 * game, a mod, or a test implementation.
 */
public final class UiGameplayMenuResolver {

    public enum IconType {
        UI,
        ITEM
    }

    /**
     * A reference to an existing icon asset.
     *
     * The renderer will later translate this
     * into the appropriate game texture.
     */
    public record IconReference(
            String id,
            IconType type) {
    }

    /**
     * Data supplied by a definition registry.
     *
     * itemIconId may be null when the definition
     * has no automatically derived item icon.
     */
    public record DefinitionInfo(
            String name,
            String itemIconId) {
    }

    /**
     * Resolved information ready for UI creation.
     */
    public record ResolvedItem(
            String title,
            IconReference icon) {
    }

    private final Function<String, String> localize;

    private final BiFunction<String, String, DefinitionInfo> definitionLookup;

    public UiGameplayMenuResolver(
            Function<String, String> localize,
            BiFunction<String, String, DefinitionInfo> definitionLookup) {

        this.localize = Objects.requireNonNull(
                localize, "localize");

        this.definitionLookup = Objects.requireNonNull(
                definitionLookup, "definitionLookup");
    }

    public ResolvedItem resolve(MenuItemDefinition item) {

        Objects.requireNonNull(item, "item");

        // --------------------------------------------------------
        // Explicit title or localisation
        // --------------------------------------------------------

        String title = null;

        if (hasText(item.getTitleKey())) {
            title = localize.apply(item.getTitleKey());
        }

        if (!hasText(title)) {
            title = item.getTitle();
        }

        // --------------------------------------------------------
        // Definition lookup
        // --------------------------------------------------------

        DefinitionInfo definition = null;

        if ("action".equals(item.getType())
                && hasText(item.getCommand())
                && hasText(item.getParameter())) {

            definition = definitionLookup.apply(
                    item.getCommand(),
                    item.getParameter());
        }

        if (!hasText(title) && definition != null) {
            title = definition.name();
        }

        // Missing definitions must not silently
        // remove an action from the menu.
        // Display its stable ID until resolved.
        if (!hasText(title)) {
            title = item.getId();
        }

        // --------------------------------------------------------
        // Icon lookup
        // --------------------------------------------------------

        IconReference icon = null;

        if (hasText(item.getIcon())) {

            // XML-specified icons use the UI atlas.
            icon = new IconReference(
                    item.getIcon(),
                    IconType.UI);

        } else if (definition != null
                && hasText(definition.itemIconId())) {

            // Definition-derived icons refer to items.
            icon = new IconReference(
                    definition.itemIconId(),
                    IconType.ITEM);
        }

        return new ResolvedItem(title, icon);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
