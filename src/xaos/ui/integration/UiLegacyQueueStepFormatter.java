
package xaos.ui.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import xaos.actions.QueueItem;

/**
 * Produces a readable, ordered representation
 * of legacy Towns construction instructions.
 *
 * Does not modify the underlying action queue.
 *
 * Internal compatibility adapter, not part
 * of the public UI API.
 */
public final class UiLegacyQueueStepFormatter {

    public record Step(
            int number,
            int queueType,
            String rawValue,
            String description) {
    }

    private final Function<String, String> itemNames;
    private final Function<String, String> creatureNames;

    public UiLegacyQueueStepFormatter(
            Function<String, String> itemNames,
            Function<String, String> creatureNames) {

        this.itemNames = Objects.requireNonNull(
                itemNames, "itemNames");

        this.creatureNames = Objects.requireNonNull(
                creatureNames, "creatureNames");
    }

    public List<Step> format(List<QueueItem> queue) {

        if (queue == null || queue.isEmpty()) {
            return List.of();
        }

        List<Step> result = new ArrayList<>(queue.size());

        String reserved = null;

        for (int index = 0; index < queue.size(); index++) {

            QueueItem operation = queue.get(index);

            if (operation == null) {

                result.add(new Step(
                        index + 1,
                        -1,
                        null,
                        "Unknown queue instruction"));

                continue;
            }

            int type = operation.getType();
            String value = operation.getValue();
            String description;

            switch (type) {

                case QueueItem.TYPE_LOCK -> {

                    reserved = names(value, itemNames);

                    description = "Reserve " + reserved;
                }

                case QueueItem.TYPE_UNLOCK -> {

                    description = "Release " + (reserved != null
                            ? reserved
                            : "reserved workstation");

                    reserved = null;
                }

                case QueueItem.TYPE_PICK ->

                    description = "Collect "
                            + names(value, itemNames);

                case QueueItem.TYPE_PICK_FRIENDLY ->

                    description = "Collect creature: "
                            + names(value, creatureNames);

                case QueueItem.TYPE_MOVE ->

                    description = "Move to "
                            + names(value, itemNames);

                case QueueItem.TYPE_WAIT ->

                    description = "Wait "
                            + safe(value)
                            + " base turns";

                case QueueItem.TYPE_DESTROY_ITEM ->

                    description = "Consume carried item";

                case QueueItem.TYPE_CREATE_ITEM ->

                    description = "Create "
                            + names(value, itemNames);

                case QueueItem.TYPE_CREATE_FRIENDLY ->

                    description = "Create creature: "
                            + names(value, creatureNames);

                case QueueItem.TYPE_PLACE_ITEM ->

                    description = "Place "
                            + names(value, itemNames);

                default ->

                    description = "Other queue instruction (type "
                            + type + ")"
                            + (value == null
                                    || value.isBlank()
                                            ? ""
                                            : ": " + value);
            }

            result.add(new Step(
                    index + 1,
                    type,
                    value,
                    description));
        }

        return List.copyOf(result);
    }

    /**
     * Comma-separated queue values are
     * alternatives, e.g. colour OR gel.
     */
    private static String names(
            String raw,
            Function<String, String> resolver) {

        if (raw == null || raw.isBlank()) {
            return "unspecified item";
        }

        List<String> names = new ArrayList<>();

        for (String token : raw.split(",")) {

            String id = token.trim();

            if (id.isEmpty()) {
                continue;
            }

            String name = resolver.apply(id);

            names.add(
                    name == null || name.isBlank()
                            ? id
                            : name);
        }

        return names.isEmpty()
                ? "unspecified item"
                : String.join(" or ", names);
    }

    private static String safe(String value) {

        return value == null || value.isBlank()
                ? "unspecified"
                : value.trim();
    }
}
