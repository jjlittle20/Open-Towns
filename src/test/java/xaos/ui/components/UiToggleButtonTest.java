
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiToggleButtonTest {

    private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

    @Test
    void toggleDisplaysExternalState() {

        AtomicBoolean fullscreen = new AtomicBoolean(false);

        UiToggleButton toggle = new UiToggleButton(
                "Fullscreen",
                FONT,
                fullscreen::get,
                () -> fullscreen.set(!fullscreen.get()));

        assertFalse(toggle.checked());
        assertFalse(toggle.selected());
        assertEquals("Fullscreen: OFF", toggle.text());

        toggle.activate();

        assertTrue(fullscreen.get());
        assertTrue(toggle.checked());
        assertTrue(toggle.selected());
        assertEquals("Fullscreen: ON", toggle.text());
    }

    @Test
    void toggleDetectsExternalStateChanges() {

        AtomicBoolean setting = new AtomicBoolean(false);

        UiToggleButton toggle = new UiToggleButton(
                "Setting",
                FONT,
                setting::get,
                () -> setting.set(!setting.get()));

        // Simulate a keyboard shortcut or a
        // different menu changing the setting.
        setting.set(true);

        toggle.refresh();

        assertEquals("Setting: ON", toggle.text());
        assertTrue(toggle.checked());

        setting.set(false);

        toggle.refresh();

        assertEquals("Setting: OFF", toggle.text());
        assertFalse(toggle.checked());
    }

    @Test
    void disabledToggleCannotExecuteAction() {

        AtomicBoolean setting = new AtomicBoolean(false);

        AtomicInteger actions = new AtomicInteger();

        UiToggleButton toggle = new UiToggleButton(
                "Setting",
                FONT,
                setting::get,
                () -> {
                    actions.incrementAndGet();
                    setting.set(!setting.get());
                });

        toggle.setEnabled(false);
        toggle.activate();

        assertEquals(0, actions.get());
        assertFalse(setting.get());
    }
}
