
package xaos.ui.integration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import xaos.actions.QueueItem;
import xaos.ui.integration.UiLegacyQueueStepFormatter.Step;

/**
 * Summarises pickup instructions without losing
 * their original order or assuming consumption.
 *
 * For example, three separate PICK operations
 * for Raw Stone become:
 *
 * Raw Stone (3 separate pickups)
 *
 * The detailed queue still retains all three.
 */
public final class UiLegacyQueuePickupSummary {

    public record Pickup(
            String name,
            int count,
            List<Integer> instructionNumbers) {

        public Pickup {
            if (count < 1) {
                throw new IllegalArgumentException(
                        "Pickup count must be positive");
            }

            instructionNumbers = List.copyOf(instructionNumbers);
        }

        public String display() {

            if (count == 1) {
                return name;
            }

            return name + " ×" + count;
        }

    }

    public record Summary(
            List<Pickup> items,
            List<Pickup> creatures) {

        public Summary {
            items = List.copyOf(items);
            creatures = List.copyOf(creatures);
        }
    }

    private static final class Accumulator {

        final String name;
        final List<Integer> numbers = new ArrayList<>();

        Accumulator(String name) {
            this.name = name;
        }
    }

    private UiLegacyQueuePickupSummary() {
    }

    public static Summary summarize(List<Step> steps) {

        if (steps == null || steps.isEmpty()) {
            return new Summary(List.of(), List.of());
        }

        // LinkedHashMap preserves the order in which
        // requirements first occur in the queue.
        Map<String, Accumulator> items = new LinkedHashMap<>();

        Map<String, Accumulator> creatures = new LinkedHashMap<>();

        for (Step step : steps) {

            boolean item = step.queueType() == QueueItem.TYPE_PICK;

            boolean creature = step.queueType() == QueueItem.TYPE_PICK_FRIENDLY;

            if (!item && !creature) {
                continue;
            }

            Map<String, Accumulator> target = item ? items : creatures;

            String key = canonicalKey(
                    step.rawValue());

            // Unknown values should not accidentally
            // be merged into one requirement.
            if (key.isEmpty()) {
                key = "<unknown>:" + step.number();
            }

            String name = pickupName(step);

            Accumulator accumulator = target.computeIfAbsent(
                    key,
                    ignored -> new Accumulator(name));

            accumulator.numbers.add(
                    step.number());
        }

        return new Summary(
                finish(items),
                finish(creatures));
    }

    /**
     * Treat alternative lists containing the same
     * item IDs as equivalent, regardless of order.
     *
     * Group by IDs, not translated names, because
     * two different items might share a name.
     */
    private static String canonicalKey(String raw) {

        if (raw == null || raw.isBlank()) {
            return "";
        }

        TreeSet<String> alternatives = new TreeSet<>();

        for (String token : raw.split(",")) {

            String id = token.trim();

            if (!id.isEmpty()) {
                alternatives.add(id);
            }
        }

        return String.join(
                "\u001F",
                alternatives);
    }

    private static String pickupName(Step step) {

        String description = step.description();

        if (description == null
                || description.isBlank()) {
            return "Unspecified pickup";
        }

        if (description.startsWith(
                "Collect creature: ")) {

            return description.substring(
                    "Collect creature: ".length());
        }

        if (description.startsWith("Collect ")) {

            return description.substring(
                    "Collect ".length());
        }

        return description;
    }

    private static List<Pickup> finish(
            Map<String, Accumulator> groups) {

        List<Pickup> result = new ArrayList<>();

        for (Accumulator value : groups.values()) {

            result.add(new Pickup(
                    value.name,
                    value.numbers.size(),
                    value.numbers));
        }

        return List.copyOf(result);
    }
}
