
package xaos.ui.integration;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import xaos.actions.ActionManager;
import xaos.actions.ActionManagerItem;
import xaos.main.World;
import xaos.tasks.TaskManager;
import xaos.tiles.entities.items.Item;
import xaos.utils.UtilsIniHeaders;

/**
 * Reads live production queue information
 * from the original Towns gameplay systems.
 *
 * This is an internal compatibility adapter.
 *
 * The reusable production UI only needs
 * UiProductionQueueCounts.
 */
public final class UiLegacyProductionQueueReader {

    private final Map<String, Integer> regularQueue;

    private final ToIntFunction<String> automatedQueue;

    private final Function<String, String> generatedItemLookup;

    private final ToIntFunction<String> worldItemCount;

    /**
     * Production constructor.
     *
     * Uses the same sources as the original
     * left-hand production panel.
     */
    public UiLegacyProductionQueueReader(
            TaskManager tasks) {

        this(
                Objects.requireNonNull(
                        tasks, "tasks")
                        .getItemsOnRegularQueue(),

                tasks::getNumItemsOnAutomatedQueue,

                actionId -> {
                    ActionManagerItem action = ActionManager.getItem(actionId);

                    return action == null
                            ? null
                            : action.getGeneratedItem();
                },

                itemId -> Item.getNumItems(
                        UtilsIniHeaders.getIntIniHeader(itemId),
                        false,
                        World.MAP_DEPTH));
    }

    /**
     * Injectable constructor for isolated tests.
     */
    UiLegacyProductionQueueReader(
            Map<String, Integer> regularQueue,
            ToIntFunction<String> automatedQueue,
            Function<String, String> generatedItemLookup,
            ToIntFunction<String> worldItemCount) {

        this.regularQueue = Objects.requireNonNull(regularQueue);

        this.automatedQueue = Objects.requireNonNull(automatedQueue);

        this.generatedItemLookup = Objects.requireNonNull(generatedItemLookup);

        this.worldItemCount = Objects.requireNonNull(worldItemCount);
    }

    /**
     * Reads current values for a production action.
     *
     * No results are cached, so new queue changes
     * are reflected on the next read.
     */
    public UiProductionQueueCounts read(
            String actionId) {

        return UiProductionQueueCounts.read(
                actionId,
                regularQueue,
                automatedQueue,
                this::countGeneratedItem);
    }

    private int countGeneratedItem(
            String actionId) {

        String generatedItem = generatedItemLookup.apply(actionId);

        // Not every action creates a physical item.
        if (generatedItem == null
                || generatedItem.isBlank()) {

            return 0;
        }

        return worldItemCount.applyAsInt(
                generatedItem);
    }
}
