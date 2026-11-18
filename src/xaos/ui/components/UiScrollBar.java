
package xaos.ui.components;

import java.util.Objects;
import xaos.ui.core.UiElement;
import xaos.ui.layout.UiRect;

/**
 * Vertical scrollbar belonging to a UiScrollContainer.
 *
 * Calculates thumb geometry and converts pointer
 * positions into scroll offsets.
 *
 * Drawing and input capture are handled separately.
 */
public final class UiScrollBar extends UiElement {

    public static final int WIDTH = 12;

    private static final int MIN_THUMB_HEIGHT = 18;

    private final UiScrollContainer owner;

    UiScrollBar(UiScrollContainer owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    /**
     * Returns the thumb's current rectangle
     * in absolute UI coordinates.
     */
    public UiRect thumbBounds() {

        UiRect track = bounds();
        int range = owner.maxScrollY();

        if (range <= 0
                || track.width() == 0
                || track.height() == 0) {

            return UiRect.EMPTY;
        }

        // Thumb height represents the visible fraction
        // of the total scrollable content.
        long totalHeight =
                (long) track.height() + range;

        int proportional = (int) (
                (long) track.height() * track.height()
                        / totalHeight);

        int thumbHeight = Math.min(
                track.height(),
                Math.max(MIN_THUMB_HEIGHT, proportional));

        // Available distance the thumb can move.
        int travel = track.height() - thumbHeight;

        int offset = (int) (
                (long) owner.scrollY() * travel / range);

        return new UiRect(
                track.x(),
                track.y() + offset,
                track.width(),
                thumbHeight);
    }

    /**
     * Scrolls to the position represented by a pointer.
     *
     * grabOffsetY is the distance from the thumb's top
     * to the point where the player grabbed it.
     *
     * @return true if the scroll position changed
     */
    public boolean scrollToPointer(
            int pointerY,
            int grabOffsetY) {

        UiRect track = bounds();
        UiRect thumb = thumbBounds();

        int travel = track.height() - thumb.height();
        int range = owner.maxScrollY();

        if (travel <= 0 || range <= 0) {
            return false;
        }

        long position = (long) pointerY
                - track.y()
                - grabOffsetY;

        long clamped = Math.max(
                0L,
                Math.min((long) travel, position));

        int requested = (int) (
                (clamped * range + travel / 2L)
                        / travel);

        return owner.scrollTo(requested);
    }
}
