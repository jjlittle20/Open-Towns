
package xaos.panels.menus;

import java.util.List;
import java.util.Objects;

/**
 * A contribution to an existing menu.
 *
 * Extensions are separate from the base menu XML.
 * Their items can be inserted at stable item IDs.
 */
public record MenuExtensionDefinition(
        String id,
        String targetMenu,
        List<Contribution> contributions) {

    public MenuExtensionDefinition {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Extension ID cannot be blank");
        }

        if (targetMenu == null || targetMenu.isBlank()) {
            throw new IllegalArgumentException(
                    "Extension targetMenu cannot be blank");
        }

        contributions = List.copyOf(
                Objects.requireNonNull(
                        contributions, "contributions"));
    }

    /**
     * One contributed item with optional placement.
     *
     * Neither before nor after: append before Back.
     */
    public record Contribution(
            MenuItemDefinition item,
            String before,
            String after) {

        public Contribution {

            Objects.requireNonNull(item, "item");

            if (before != null && after != null) {
                throw new IllegalArgumentException(
                        "Extension item cannot specify "
                                + "both before and after: "
                                + item.getId());
            }
        }
    }
}
