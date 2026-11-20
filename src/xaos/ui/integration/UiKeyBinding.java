
package xaos.ui.integration;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Read-only view of an externally owned
 * keyboard binding.
 *
 * A binding has an action name and up to
 * two assigned physical keys.
 *
 * Key capture and reassignment will be
 * handled separately.
 */
public record UiKeyBinding(
        String itemId,
        int functionId,
        Supplier<String> description,
        IntSupplier primaryKey,
        IntSupplier secondaryKey,
        IntFunction<String> keyName,
        int unboundKey) {

    public UiKeyBinding {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(primaryKey, "primaryKey");
        Objects.requireNonNull(secondaryKey, "secondaryKey");
        Objects.requireNonNull(keyName, "keyName");
    }

    public int primaryCode() {
        return primaryKey.getAsInt();
    }

    public int secondaryCode() {
        return secondaryKey.getAsInt();
    }

    /**
     * Returns the current translated action
     * name and assigned keys.
     *
     * Values are read live, so changes made
     * elsewhere appear automatically.
     */
    public String displayText() {

        String label = Objects.requireNonNull(
                description.get(),
                "Keyboard action description");

        StringJoiner keys = new StringJoiner(", ");

        int primary = primaryCode();
        int secondary = secondaryCode();

        if (primary != unboundKey) {
            keys.add(keyName.apply(primary));
        }

        if (secondary != unboundKey
                && secondary != primary) {

            keys.add(keyName.apply(secondary));
        }

        String assigned = keys.toString();

        if (assigned.isEmpty()) {
            return label;
        }

        return label + " (" + assigned + ")";
    }
}
