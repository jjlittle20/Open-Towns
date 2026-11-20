
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiCycleButtonTest {

    private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

    @Test
    void advancingCycleUpdatesDisplayedValue() {

        AtomicInteger autosave = new AtomicInteger(0);

        UiCycleButton cycle = new UiCycleButton(
                "Autosave",
                FONT,
                () -> autosave.get() == 0
                        ? "Disabled"
                        : autosave.get() + " Days",
                () -> autosave.set(
                        (autosave.get() + 1) % 11));

        assertEquals(
                "Autosave: Disabled",
                cycle.text());

        cycle.activate();

        assertEquals(1, autosave.get());
        assertEquals(
                "Autosave: 1 Days",
                cycle.text());

        autosave.set(10);
        cycle.refresh();

        cycle.activate();

        assertEquals(0, autosave.get());
        assertEquals(
                "Autosave: Disabled",
                cycle.text());
    }

    @Test
    void refreshDetectsExternalStateChanges() {

        AtomicInteger difficulty = new AtomicInteger(2);

        String[] values = {
                "Off",
                "Easy",
                "Normal",
                "Hard",
                "Harder",
                "Insane"
        };

        UiCycleButton cycle = new UiCycleButton(
                "Sieges",
                FONT,
                () -> values[difficulty.get()],
                () -> difficulty.set(
                        (difficulty.get() + 1)
                                % values.length));

        assertEquals(
                "Sieges: Normal",
                cycle.text());

        difficulty.set(4);
        cycle.refresh();

        assertEquals(
                "Sieges: Harder",
                cycle.text());

        cycle.activate();

        assertEquals(
                "Sieges: Insane",
                cycle.text());
    }

    @Test
    void disabledCycleCannotAdvance() {

        AtomicInteger value = new AtomicInteger();

        UiCycleButton cycle = new UiCycleButton(
                "Setting",
                FONT,
                () -> Integer.toString(value.get()),
                value::incrementAndGet);

        cycle.setEnabled(false);
        cycle.activate();

        assertEquals(0, value.get());
    }
}
