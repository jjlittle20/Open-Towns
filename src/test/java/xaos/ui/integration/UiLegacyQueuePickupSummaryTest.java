
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import xaos.actions.QueueItem;
import xaos.ui.integration.UiLegacyQueueStepFormatter.Step;

class UiLegacyQueuePickupSummaryTest {

    private static Step step(
            int number,
            int type,
            String raw,
            String description) {

        return new Step(
                number, type, raw, description);
    }

    @Test
    void millShowsThreeSeparateStonePickups() {

        // Representative pickup operations from qmill.
        var steps = List.of(
                step(2, QueueItem.TYPE_PICK,
                        "rmstone",
                        "Collect Raw Stone"),

                step(3, QueueItem.TYPE_MOVE,
                        "masonbench",
                        "Move to Mason Bench"),

                step(6, QueueItem.TYPE_PICK,
                        "rmstone",
                        "Collect Raw Stone"),

                step(10, QueueItem.TYPE_PICK,
                        "rmstone",
                        "Collect Raw Stone"),

                step(16, QueueItem.TYPE_PICK,
                        "rmwood",
                        "Collect Raw Wood"));

        var summary = UiLegacyQueuePickupSummary
                .summarize(steps);

        assertEquals(2, summary.items().size());

        var stone = summary.items().get(0);

        assertEquals("Raw Stone", stone.name());
        assertEquals(3, stone.count());

        assertEquals(
                List.of(2, 6, 10),
                stone.instructionNumbers());

        assertEquals(
                "Raw Stone ×3",
                stone.display());

        assertEquals(
                "Raw Wood",
                summary.items().get(1).display());

        // A MOVE operation is not a pickup.
        assertTrue(summary.creatures().isEmpty());
    }

    @Test
    void alternativeItemsRemainOnePickupGroup() {

        var summary = UiLegacyQueuePickupSummary
                .summarize(List.of(
                        step(
                                1,
                                QueueItem.TYPE_PICK,
                                "greencolor,greengel",
                                "Collect Green Colour or Green Gel")));

        assertEquals(1, summary.items().size());
        assertEquals(1, summary.items().get(0).count());

        assertEquals(
                "Green Colour or Green Gel",
                summary.items().get(0).display());
    }

    @Test
    void reorderedAlternativesAreSameGroup() {

        var summary = UiLegacyQueuePickupSummary
                .summarize(List.of(
                        step(
                                1,
                                QueueItem.TYPE_PICK,
                                "greencolor,greengel",
                                "Collect Green Colour or Green Gel"),

                        step(
                                5,
                                QueueItem.TYPE_PICK,
                                "greengel,greencolor",
                                "Collect Green Gel or Green Colour")));

        assertEquals(1, summary.items().size());

        assertEquals(
                "Green Colour or Green Gel ×2",
                summary.items().get(0).display());

        assertEquals(
                List.of(1, 5),
                summary.items().get(0)
                        .instructionNumbers());
    }

    @Test
    void creaturePickupsRemainSeparateFromItems() {

        var summary = UiLegacyQueuePickupSummary
                .summarize(List.of(
                        step(
                                1,
                                QueueItem.TYPE_PICK,
                                "pig",
                                "Collect Pig Item"),

                        step(
                                2,
                                QueueItem.TYPE_PICK_FRIENDLY,
                                "pig",
                                "Collect creature: Pig")));

        assertEquals(
                "Pig Item",
                summary.items().get(0).display());

        assertEquals(
                "Pig",
                summary.creatures().get(0).display());
    }

    @Test
    void differentIdsWithSameNameStaySeparate() {

        var summary = UiLegacyQueuePickupSummary
                .summarize(List.of(
                        step(
                                1,
                                QueueItem.TYPE_PICK,
                                "wood_a",
                                "Collect Wood"),

                        step(
                                2,
                                QueueItem.TYPE_PICK,
                                "wood_b",
                                "Collect Wood")));

        assertEquals(2, summary.items().size());
    }

    @Test
    void emptyInputProducesEmptySummary() {

        var summary = UiLegacyQueuePickupSummary
                .summarize(List.of());

        assertTrue(summary.items().isEmpty());
        assertTrue(summary.creatures().isEmpty());
    }
}
