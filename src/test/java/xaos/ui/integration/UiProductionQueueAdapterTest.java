
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class UiProductionQueueAdapterTest {

    private static final class RecordingBackend
            implements UiProductionQueueAdapter.Backend {

        final List<UiProductionQueueAdapter.Action> operations = new ArrayList<>();

        final List<UiProductionQueueAdapter.Action> tutorials = new ArrayList<>();

        @Override
        public void execute(UiProductionQueueAdapter.Action action) {
            operations.add(action);
        }

        @Override
        public void tutorial(UiProductionQueueAdapter.Action action) {
            tutorials.add(action);
        }
    }

    @Test
    void normalClickExecutesOnce() {
        var adapter = new UiProductionQueueAdapter();
        var backend = new RecordingBackend();

        adapter.apply(
                UiProductionQueueAdapter.Action.ADD_REGULAR,
                false,
                false,
                backend);

        assertEquals(1, backend.operations.size());
        assertEquals(1, backend.tutorials.size());
    }

    @Test
    void shiftClickExecutesFiveTimes() {
        var adapter = new UiProductionQueueAdapter();
        var backend = new RecordingBackend();

        adapter.apply(
                UiProductionQueueAdapter.Action.REMOVE_REGULAR,
                true,
                false,
                backend);

        assertEquals(5, backend.operations.size());
        assertEquals(1, backend.tutorials.size());
    }

    @Test
    void controlClickExecutesOneHundredTimes() {
        var adapter = new UiProductionQueueAdapter();
        var backend = new RecordingBackend();

        adapter.apply(
                UiProductionQueueAdapter.Action.INCREASE_AUTOMATED,
                false,
                true,
                backend);

        assertEquals(100, backend.operations.size());
        assertEquals(1, backend.tutorials.size());
    }

    @Test
    void controlTakesPriorityOverShift() {
        var adapter = new UiProductionQueueAdapter();
        var backend = new RecordingBackend();

        adapter.apply(
                UiProductionQueueAdapter.Action.DECREASE_AUTOMATED,
                true,
                true,
                backend);

        assertEquals(100, backend.operations.size());
        assertEquals(1, backend.tutorials.size());
    }
}
