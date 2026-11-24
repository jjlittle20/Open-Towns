
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import xaos.actions.QueueItem;

class UiLegacyQueuePhaseGrouperTest {

    private static QueueItem step(
            int type,
            String value) {

        QueueItem item = new QueueItem();

        item.setType(type);
        item.setValueNoException(value);

        return item;
    }

    private static String itemName(String id) {

        return switch (id) {
            case "masonbench" -> "Mason Bench";
            case "rmstone" -> "Raw Stone";
            case "greencolor" -> "Green Colour";
            case "greengel" -> "Green Gel";
            case "engravedgreenwall" ->
                "Engraved Green Wall";
            case "mill" -> "Mill";
            default -> id;
        };
    }

    private static List<UiLegacyQueueStepFormatter.Step> format(List<QueueItem> queue) {

        return new UiLegacyQueueStepFormatter(
                UiLegacyQueuePhaseGrouperTest::itemName,
                id -> id)
                .format(queue);
    }

    @Test
    void greenWallProducesFiveReadableStages() {

        var queue = List.of(
                step(QueueItem.TYPE_LOCK,
                        "masonbench"),
                step(QueueItem.TYPE_PICK,
                        "rmstone"),
                step(QueueItem.TYPE_MOVE,
                        "masonbench"),
                step(QueueItem.TYPE_WAIT, "38"),
                step(QueueItem.TYPE_DESTROY_ITEM,
                        null),
                step(QueueItem.TYPE_PICK,
                        "greencolor,greengel"),
                step(QueueItem.TYPE_MOVE,
                        "masonbench"),
                step(QueueItem.TYPE_WAIT, "20"),
                step(QueueItem.TYPE_DESTROY_ITEM,
                        null),
                step(QueueItem.TYPE_UNLOCK, null),
                step(QueueItem.TYPE_CREATE_ITEM,
                        "engravedgreenwall"));

        var originalSteps = format(queue);

        var phases = new UiLegacyQueuePhaseGrouper()
                .group(originalSteps);

        assertEquals(5, phases.size());

        assertEquals(
                "Reserve Mason Bench",
                phases.get(0).title());

        assertEquals(
                "Work with Raw Stone at Mason Bench",
                phases.get(1).title());

        assertEquals(
                "Work with Green Colour or Green Gel"
                        + " at Mason Bench",
                phases.get(2).title());

        assertEquals(
                "Release Mason Bench",
                phases.get(3).title());

        assertEquals(
                "Create Engraved Green Wall",
                phases.get(4).title());

        // The first work stage contains its
        // original four instructions.
        assertEquals(
                List.of(2, 3, 4, 5),
                phases.get(1).steps()
                        .stream()
                        .map(s -> s.number())
                        .toList());

        // The second work stage is separate.
        assertEquals(
                List.of(6, 7, 8, 9),
                phases.get(2).steps()
                        .stream()
                        .map(s -> s.number())
                        .toList());

        // Grouping must preserve all eleven
        // instructions in their original order.
        var flattened = phases.stream()
                .flatMap(p -> p.steps().stream())
                .toList();

        assertEquals(originalSteps, flattened);
    }

    @Test
    void repeatedMaterialsRemainSeparateStages() {

        // Similar to multi-stage workshop
        // actions that collect stone repeatedly.
        var queue = List.of(
                step(QueueItem.TYPE_LOCK,
                        "masonbench"),
                step(QueueItem.TYPE_PICK, "rmstone"),
                step(QueueItem.TYPE_MOVE,
                        "masonbench"),
                step(QueueItem.TYPE_WAIT, "21"),
                step(QueueItem.TYPE_DESTROY_ITEM,
                        null),
                step(QueueItem.TYPE_PICK, "rmstone"),
                step(QueueItem.TYPE_MOVE,
                        "masonbench"),
                step(QueueItem.TYPE_WAIT, "21"),
                step(QueueItem.TYPE_DESTROY_ITEM,
                        null),
                step(QueueItem.TYPE_UNLOCK, null),
                step(QueueItem.TYPE_CREATE_ITEM, "mill"));

        var phases = new UiLegacyQueuePhaseGrouper()
                .group(format(queue));

        long stoneStages = phases.stream()
                .filter(p -> p.title().equals(
                        "Work with Raw Stone at Mason Bench"))
                .count();

        assertEquals(2, stoneStages);

        assertEquals(
                11,
                phases.stream()
                        .mapToInt(p -> p.steps().size())
                        .sum());
    }

    @Test
    void unknownInstructionsRemainInTheirStage() {

        var queue = List.of(
                step(QueueItem.TYPE_PICK, "rmstone"),
                step(999, "custom-operation"),
                step(QueueItem.TYPE_WAIT, "10"));

        var original = format(queue);

        var phases = new UiLegacyQueuePhaseGrouper()
                .group(original);

        assertEquals(1, phases.size());

        assertEquals(
                original,
                phases.get(0).steps());

        assertEquals(
                999,
                phases.get(0).steps()
                        .get(1).queueType());
    }

    @Test
    void emptyQueueHasNoPhases() {

        assertTrue(
                new UiLegacyQueuePhaseGrouper()
                        .group(List.of())
                        .isEmpty());
    }

    @Test
    void phasesCannotBeModifiedAfterGrouping() {

        var phases = new UiLegacyQueuePhaseGrouper()
                .group(format(List.of(
                        step(QueueItem.TYPE_PICK,
                                "rmstone"))));

        assertThrows(
                UnsupportedOperationException.class,
                () -> phases.clear());

        assertThrows(
                UnsupportedOperationException.class,
                () -> phases.get(0).steps().clear());
    }
}
