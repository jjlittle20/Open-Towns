
package xaos.ui.layout;

import java.util.Objects;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;

/**
 * Positions children independently using their requested
 * positions relative to the container's content area.
 *
 * Useful for floating windows, dialogs and overlays.
 */
public final class UiAbsoluteLayout implements UiLayout {

    public static final UiAbsoluteLayout INSTANCE =
            new UiAbsoluteLayout();

    private UiAbsoluteLayout() {}

    // ------------------------------------------------------------
    // Measurement
    // ------------------------------------------------------------

    @Override
    public UiSize measure(UiContainer container, UiSize available) {
        Objects.requireNonNull(container, "container");
        Objects.requireNonNull(available, "available");

        UiInsets padding = container.padding();

        int innerWidth = Math.max(
                0, available.width() - padding.horizontal());

        int innerHeight = Math.max(
                0, available.height() - padding.vertical());

        int contentWidth = 0;
        int contentHeight = 0;

        for (UiElement child : container.children()) {
            if (!child.visible()) {
                continue;
            }

            UiPoint position = child.position();

            UiSize childAvailable = new UiSize(
                    remaining(innerWidth, position.x()),
                    remaining(innerHeight, position.y()));

            UiSize desired = child.measure(childAvailable);

            int right = Math.toIntExact(Math.max(
                    0L, (long) position.x() + desired.width()));

            int bottom = Math.toIntExact(Math.max(
                    0L, (long) position.y() + desired.height()));

            contentWidth = Math.max(contentWidth, right);
            contentHeight = Math.max(contentHeight, bottom);
        }

        int requestedWidth = Math.addExact(
                contentWidth, padding.horizontal());

        int requestedHeight = Math.addExact(
                contentHeight, padding.vertical());

        return new UiSize(
                Math.max(
                        container.minimumSize().width(),
                        Math.max(
                                container.preferredSize().width(),
                                requestedWidth)),
                Math.max(
                        container.minimumSize().height(),
                        Math.max(
                                container.preferredSize().height(),
                                requestedHeight)));
    }

    // ------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------

    @Override
    public void layout(UiContainer container, UiRect bounds) {
        Objects.requireNonNull(container, "container");
        Objects.requireNonNull(bounds, "bounds");

        UiInsets padding = container.padding();

        int originX = Math.addExact(
                bounds.x(), padding.left());

        int originY = Math.addExact(
                bounds.y(), padding.top());

        int innerWidth = Math.max(
                0, bounds.width() - padding.horizontal());

        int innerHeight = Math.max(
                0, bounds.height() - padding.vertical());

        for (UiElement child : container.children()) {
            UiPoint position = child.position();

            int childX = Math.addExact(
                    originX, position.x());

            int childY = Math.addExact(
                    originY, position.y());

            if (!child.visible()) {
                child.layout(new UiRect(
                        childX, childY, 0, 0));
                continue;
            }

            UiSize available = new UiSize(
                    remaining(innerWidth, position.x()),
                    remaining(innerHeight, position.y()));

            UiSize desired = child.measure(available);

            int width = child.horizontalSizePolicy()
                    == UiSizePolicy.FILL
                            ? Math.max(
                                    desired.width(), available.width())
                            : desired.width();

            int height = child.verticalSizePolicy()
                    == UiSizePolicy.FILL
                            ? Math.max(
                                    desired.height(), available.height())
                            : desired.height();

            child.layout(new UiRect(
                    childX, childY, width, height));
        }
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static int remaining(int total, int offset) {
        long remaining = (long) total - offset;

        return (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(0L, remaining));
    }
}
