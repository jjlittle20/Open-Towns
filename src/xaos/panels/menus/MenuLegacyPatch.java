
package xaos.panels.menus;

import java.util.List;
import java.util.Objects;

/**
 * Internal representation of changes from a
 * legacy Towns menu overlay.
 *
 * This is a compatibility mechanism, not
 * part of the public modding API.
 */
public record MenuLegacyPatch(
        String menuId,
        List<Edit> edits) {

    public MenuLegacyPatch {

        if (menuId == null || menuId.isBlank()) {
            throw new IllegalArgumentException(
                    "Menu ID cannot be blank");
        }

        edits = List.copyOf(
                Objects.requireNonNull(edits, "edits"));
    }

    public sealed interface Edit
            permits Add, Remove, Replace, Clear {
    }

    /**
     * Insert a new item before any trailing
     * Back button.
     */
    public record Add(MenuItemDefinition item)
            implements Edit {

        public Add {
            Objects.requireNonNull(item, "item");
        }
    }

    /**
     * Remove one existing entry by its
     * resolved stable ID.
     */
    public record Remove(String itemId)
            implements Edit {

        public Remove {
            requireId(itemId);
        }
    }

    /**
     * Update an existing item's metadata
     * while preserving its ID and position.
     *
     * Used for existing submenu changes.
     */
    public record Replace(
            String itemId,
            MenuItemDefinition item)
            implements Edit {

        public Replace {

            requireId(itemId);
            Objects.requireNonNull(item, "item");

            if (!itemId.equalsIgnoreCase(
                    item.getId())) {

                throw new IllegalArgumentException(
                        "Replacement must preserve "
                                + "the existing item ID");
            }
        }
    }

    /**
     * Remove every entry from one menu.
     *
     * Matches legacy deleteContent behaviour.
     */
    public record Clear() implements Edit {
    }

    private static void requireId(String id) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Item ID cannot be blank");
        }
    }
}
