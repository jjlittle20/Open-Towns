
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiRoot;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiLabelTest {

    // A predictable fake font for unit testing.
    private static final UiTextMetrics TEST_FONT =
            text -> new UiSize(text.length() * 7, 16);

    @Test
    void labelMeasuresUsingFontMetrics() {
        UiLabel label = new UiLabel("Hello", TEST_FONT);

        assertEquals(
                new UiSize(35, 16),
                label.measure(new UiSize(500, 500)));
    }

    @Test
    void changingTextInvalidatesParentLayout() {
        UiContainer container = new UiContainer();
        UiLabel label = new UiLabel("Hi", TEST_FONT);

        container.add(label);

        UiRoot root = new UiRoot(container);
        root.updateLayout(new UiSize(400, 200));

        assertEquals(
                new UiRect(0, 0, 14, 16),
                label.bounds());

        label.setText("Hello");

        assertTrue(label.isLayoutDirty());
        assertTrue(container.isLayoutDirty());

        assertTrue(root.updateLayout(new UiSize(400, 200)));

        assertEquals(
                new UiRect(0, 0, 35, 16),
                label.bounds());

        assertFalse(container.isLayoutDirty());
    }

    @Test
    void unchangedTextDoesNotInvalidateLayout() {
        UiLabel label = new UiLabel("Hello", TEST_FONT);

        label.layout(new UiRect(0, 0, 35, 16));

        label.setText("Hello");

        assertFalse(label.isLayoutDirty());
    }

    @Test
    void availableSpaceLimitsLabelMeasurement() {
        UiLabel label = new UiLabel(
                "A fairly long label",
                TEST_FONT);

        assertEquals(
                new UiSize(40, 10),
                label.measure(new UiSize(40, 10)));
    }

    @Test
    void hiddenLabelsDoNotTakeSpace() {
        UiLabel label = new UiLabel("Hello", TEST_FONT);

        label.setVisible(false);

        assertEquals(
                UiSize.ZERO,
                label.measure(new UiSize(500, 500)));
    }
}
