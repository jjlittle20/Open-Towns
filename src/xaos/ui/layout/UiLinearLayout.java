
package xaos.ui.layout;

import java.util.Objects;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays out visible children in a vertical or horizontal sequence.
 */
public final class UiLinearLayout implements UiLayout {

    public static final UiLinearLayout VERTICAL = new UiLinearLayout(true);

    public static final UiLinearLayout HORIZONTAL = new UiLinearLayout(false);

    private final boolean vertical;

    private UiLinearLayout(boolean vertical) {
        this.vertical = vertical;
    }

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

        int primary = 0;
        int secondary = 0;
        boolean first = true;

        for (UiElement child : container.children()) {

            if (!child.visible()) {
                continue;
            }

            if (!first) {
                primary = Math.addExact(
                        primary, container.spacing());
            }

            first = false;

            UiSize childAvailable = vertical
                    ? new UiSize(
                            innerWidth,
                            Math.max(0, innerHeight - primary))
                    : new UiSize(
                            Math.max(0, innerWidth - primary),
                            innerHeight);

            UiSize desired = child.measure(childAvailable);

            primary = Math.addExact(
                    primary,
                    vertical ? desired.height() : desired.width());

            secondary = Math.max(
                    secondary,
                    vertical ? desired.width() : desired.height());
        }

        int contentWidth = vertical ? secondary : primary;
        int contentHeight = vertical ? primary : secondary;

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

        int x = Math.addExact(bounds.x(), padding.left());
        int y = Math.addExact(bounds.y(), padding.top());

        int innerWidth = Math.max(
                0, bounds.width() - padding.horizontal());

        int innerHeight = Math.max(
                0, bounds.height() - padding.vertical());

        // ------------------------------------------------------------
        // Pass 1: Measure visible children
        // ------------------------------------------------------------

        List<UiSize> measuredSizes = new ArrayList<>();

        int usedPrimary = 0;
        int fillCount = 0;
        boolean first = true;

        for (UiElement child : container.children()) {
            if (!child.visible()) {
                continue;
            }

            if (!first) {
                usedPrimary = Math.addExact(
                        usedPrimary, container.spacing());
            }

            first = false;

            UiSize available = vertical
                    ? new UiSize(
                            innerWidth,
                            Math.max(0, innerHeight - usedPrimary))
                    : new UiSize(
                            Math.max(0, innerWidth - usedPrimary),
                            innerHeight);

            UiSize desired = child.measure(available);

            measuredSizes.add(desired);

            usedPrimary = Math.addExact(
                    usedPrimary,
                    vertical ? desired.height() : desired.width());

            boolean fillsPrimary = vertical
                    ? child.verticalSizePolicy() == UiSizePolicy.FILL
                    : child.horizontalSizePolicy() == UiSizePolicy.FILL;

            if (fillsPrimary) {
                fillCount++;
            }
        }

        // ------------------------------------------------------------
        // Calculate remaining space
        // ------------------------------------------------------------

        int primarySpace = vertical ? innerHeight : innerWidth;

        int remaining = Math.max(0, primarySpace - usedPrimary);

        int extraPerFill = fillCount == 0
                ? 0
                : remaining / fillCount;

        int extraRemainder = fillCount == 0
                ? 0
                : remaining % fillCount;

        // ------------------------------------------------------------
        // Pass 2: Position children
        // ------------------------------------------------------------

        int index = 0;

        for (UiElement child : container.children()) {

            if (!child.visible()) {
                child.layout(new UiRect(x, y, 0, 0));
                continue;
            }

            if (index > 0) {
                if (vertical) {
                    y = Math.addExact(y, container.spacing());
                } else {
                    x = Math.addExact(x, container.spacing());
                }
            }

            UiSize desired = measuredSizes.get(index);

            boolean fillsPrimary = vertical
                    ? child.verticalSizePolicy() == UiSizePolicy.FILL
                    : child.horizontalSizePolicy() == UiSizePolicy.FILL;

            int extra = 0;

            if (fillsPrimary) {
                extra = extraPerFill;

                if (extraRemainder > 0) {
                    extra++;
                    extraRemainder--;
                }
            }

            int childWidth;
            int childHeight;

            if (vertical) {
                childWidth = child.horizontalSizePolicy() == UiSizePolicy.FILL
                        ? Math.max(innerWidth, desired.width())
                        : desired.width();

                childHeight = Math.addExact(
                        desired.height(), extra);
            } else {
                childWidth = Math.addExact(
                        desired.width(), extra);

                childHeight = child.verticalSizePolicy() == UiSizePolicy.FILL
                        ? Math.max(innerHeight, desired.height())
                        : desired.height();
            }

            child.layout(new UiRect(
                    x, y, childWidth, childHeight));

            if (vertical) {
                y = Math.addExact(y, childHeight);
            } else {
                x = Math.addExact(x, childWidth);
            }

            index++;
        }
    }

}
