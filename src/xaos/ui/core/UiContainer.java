
package xaos.ui.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiLayout;
import xaos.ui.layout.UiLinearLayout;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;

/**
 * Holds UI elements and delegates their measurement
 * and positioning to a layout strategy.
 *
 * Uses vertical stacking by default.
 */
public class UiContainer extends UiElement {

    private final List<UiElement> children = new ArrayList<>();

    private final List<UiElement> childrenView = Collections.unmodifiableList(children);

    private UiInsets padding = UiInsets.NONE;
    private int spacing;

    private UiLayout layoutStrategy = UiLinearLayout.VERTICAL;

    // ------------------------------------------------------------
    // Children
    // ------------------------------------------------------------

    public List<UiElement> children() {
        return childrenView;
    }

    public void add(UiElement child) {
        Objects.requireNonNull(child, "child");

        if (child.parent() != null) {
            throw new IllegalArgumentException(
                    "Element already belongs to a container");
        }

        for (UiContainer ancestor = this; ancestor != null; ancestor = ancestor.parent()) {

            if (ancestor == child) {
                throw new IllegalArgumentException(
                        "A container cannot contain itself or an ancestor");
            }
        }

        children.add(child);
        child.setParent(this);
        invalidateLayout();
    }

    public boolean remove(UiElement child) {
        if (!children.remove(child)) {
            return false;
        }

        child.setParent(null);
        child.invalidateLayout();

        invalidateLayout();
        return true;
    }

    public void clear() {
        if (children.isEmpty()) {
            return;
        }

        for (UiElement child : children) {
            child.setParent(null);
            child.invalidateLayout();
        }

        children.clear();
        invalidateLayout();
    }

    /**
     * Moves a child to the end of the stacking order.
     *
     * Returns true if the order changed.
     *
     * Rendering and input routing will use this order later.
     */
    public boolean bringToFront(UiElement child) {
        if (child == null || child.parent() != this) {
            return false;
        }

        if (children.get(children.size() - 1) == child) {
            return false;
        }

        children.remove(child);
        children.add(child);

        invalidateLayout();
        return true;
    }

    // ------------------------------------------------------------
    // Padding and spacing
    // ------------------------------------------------------------

    public UiInsets padding() {
        return padding;
    }

    public void setPadding(UiInsets padding) {
        UiInsets newPadding = Objects.requireNonNull(
                padding, "padding");

        if (!this.padding.equals(newPadding)) {
            this.padding = newPadding;
            invalidateLayout();
        }
    }

    public int spacing() {
        return spacing;
    }

    public void setSpacing(int spacing) {
        if (spacing < 0) {
            throw new IllegalArgumentException(
                    "Spacing cannot be negative");
        }

        if (this.spacing != spacing) {
            this.spacing = spacing;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Layout strategy
    // ------------------------------------------------------------

    public UiLayout layoutStrategy() {
        return layoutStrategy;
    }

    public void setLayoutStrategy(UiLayout layoutStrategy) {
        UiLayout newLayout = Objects.requireNonNull(
                layoutStrategy, "layoutStrategy");

        if (this.layoutStrategy != newLayout) {
            this.layoutStrategy = newLayout;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Measurement
    // ------------------------------------------------------------

    @Override
    public UiSize measure(UiSize available) {
        Objects.requireNonNull(available, "available");

        if (!visible()) {
            return UiSize.ZERO;
        }

        return layoutStrategy.measure(this, available);
    }

    // ------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------

    @Override
    public void layout(UiRect rectangle) {
        super.layout(rectangle);

        layoutStrategy.layout(this, rectangle);
    }
}
