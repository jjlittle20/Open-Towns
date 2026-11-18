
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiRoot;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;

class UiScrollContainerTest {

    private static UiScrollContainer createScrollList() {

        UiScrollContainer scroll = new UiScrollContainer();

        scroll.content().setSpacing(5);

        for (int i = 0; i < 5; i++) {
            UiElement item = new UiElement();

            item.setPreferredSize(
                    new UiSize(120, 30));

            scroll.content().add(item);
        }

        return scroll;
    }

    @Test
    void scrollingContentDoesNotExpandViewport() {
        UiScrollContainer scroll = createScrollList();

        UiRoot root = new UiRoot(scroll);
        root.updateLayout(new UiSize(200, 80));

        assertEquals(
                new UiRect(0, 0, 200, 80),
                scroll.bounds());

        // Five 30-unit rows with four 5-unit gaps.
        assertEquals(
                new UiRect(0, 0, 188, 170),
                scroll.content().bounds());

        assertEquals(90, scroll.maxScrollY());
        assertTrue(scroll.clipChildren());
    }

    @Test
    void scrollingMovesContentWithoutMovingViewport() {
        UiScrollContainer scroll = createScrollList();

        UiRoot root = new UiRoot(scroll);
        root.updateLayout(new UiSize(200, 80));

        assertTrue(scroll.scrollBy(35));
        assertTrue(scroll.isLayoutDirty());

        root.updateLayout(new UiSize(200, 80));

        assertEquals(35, scroll.scrollY());

        assertEquals(
                new UiRect(0, 0, 200, 80),
                scroll.bounds());

        assertEquals(
                new UiRect(0, -35, 188, 170),
                scroll.content().bounds());

        // The first item moves with the content.
        assertEquals(
                -35,
                scroll.content().children()
                        .get(0).bounds().y());
    }

    @Test
    void scrollPositionIsClampedToValidRange() {
        UiScrollContainer scroll = createScrollList();

        UiRoot root = new UiRoot(scroll);
        root.updateLayout(new UiSize(200, 80));

        assertTrue(scroll.scrollTo(10000));
        root.updateLayout(new UiSize(200, 80));

        assertEquals(90, scroll.scrollY());

        // Already at the bottom.
        assertFalse(scroll.scrollBy(50));

        assertTrue(scroll.scrollBy(-1000));
        root.updateLayout(new UiSize(200, 80));

        assertEquals(0, scroll.scrollY());

        // Already at the top.
        assertFalse(scroll.scrollBy(-50));
    }

    @Test
    void expandingViewportResetsUnnecessaryScrolling() {
        UiScrollContainer scroll = createScrollList();

        UiRoot root = new UiRoot(scroll);
        root.updateLayout(new UiSize(200, 80));

        scroll.scrollTo(90);
        root.updateLayout(new UiSize(200, 80));

        assertEquals(90, scroll.scrollY());

        // All items now fit inside the viewport.
        root.updateLayout(new UiSize(200, 200));

        assertEquals(0, scroll.maxScrollY());
        assertEquals(0, scroll.scrollY());

        assertEquals(
                new UiRect(0, 0, 200, 200),
                scroll.content().bounds());
    }

    @Test
    void unchangedScrollPositionDoesNotInvalidateLayout() {
        UiScrollContainer scroll = createScrollList();

        UiRoot root = new UiRoot(scroll);
        root.updateLayout(new UiSize(200, 80));

        assertFalse(scroll.scrollTo(0));
        assertFalse(scroll.isLayoutDirty());

        scroll.scrollBy(20);
        assertTrue(root.updateLayout(new UiSize(200, 80)));

        assertFalse(scroll.scrollBy(0));
        assertFalse(root.updateLayout(new UiSize(200, 80)));
    }

    @Test
    void scrollbarThumbReflectsScrollPosition() {
        UiScrollContainer scroll = createScrollList();

        UiRoot root = new UiRoot(scroll);
        root.updateLayout(new UiSize(200, 80));

        UiScrollBar bar = scroll.scrollBar();

        // Twelve units are reserved for the scrollbar.
        assertEquals(
                new UiRect(188, 0, 12, 80),
                bar.bounds());

        // Five rows of 30 with four gaps of 5 = 170.
        assertEquals(90, scroll.maxScrollY());

        // 80 * 80 / 170 = 37, rounded down.
        assertEquals(
                new UiRect(188, 0, 12, 37),
                bar.thumbBounds());

        // Scroll completely to the bottom.
        scroll.scrollTo(90);
        root.updateLayout(new UiSize(200, 80));

        assertEquals(
                new UiRect(188, 43, 12, 37),
                bar.thumbBounds());
    }

    @Test
    void scrollbarDisappearsWhenEverythingFits() {
        UiScrollContainer scroll = createScrollList();

        UiRoot root = new UiRoot(scroll);
        root.updateLayout(new UiSize(200, 80));

        assertEquals(12, scroll.scrollBar().bounds().width());

        // Expand the viewport to fit all the items.
        root.updateLayout(new UiSize(200, 200));

        assertEquals(0, scroll.maxScrollY());
        assertEquals(0, scroll.scrollY());

        assertEquals(
                UiRect.EMPTY.width(),
                scroll.scrollBar().bounds().width());

        assertEquals(
                UiRect.EMPTY.height(),
                scroll.scrollBar().bounds().height());
    }

}
