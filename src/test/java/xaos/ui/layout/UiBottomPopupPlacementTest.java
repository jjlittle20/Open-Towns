
package xaos.ui.layout;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UiBottomPopupPlacementTest {

    @Test
    void stockpilesUseOneRowWhenSpaceAllows() {

        var result = UiBottomPopupPlacement.calculate(
                10,
                220,
                1200,
                800);

        assertEquals(10, result.columns());
        assertEquals(1, result.rows());

        assertEquals(
                new UiRect(220, 688, 816, 96),
                result.bounds());

        assertFalse(result.requiresScrolling());
    }

    @Test
    void zonesWrapIntoAdditionalRows() {

        var result = UiBottomPopupPlacement.calculate(
                16,
                220,
                1200,
                800);

        assertEquals(12, result.columns());
        assertEquals(2, result.rows());

        assertEquals(
                new UiRect(220, 608, 976, 176),
                result.bounds());

        assertFalse(result.requiresScrolling());
    }

    @Test
    void narrowSpaceProducesMoreRows() {

        var result = UiBottomPopupPlacement.calculate(
                16,
                220,
                600,
                700);

        assertEquals(4, result.columns());
        assertEquals(4, result.rows());

        assertEquals(
                new UiRect(220, 348, 336, 336),
                result.bounds());
    }

    @Test
    void shortViewportRequestsScrolling() {

        var result = UiBottomPopupPlacement.calculate(
                16,
                220,
                320,
                200);

        assertEquals(1, result.columns());
        assertEquals(16, result.rows());

        assertTrue(result.requiresScrolling());

        assertEquals(184, result.bounds().height());
        assertEquals(0, result.bounds().y());
    }

    @Test
    void singleEntryStillHasBorderPadding() {

        var result = UiBottomPopupPlacement.calculate(
                1,
                100,
                500,
                500);

        assertEquals(1, result.columns());
        assertEquals(1, result.rows());

        assertEquals(96, result.bounds().width());
        assertEquals(96, result.bounds().height());
    }

    @Test
    void rejectsEmptyPopup() {

        assertThrows(
                IllegalArgumentException.class,
                () -> UiBottomPopupPlacement.calculate(
                        0, 100, 500, 500));
    }
}
