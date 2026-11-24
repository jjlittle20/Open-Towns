
package xaos.ui.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xaos.actions.QueueItem;
import xaos.ui.integration.UiLegacyQueueStepFormatter.Step;

/**
 * Groups original Towns queue instructions into
 * readable construction stages.
 *
 * Every original step is preserved in order.
 *
 * Does not modify the game queue or determine
 * whether a task is currently running.
 */
public final class UiLegacyQueuePhaseGrouper {

    public enum PhaseKind {
        SETUP,
        WORK,
        RELEASE,
        OUTPUT,
        OTHER
    }

    public record Phase(
            int number,
            PhaseKind kind,
            String title,
            List<Step> steps) {

        public Phase {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(title, "title");

            // Ensure stages cannot be modified
            // after they have been created.
            steps = List.copyOf(steps);
        }
    }

    /**
     * Converts an ordered list of formatted
     * queue steps into construction stages.
     */
    public List<Phase> group(List<Step> steps) {

        if (steps == null || steps.isEmpty()) {
            return List.of();
        }

        List<Phase> result = new ArrayList<>();
        List<Step> current = new ArrayList<>();

        PhaseKind currentKind = null;

        for (Step step : steps) {

            Objects.requireNonNull(step, "step");

            PhaseKind newKind = startingKind(step.queueType());

            // Certain operations start a distinct
            // construction stage.
            if (newKind != null) {

                if (!current.isEmpty()) {
                    finish(
                            result,
                            currentKind,
                            current);

                    current.clear();
                }

                currentKind = newKind;
            }

            // Operations without a preceding
            // stage still need somewhere to go.
            if (currentKind == null) {
                currentKind = PhaseKind.OTHER;
            }

            current.add(step);
        }

        if (!current.isEmpty()) {
            finish(result, currentKind, current);
        }

        return List.copyOf(result);
    }

    /**
     * Identify instructions that begin a stage.
     *
     * PICK begins a new work stage even if the
     * same workstation is used repeatedly.
     */
    private static PhaseKind startingKind(int type) {

        return switch (type) {

            case QueueItem.TYPE_LOCK ->
                PhaseKind.SETUP;

            case QueueItem.TYPE_PICK,
                    QueueItem.TYPE_PICK_FRIENDLY ->
                PhaseKind.WORK;

            case QueueItem.TYPE_UNLOCK ->
                PhaseKind.RELEASE;

            case QueueItem.TYPE_CREATE_ITEM,
                    QueueItem.TYPE_CREATE_ITEM_BY_TYPE,
                    QueueItem.TYPE_CREATE_FRIENDLY,
                    QueueItem.TYPE_PLACE_ITEM,
                    QueueItem.TYPE_REPLACE_CELL_ITEM,
                    QueueItem.TYPE_CHANGE_TERRAIN,
                    QueueItem.TYPE_PLACE_FLUID ->
                PhaseKind.OUTPUT;

            default -> null;
        };
    }

    private static void finish(
            List<Phase> result,
            PhaseKind kind,
            List<Step> steps) {

        result.add(new Phase(
                result.size() + 1,
                kind,
                titleFor(kind, steps),
                steps));
    }

    /**
     * Generate a short heading from the actual
     * operations rather than hardcoded action IDs.
     */
    private static String titleFor(
            PhaseKind kind,
            List<Step> steps) {

        Step first = steps.get(0);

        if (kind == PhaseKind.SETUP
                || kind == PhaseKind.RELEASE
                || kind == PhaseKind.OUTPUT) {

            return first.description();
        }

        if (kind == PhaseKind.WORK) {

            String description = first.description();

            String input = description.startsWith("Collect ")
                    ? description.substring(
                            "Collect ".length())
                    : description;

            String destination = null;

            for (Step step : steps) {

                if (step.queueType() == QueueItem.TYPE_MOVE) {

                    String movement = step.description();

                    destination = movement.startsWith("Move to ")
                            ? movement.substring(
                                    "Move to ".length())
                            : movement;

                    break;
                }
            }

            if (destination != null) {
                return "Work with " + input
                        + " at " + destination;
            }

            if (steps.size() > 1) {
                return "Work with " + input;
            }

            return first.description();
        }

        return "Other operations";
    }
}
