
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import xaos.actions.QueueItem;

class UiLegacyQueueStepFormatterTest {

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
            default -> id;
        };
    }

    @Test
    void engravedGreenWallPreservesBothWorkStages() {

        // Mirrors qengravedgreenwall from
        // the original src/data/actions.xml.
        List<QueueItem> queue = List.of(
                step(QueueItem.TYPE_LOCK,
                        "masonbench"),
                step(QueueItem.TYPE_PICK,
                        "rmstone"),
                step(QueueItem.TYPE_MOVE,
                        "masonbench"),
                step(QueueItem.TYPE_WAIT,
                        "38"),
                step(QueueItem.TYPE_DESTROY_ITEM,
                        null),
                step(QueueItem.TYPE_PICK,
                        "greencolor,greengel"),
                step(QueueItem.TYPE_MOVE,
                        "masonbench"),
                step(QueueItem.TYPE_WAIT,
                        "20"),
                step(QueueItem.TYPE_DESTROY_ITEM,
                        null),
                step(QueueItem.TYPE_UNLOCK,
                        null),
                step(QueueItem.TYPE_CREATE_ITEM,
                        "engravedgreenwall"));

        UiLegacyQueueStepFormatter formatter = new UiLegacyQueueStepFormatter(
                UiLegacyQueueStepFormatterTest::itemName,
                id -> id);

        List<UiLegacyQueueStepFormatter.Step> steps = formatter.format(queue);

        assertEquals(11, steps.size());

        assertEquals(
                "Reserve Mason Bench",
                steps.get(0).description());

        assertEquals(
                "Collect Raw Stone",
                steps.get(1).description());

        assertEquals(
                "Move to Mason Bench",
                steps.get(2).description());

        assertEquals(
                "Wait 38 base turns",
                steps.get(3).description());

        assertEquals(
                "Collect Green Colour or Green Gel",
                steps.get(5).description());

        assertEquals(
                "Move to Mason Bench",
                steps.get(6).description());

        assertEquals(
                "Wait 20 base turns",
                steps.get(7).description());

        assertEquals(
                "Release Mason Bench",
                steps.get(9).description());

        assertEquals(
                "Create Engraved Green Wall",
                steps.get(10).description());

        // Both visits must remain visible.
        long moves = steps.stream()
                .filter(s -> s.queueType() == QueueItem.TYPE_MOVE)
                .count();

        assertEquals(2, moves);

        // The original operation and value
        // must remain available for inspection.
        assertEquals(
                "greencolor,greengel",
                steps.get(5).rawValue());

        assertEquals(
                7,
                steps.get(6).number());
    }

    @Test
    void unknownOperationsAreNotDiscarded() {

        UiLegacyQueueStepFormatter formatter = new UiLegacyQueueStepFormatter(
                id -> id,
                id -> id);

        var steps = formatter.format(
                List.of(step(999, "custom-data")));

        assertEquals(1, steps.size());

        assertEquals(
                999,
                steps.get(0).queueType());

        assertTrue(
                steps.get(0).description()
                        .contains("custom-data"));
    }

    @Test
    void emptyQueueProducesNoSteps() {

        UiLegacyQueueStepFormatter formatter = new UiLegacyQueueStepFormatter(
                id -> id,
                id -> id);

        assertTrue(
                formatter.format(List.of())
                        .isEmpty());
    }
}
