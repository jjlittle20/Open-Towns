
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class UiProductionQueueCountsTest {

    @Test
    void readsRegularAndAutomatedCounts() {

        Map<String, Integer> regular = Map.of("qbread", 5);

        UiProductionQueueCounts counts = UiProductionQueueCounts.read(
                "qbread",
                regular,
                id -> 3,
                id -> 12);

        assertEquals(5, counts.regular());
        assertEquals(3, counts.automated());
        assertEquals(12, counts.inWorld());

        assertTrue(counts.showInWorldCount());
    }

    @Test
    void missingRegularQueueEntryMeansZero() {

        UiProductionQueueCounts counts = UiProductionQueueCounts.read(
                "qbread",
                Map.of(),
                id -> 2,
                id -> 0);

        assertEquals(0, counts.regular());
        assertEquals(2, counts.automated());

        assertFalse(counts.showInWorldCount());
    }

    @Test
    void readsCurrentValuesWithoutCaching() {

        Map<String, Integer> regular = new HashMap<>();

        regular.put("qbread", 2);

        UiProductionQueueCounts before = UiProductionQueueCounts.read(
                "qbread",
                regular,
                id -> 0,
                id -> 0);

        regular.put("qbread", 7);

        UiProductionQueueCounts after = UiProductionQueueCounts.read(
                "qbread",
                regular,
                id -> 0,
                id -> 0);

        assertEquals(2, before.regular());
        assertEquals(7, after.regular());
    }

    @Test
    void rejectsNegativeCounts() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new UiProductionQueueCounts(
                        -1, 0, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> new UiProductionQueueCounts(
                        0, -1, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> new UiProductionQueueCounts(
                        0, 0, -1));
    }

    @Test
    void rejectsMissingActionId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> UiProductionQueueCounts.read(
                        "",
                        Map.of(),
                        id -> 0,
                        id -> 0));
    }
}
