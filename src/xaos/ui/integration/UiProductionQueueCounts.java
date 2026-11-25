
package xaos.ui.integration;

import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Read-only production queue counts for one
 * production action.
 *
 * Does not depend on Game, World, OpenGL,
 * or the original Towns panel classes.
 */
public record UiProductionQueueCounts(
        int regular,
        int automated,
        int inWorld) {

    public UiProductionQueueCounts {

        if (regular < 0
                || automated < 0
                || inWorld < 0) {

            throw new IllegalArgumentException(
                    "Production counts cannot be negative");
        }
    }

    /**
     * Reads the current queue state.
     *
     * Providers are supplied by the game
     * integration layer.
     *
     * An absent regular queue entry means zero,
     * matching the original Towns behaviour.
     */
    public static UiProductionQueueCounts read(
            String actionId,
            Map<String, Integer> regularQueue,
            ToIntFunction<String> automatedQueue,
            ToIntFunction<String> worldItems) {

        if (actionId == null || actionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Production action ID is required");
        }

        Objects.requireNonNull(regularQueue);
        Objects.requireNonNull(automatedQueue);
        Objects.requireNonNull(worldItems);

        Integer regularCount = regularQueue.get(actionId);

        return new UiProductionQueueCounts(
                regularCount == null ? 0 : regularCount,
                automatedQueue.applyAsInt(actionId),
                worldItems.applyAsInt(actionId));
    }

    /**
     * The original production panel only draws
     * an in-world count when it is positive.
     */
    public boolean showInWorldCount() {
        return inWorld > 0;
    }
}
