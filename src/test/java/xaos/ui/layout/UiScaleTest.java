
package xaos.ui.layout;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UiScaleTest {

    @Test
    void defaultScalePreservesDimensions() {

        UiScale scale = UiScale.DEFAULT;

        assertEquals(
                new UiSize(960, 64),
                scale.size(new UiSize(960, 64)));

        assertEquals(
                new UiPoint(32, 64),
                scale.point(new UiPoint(32, 64)));
    }

    @Test
    void supportsOriginalTownsScaleSettings() {

        assertEquals(
                new UiSize(720, 48),
                new UiScale(0.75f)
                        .size(new UiSize(960, 64)));

        assertEquals(
                new UiSize(960, 64),
                new UiScale(1.0f)
                        .size(new UiSize(960, 64)));

        assertEquals(
                new UiSize(1200, 80),
                new UiScale(1.25f)
                        .size(new UiSize(960, 64)));

        assertEquals(
                new UiSize(1440, 96),
                new UiScale(1.5f)
                        .size(new UiSize(960, 64)));
    }

    @Test
    void scalesBottomToggleOffset() {

        UiInsets original = new UiInsets(0, 0, 32, 0);

        assertEquals(
                new UiInsets(0, 0, 24, 0),
                new UiScale(0.75f)
                        .insets(original));

        assertEquals(
                new UiInsets(0, 0, 40, 0),
                new UiScale(1.25f)
                        .insets(original));

        assertEquals(
                new UiInsets(0, 0, 48, 0),
                new UiScale(1.5f)
                        .insets(original));
    }

    @Test
    void preservesNegativePositionOffsets() {

        UiScale scale = new UiScale(1.5f);

        assertEquals(
                new UiPoint(-48, 96),
                scale.point(new UiPoint(-32, 64)));
    }

    @Test
    void rejectsInvalidScaleFactors() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new UiScale(0f));

        assertThrows(
                IllegalArgumentException.class,
                () -> new UiScale(-1f));

        assertThrows(
                IllegalArgumentException.class,
                () -> new UiScale(Float.NaN));

        assertThrows(
                IllegalArgumentException.class,
                () -> new UiScale(
                        Float.POSITIVE_INFINITY));
    }

    @Test
    void rejectsIntegerOverflow() {

        UiScale scale = new UiScale(2f);

        assertThrows(
                ArithmeticException.class,
                () -> scale.pixels(
                        Integer.MAX_VALUE));
    }
}
