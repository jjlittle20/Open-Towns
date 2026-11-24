
package xaos.ui.layout;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;

class UiToolbarLayoutTest {

    private static UiContainer toolbar(
            int count) {

        UiContainer container = new UiContainer();

        container.setLayoutStrategy(
                new UiToolbarRowLayout(
                        10,
                        new UiSize(64, 64)));

        for (int i = 0; i < count; i++) {
            container.add(new UiElement());
        }

        return container;
    }

    @Test
    void scrollMovesOneItemAtATime() {

        UiToolbarPager pager = new UiToolbarPager(10);

        List<Integer> items = IntStream.range(0, 12)
                .boxed()
                .toList();

        assertEquals(
                IntStream.range(0, 10).boxed().toList(),
                pager.visibleItems(items));

        assertFalse(pager.canScrollLeft());
        assertTrue(pager.canScrollRight());

        assertTrue(pager.scrollRight());

        assertEquals(
                IntStream.range(1, 11).boxed().toList(),
                pager.visibleItems(items));

        assertTrue(pager.scrollRight());

        assertEquals(
                IntStream.range(2, 12).boxed().toList(),
                pager.visibleItems(items));

        // No scrolling beyond the final item.
        assertFalse(pager.scrollRight());

        assertTrue(pager.scrollLeft());
        assertEquals(1, pager.firstIndex());
    }

    @Test
    void shrinkingMenuClampsScrollPosition() {

        UiToolbarPager pager = new UiToolbarPager(10);

        pager.setItemCount(12);

        pager.scrollRight();
        pager.scrollRight();

        assertEquals(2, pager.firstIndex());

        // Simulate a mod unregistering buttons.
        List<Integer> smaller = List.of(0, 1, 2, 3, 4);

        assertEquals(
                smaller,
                pager.visibleItems(smaller));

        assertEquals(0, pager.firstIndex());

        assertFalse(pager.canScrollLeft());
        assertFalse(pager.canScrollRight());
    }

    @Test
    void toolbarPositionsMatchClassicTowns() {

        UiContainer toolbar = toolbar(10);

        assertEquals(
                new UiSize(640, 64),
                toolbar.measure(
                        new UiSize(960, 64)));

        toolbar.layout(
                new UiRect(100, 300, 960, 64));

        List<UiElement> buttons = toolbar.children();

        // 29px leading gap.
        assertEquals(
                new UiRect(129, 300, 64, 64),
                buttons.get(0).bounds());

        // 64px icon + 29px gap.
        assertEquals(
                new UiRect(222, 300, 64, 64),
                buttons.get(1).bounds());

        assertEquals(
                new UiRect(966, 300, 64, 64),
                buttons.get(9).bounds());
    }

    @Test
    void fewerButtonsKeepOriginalSlotPositions() {

        UiContainer toolbar = toolbar(3);

        toolbar.layout(
                new UiRect(0, 0, 960, 64));

        assertEquals(
                new UiRect(29, 0, 64, 64),
                toolbar.children().get(0).bounds());

        assertEquals(
                new UiRect(122, 0, 64, 64),
                toolbar.children().get(1).bounds());

        assertEquals(
                new UiRect(215, 0, 64, 64),
                toolbar.children().get(2).bounds());
    }

    @Test
    void hiddenItemsDoNotOccupySlots() {

        UiContainer toolbar = toolbar(3);

        UiElement hidden = toolbar.children().get(1);

        hidden.setVisible(false);

        toolbar.layout(
                new UiRect(0, 0, 960, 64));

        assertEquals(
                new UiRect(29, 0, 64, 64),
                toolbar.children().get(0).bounds());

        assertEquals(
                new UiRect(122, 0, 64, 64),
                toolbar.children().get(2).bounds());

        assertEquals(
                0,
                hidden.bounds().width());
    }

    @Test
    void layoutRejectsTooManyVisibleButtons() {

        UiContainer toolbar = toolbar(11);

        assertThrows(
                IllegalArgumentException.class,
                () -> toolbar.layout(
                        new UiRect(0, 0, 960, 64)));
    }

    @Test
    void pagerRejectsInvalidSlotCount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new UiToolbarPager(0));
    }
}
