
package xaos.ui.components;

import java.util.Objects;
import xaos.ui.core.UiContainer;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiLayout;
import xaos.ui.layout.UiLinearLayout;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiSizePolicy;

/**
 * A vertically scrolling viewport.
 *
 * Items should be added to content(), rather than
 * directly to the scroll container.
 *
 * The viewport remains fixed while its content
 * moves vertically within a clipped region.
 */
public final class UiScrollContainer extends UiContainer {

    private static final UiLayout VIEWPORT_LAYOUT = new ViewportLayout();

    private final UiContainer content = new UiContainer();
    private final UiScrollBar scrollBar = new UiScrollBar(this);

    private int scrollY;
    private int maxScrollY;

    // ------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------

    public UiScrollContainer() {

        setLayoutStrategy(VIEWPORT_LAYOUT);
        setClipChildren(true);

        setHorizontalSizePolicy(UiSizePolicy.FILL);
        setVerticalSizePolicy(UiSizePolicy.FILL);

        content.setLayoutStrategy(
                UiLinearLayout.VERTICAL);

        super.add(content);

        // The scrollbar must be drawn after the content
        // so it appears above scrolling items.
        super.add(scrollBar);
    }

    // ------------------------------------------------------------
    // Content
    // ------------------------------------------------------------

    /**
     * Returns the container holding scrollable items.
     */
    public UiContainer content() {
        return content;
    }

    /**
     * Returns this container's vertical scrollbar.
     */
    public UiScrollBar scrollBar() {
        return scrollBar;
    }

    // ------------------------------------------------------------
    // Scroll position
    // ------------------------------------------------------------

    public int scrollY() {
        return scrollY;
    }

    public int maxScrollY() {
        return maxScrollY;
    }

    /**
     * Scrolls to an absolute vertical offset.
     *
     * The offset is clamped to the current scroll range.
     *
     * @return true if the position changed
     */
    public boolean scrollTo(int requestedY) {

        int next = Math.max(
                0,
                Math.min(requestedY, maxScrollY));

        if (next == scrollY) {
            return false;
        }

        scrollY = next;
        invalidateLayout();

        return true;
    }

    /**
     * Scrolls by the specified number of UI units.
     *
     * Positive values scroll down.
     * Negative values scroll up.
     */
    public boolean scrollBy(int amount) {

        long requested = (long) scrollY + amount;

        return scrollTo((int) Math.max(
                Integer.MIN_VALUE,
                Math.min(Integer.MAX_VALUE, requested)));
    }

    // ------------------------------------------------------------
    // Viewport layout
    // ------------------------------------------------------------

    private static final class ViewportLayout implements UiLayout {

        /**
         * The scroll container measures as a viewport,
         * not as the full height of its contents.
         *
         * This prevents large lists from forcing
         * their parent windows to grow indefinitely.
         */
        @Override
        public UiSize measure(
                UiContainer container,
                UiSize available) {

            Objects.requireNonNull(container, "container");
            Objects.requireNonNull(available, "available");

            return new UiSize(
                    Math.max(
                            container.minimumSize().width(),
                            Math.min(
                                    container.preferredSize().width(),
                                    available.width())),
                    Math.max(
                            container.minimumSize().height(),
                            Math.min(
                                    container.preferredSize().height(),
                                    available.height())));
        }

        @Override
        public void layout(
                UiContainer container,
                UiRect bounds) {

            UiScrollContainer scroll = (UiScrollContainer) container;

            UiInsets padding = scroll.padding();

            int viewportWidth = Math.max(
                    0,
                    bounds.width() - padding.horizontal());

            int viewportHeight = Math.max(
                    0,
                    bounds.height() - padding.vertical());

            // ------------------------------------------------------------
            // Measure content before allocating scrollbar space.
            // ------------------------------------------------------------

            UiSize measured = scroll.content.measure(
                    new UiSize(
                            viewportWidth,
                            Integer.MAX_VALUE));

            // Only reserve scrollbar space if content overflows.
            int barWidth = measured.height() > viewportHeight
                    ? Math.min(
                            UiScrollBar.WIDTH,
                            viewportWidth)
                    : 0;

            int contentWidth = viewportWidth - barWidth;

            // Re-measure using the narrower content area
            // if a scrollbar is required.
            if (barWidth > 0) {
                measured = scroll.content.measure(
                        new UiSize(
                                contentWidth,
                                Integer.MAX_VALUE));
            }

            int contentHeight = Math.max(
                    viewportHeight,
                    measured.height());

            // ------------------------------------------------------------
            // Scroll limits
            // ------------------------------------------------------------

            scroll.maxScrollY = contentHeight - viewportHeight;

            // A resized window might no longer need
            // the previous scroll offset.
            scroll.scrollY = Math.min(
                    scroll.scrollY,
                    scroll.maxScrollY);

            // ------------------------------------------------------------
            // Position content
            // ------------------------------------------------------------

            int contentX = Math.addExact(
                    bounds.x(),
                    padding.left());

            int contentY = Math.addExact(
                    bounds.y(),
                    padding.top());

            scroll.content.layout(new UiRect(
                    contentX,
                    Math.subtractExact(
                            contentY, scroll.scrollY),
                    contentWidth,
                    contentHeight));

            // ------------------------------------------------------------
            // Position scrollbar
            // ------------------------------------------------------------

            // The scrollbar occupies the right side of
            // the viewport, outside the content's width.
            //
            // A zero-sized scrollbar is neither drawn
            // nor hit-tested.
            scroll.scrollBar.layout(new UiRect(
                    Math.addExact(contentX, contentWidth),
                    contentY,
                    barWidth,
                    barWidth == 0 ? 0 : viewportHeight));
        }

    }
}
