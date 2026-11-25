
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class UiLegacyProductionQueueReaderTest {

    @Test
    void readsAllThreeProductionCounts() {

        UiLegacyProductionQueueReader reader = new UiLegacyProductionQueueReader(
                Map.of("qbread", 5),
                id -> 3,
                id -> "bread",
                itemId -> {
                    assertEquals("bread", itemId);
                    return 12;
                });

        UiProductionQueueCounts counts = reader.read("qbread");

        assertEquals(5, counts.regular());
        assertEquals(3, counts.automated());
        assertEquals(12, counts.inWorld());
    }

    @Test
    void missingGeneratedItemReturnsZero() {

        AtomicInteger worldLookups = new AtomicInteger();

        UiLegacyProductionQueueReader reader = new UiLegacyProductionQueueReader(
                Map.of(),
                id -> 2,
                id -> null,
                itemId -> {
                    worldLookups.incrementAndGet();
                    return 100;
                });

        UiProductionQueueCounts counts = reader.read("qharvest");

        assertEquals(0, counts.regular());
        assertEquals(2, counts.automated());
        assertEquals(0, counts.inWorld());

        // No physical item lookup should happen.
        assertEquals(0, worldLookups.get());
    }

    @Test
    void blankGeneratedItemReturnsZero() {

        UiLegacyProductionQueueReader reader = new UiLegacyProductionQueueReader(
                Map.of(),
                id -> 0,
                id -> "",
                itemId -> {
                    fail("Should not count a blank item");
                    return 0;
                });

        assertEquals(
                0,
                reader.read("qexample").inWorld());
    }

    @Test
    void readsUpdatedValuesWithoutCaching() {

        Map<String, Integer> regular = new HashMap<>();

        AtomicInteger automated = new AtomicInteger(1);

        AtomicInteger inWorld = new AtomicInteger(3);

        regular.put("qbread", 2);

        UiLegacyProductionQueueReader reader = new UiLegacyProductionQueueReader(
                regular,
                id -> automated.get(),
                id -> "bread",
                id -> inWorld.get());

        UiProductionQueueCounts before = reader.read("qbread");

        regular.put("qbread", 8);
        automated.set(4);
        inWorld.set(15);

        UiProductionQueueCounts after = reader.read("qbread");

        assertEquals(
                new UiProductionQueueCounts(2, 1, 3),
                before);

        assertEquals(
                new UiProductionQueueCounts(8, 4, 15),
                after);
    }

    @Test
    void countsUseProductionActionId() {

        UiLegacyProductionQueueReader reader = new UiLegacyProductionQueueReader(
                Map.of("qironarmor", 6),
                id -> {
                    assertEquals("qironarmor", id);
                    return 2;
                },
                id -> {
                    assertEquals("qironarmor", id);
                    return "ironarmor";
                },
                itemId -> {
                    assertEquals("ironarmor", itemId);
                    return 9;
                });

        assertEquals(
                new UiProductionQueueCounts(6, 2, 9),
                reader.read("qironarmor"));
    }
}
