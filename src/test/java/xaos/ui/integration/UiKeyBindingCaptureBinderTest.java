
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import xaos.ui.components.UiKeyBindingRow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiRoot;
import xaos.ui.input.UiInputRouter;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiKeyBindingCaptureBinderTest {

    private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

    private static final int ESCAPE = 256;
    private static final int KEY_R = 82;

    private static UiInputRouter input() {
        return new UiInputRouter(
                new UiRoot(new UiContainer()));
    }

    private static UiKeyBindingRow row(
            AtomicInteger primary,
            AtomicInteger secondary) {

        return new UiKeyBindingRow(
                "Move Up",
                FONT,
                primary::get,
                secondary::get,
                key -> key == 0
                        ? "Unbound"
                        : "Key " + key,
                slot -> {
                });
    }

    @Test
    void assigningPhysicalKeyUpdatesSelectedSlot() {

        AtomicInteger primary = new AtomicInteger(87);
        AtomicInteger secondary = new AtomicInteger(265);
        AtomicInteger updates = new AtomicInteger();

        UiInputRouter input = input();
        UiKeyBindingRow row = row(primary, secondary);

        UiKeyBindingCaptureBinder.bind(
                row,
                input,
                ESCAPE,
                (slot, key) -> {
                    if (slot == 0) {
                        primary.set(key);
                    } else {
                        secondary.set(key);
                    }
                },
                updates::incrementAndGet);

        // Select the primary binding.
        row.primaryButton().activate();

        assertTrue(input.isCapturingPhysicalKey());
        assertEquals(0, row.capturingSlot());

        assertEquals(
                "Press a key...",
                row.primaryButton().text());

        // Capture R.
        assertTrue(input.physicalKeyEvent(KEY_R, true));

        assertEquals(KEY_R, primary.get());
        assertEquals(265, secondary.get());

        assertFalse(input.isCapturingPhysicalKey());
        assertEquals(-1, row.capturingSlot());

        assertEquals(
                "Primary: Key 82",
                row.primaryButton().text());

        assertEquals(1, updates.get());

        // Release the captured key.
        assertTrue(input.physicalKeyEvent(KEY_R, false));
    }

    @Test
    void escapeCancelsWithoutChangingBindings() {

        AtomicInteger primary = new AtomicInteger(87);
        AtomicInteger secondary = new AtomicInteger(265);
        AtomicInteger updates = new AtomicInteger();

        UiInputRouter input = input();
        UiKeyBindingRow row = row(primary, secondary);

        UiKeyBindingCaptureBinder.bind(
                row,
                input,
                ESCAPE,
                (slot, key) -> secondary.set(key),
                updates::incrementAndGet);

        row.secondaryButton().activate();

        assertEquals(1, row.capturingSlot());
        assertEquals(
                "Press a key...",
                row.secondaryButton().text());

        // Escape cancels the capture.
        assertTrue(input.physicalKeyEvent(
                ESCAPE, true));

        assertFalse(input.isCapturingPhysicalKey());

        assertEquals(-1, row.capturingSlot());
        assertEquals(87, primary.get());
        assertEquals(265, secondary.get());
        assertEquals(0, updates.get());

        assertEquals(
                "Secondary: Key 265",
                row.secondaryButton().text());
    }

    @Test
    void cancellingUiInputRestoresBindingDisplay() {

        AtomicInteger primary = new AtomicInteger(87);
        AtomicInteger secondary = new AtomicInteger(265);

        UiInputRouter input = input();
        UiKeyBindingRow row = row(primary, secondary);

        UiKeyBindingCaptureBinder.bind(
                row,
                input,
                ESCAPE,
                (slot, key) -> primary.set(key),
                row::refresh);

        row.primaryButton().activate();

        assertEquals(
                "Press a key...",
                row.primaryButton().text());

        // Simulates a window or screen closing.
        input.cancelInput();

        assertFalse(input.isCapturingPhysicalKey());
        assertEquals(-1, row.capturingSlot());

        assertEquals(
                "Primary: Key 87",
                row.primaryButton().text());

        assertEquals(87, primary.get());
    }
}
