
package xaos.ui.layout;

import java.util.Objects;

import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;

/**
 * Distributes a fixed number of toolbar
 * slots evenly across the available width.
 *
 * Only currently visible items should be
 * added to this container.
 *
 * Scroll arrows are separate UI controls.
 */
public final class UiToolbarRowLayout
        implements UiLayout {

    private final int slots;
    private final UiSize buttonSize;

    public UiToolbarRowLayout(
            int slots,
            UiSize buttonSize) {

        if (slots < 1) {
            throw new IllegalArgumentException(
                    "slots must be >= 1");
        }

        this.buttonSize = Objects.requireNonNull(
                buttonSize, "buttonSize");

        if (buttonSize.width() == 0
                || buttonSize.height() == 0) {

            throw new IllegalArgumentException(
                    "buttonSize must be positive");
        }

        this.slots = slots;
    }

    @Override
    public UiSize measure(
            UiContainer container,
            UiSize available) {

        Objects.requireNonNull(
                container, "container");

        Objects.requireNonNull(
                available, "available");

        checkVisibleCount(container);

        UiInsets padding = container.padding();

        int minimumWidth = Math.addExact(
                Math.multiplyExact(
                        slots,
                        buttonSize.width()),
                padding.horizontal());

        int minimumHeight = Math.addExact(
                buttonSize.height(),
                padding.vertical());

        return new UiSize(
                Math.max(
                        minimumWidth,
                        Math.max(
                                container.minimumSize().width(),
                                container.preferredSize().width())),
                Math.max(
                        minimumHeight,
                        Math.max(
                                container.minimumSize().height(),
                                container.preferredSize().height())));
    }

    @Override
    public void layout(
            UiContainer container,
            UiRect bounds) {

        Objects.requireNonNull(
                container, "container");

        Objects.requireNonNull(
                bounds, "bounds");

        checkVisibleCount(container);

        UiInsets padding = container.padding();

        int innerWidth = Math.max(
                0,
                bounds.width() - padding.horizontal());

        int innerHeight = Math.max(
                0,
                bounds.height() - padding.vertical());

        /*
         * Matches the original Towns formula.
         *
         * With 10 slots, 64px icons and 960px:
         *
         * (960 - 10 * 64) / 11 = 29px
         */
        int remaining = Math.max(
                0,
                innerWidth - slots * buttonSize.width());

        int gap = remaining / (slots + 1);

        int x = bounds.x()
                + padding.left()
                + gap;

        int y = bounds.y()
                + padding.top()
                + Math.max(
                        0,
                        (innerHeight - buttonSize.height()) / 2);

        int column = 0;

        for (UiElement child : container.children()) {

            if (!child.visible()) {

                child.layout(
                        new UiRect(x, y, 0, 0));

                continue;
            }

            child.layout(
                    new UiRect(
                            x + column * (buttonSize.width() + gap),
                            y,
                            buttonSize.width(),
                            buttonSize.height()));

            column++;
        }
    }

    private void checkVisibleCount(
            UiContainer container) {

        long visible = container.children()
                .stream()
                .filter(UiElement::visible)
                .count();

        if (visible > slots) {

            throw new IllegalArgumentException(
                    "Toolbar has " + visible
                            + " visible buttons, but only "
                            + slots + " slots");
        }
    }
}
