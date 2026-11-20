
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiWindowCloseButtonTest {

    private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

    @Test
    void closeButtonUsesStandardDimensions() {

        UiWindow window = new UiWindow("Test");

        window.setCloseAction(FONT, () -> {
        });

        UiWindowCloseButton button = window.closeButton().orElseThrow();

        assertEquals("X", button.text());

        assertEquals(
                new UiSize(20, 20),
                button.minimumSize());

        assertEquals(
                new UiSize(20, 20),
                button.preferredSize());

        assertEquals(
                UiInsets.NONE,
                button.padding());

        assertSame(
                window.titleBar(),
                button.parent());
    }

    @Test
    void clickingCloseButtonRunsCloseAction() {

        UiWindow window = new UiWindow("Test");

        AtomicInteger closes = new AtomicInteger();

        window.setCloseAction(
                FONT,
                closes::incrementAndGet);

        window.closeButton()
                .orElseThrow()
                .activate();

        assertEquals(1, closes.get());
    }

    @Test
    void updatingCloseActionDoesNotDuplicateButton() {

        UiWindow window = new UiWindow("Test");

        AtomicInteger original = new AtomicInteger();
        AtomicInteger replacement = new AtomicInteger();

        window.setCloseAction(
                FONT,
                original::incrementAndGet);

        UiWindowCloseButton button = window.closeButton().orElseThrow();

        int childCount = window.titleBar().children().size();

        // Reconfigure the action.
        window.setCloseAction(
                FONT,
                replacement::incrementAndGet);

        assertSame(
                button,
                window.closeButton().orElseThrow());

        assertEquals(
                childCount,
                window.titleBar().children().size());

        button.activate();

        assertEquals(0, original.get());
        assertEquals(1, replacement.get());
    }

    @Test
    void windowsWithoutCloseActionsHaveNoCloseButton() {

        UiWindow window = new UiWindow("Test");

        assertTrue(window.closeButton().isEmpty());
    }
}
