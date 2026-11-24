
package xaos.ui.layout;

import java.util.List;
import java.util.Objects;

/**
 * One-item-at-a-time paging for a fixed number
 * of visible toolbar slots.
 *
 * Does not depend on rendering or gameplay.
 */
public final class UiToolbarPager {

    private final int visibleSlots;

    private int itemCount;
    private int firstIndex;

    public UiToolbarPager(int visibleSlots) {

        if (visibleSlots < 1) {
            throw new IllegalArgumentException(
                    "visibleSlots must be >= 1");
        }

        this.visibleSlots = visibleSlots;
    }

    public int visibleSlots() {
        return visibleSlots;
    }

    public int firstIndex() {
        return firstIndex;
    }

    public int itemCount() {
        return itemCount;
    }

    public int lastExclusive() {

        return (int) Math.min(
                (long) itemCount,
                (long) firstIndex + visibleSlots);
    }

    public boolean canScrollLeft() {
        return firstIndex > 0;
    }

    public boolean canScrollRight() {
        return firstIndex < maximumFirstIndex();
    }

    public boolean scrollLeft() {

        if (!canScrollLeft()) {
            return false;
        }

        firstIndex--;
        return true;
    }

    public boolean scrollRight() {

        if (!canScrollRight()) {
            return false;
        }

        firstIndex++;
        return true;
    }

    public void reset() {
        firstIndex = 0;
    }

    /**
     * Updates the number of items.
     *
     * If mods remove items, keep the current
     * scroll position within the valid range.
     */
    public void setItemCount(int count) {

        if (count < 0) {
            throw new IllegalArgumentException(
                    "itemCount must be >= 0");
        }

        itemCount = count;

        firstIndex = Math.min(
                firstIndex,
                maximumFirstIndex());
    }

    /**
     * Returns an immutable snapshot of the
     * currently visible menu entries.
     *
     * Also reconciles the count after menu
     * extensions are added or removed.
     */
    public <T> List<T> visibleItems(List<T> items) {

        Objects.requireNonNull(items, "items");

        setItemCount(items.size());

        return List.copyOf(
                items.subList(
                        firstIndex,
                        lastExclusive()));
    }

    private int maximumFirstIndex() {
        return Math.max(
                0,
                itemCount - visibleSlots);
    }
}
