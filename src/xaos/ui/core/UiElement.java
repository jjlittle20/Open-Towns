
package xaos.ui.core;

import java.util.Objects;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiSizePolicy;
import xaos.ui.layout.UiPoint;

/**
 * Base class for elements in the internal UI component tree.
 *
 * Coordinates and sizes use logical UI units.
 * Rendering and game logic are handled separately.
 */
public class UiElement {

    private UiContainer parent;
    private UiRect bounds = UiRect.EMPTY;
    private UiPoint position = UiPoint.ZERO;

    private UiSize preferredSize = UiSize.ZERO;
    private UiSize minimumSize = UiSize.ZERO;

    private boolean visible = true;

    private UiSizePolicy horizontalSizePolicy = UiSizePolicy.PREFERRED;

    private UiSizePolicy verticalSizePolicy = UiSizePolicy.PREFERRED;

    private boolean layoutDirty = true;

    // ------------------------------------------------------------
    // Parent
    // ------------------------------------------------------------

    public final UiContainer parent() {
        return parent;
    }

    final void setParent(UiContainer parent) {
        this.parent = parent;
    }

    // ------------------------------------------------------------
    // Bounds
    // ------------------------------------------------------------

    public final UiRect bounds() {
        return bounds;
    }

    // ------------------------------------------------------------
    // Position
    // ------------------------------------------------------------

    /**
     * Returns the requested position relative to the parent's
     * content area.
     *
     * Used by absolute layouts. Linear layouts ignore this value.
     */
    public final UiPoint position() {
        return position;
    }

    /**
     * Changes the requested position of this element.
     */
    public final void setPosition(UiPoint position) {
        UiPoint newPosition = Objects.requireNonNull(
                position, "position");

        if (!this.position.equals(newPosition)) {
            this.position = newPosition;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Visibility
    // ------------------------------------------------------------

    public final boolean visible() {
        return visible;
    }

    public final void setVisible(boolean visible) {
        if (this.visible == visible) {
            return;
        }

        this.visible = visible;
        invalidateLayout();
    }

    // ------------------------------------------------------------
    // Preferred size
    // ------------------------------------------------------------

    public final UiSize preferredSize() {
        return preferredSize;
    }

    public final void setPreferredSize(UiSize preferredSize) {
        UiSize newSize = Objects.requireNonNull(
                preferredSize, "preferredSize");

        if (!this.preferredSize.equals(newSize)) {
            this.preferredSize = newSize;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Minimum size
    // ------------------------------------------------------------

    public final UiSize minimumSize() {
        return minimumSize;
    }

    public final void setMinimumSize(UiSize minimumSize) {
        UiSize newSize = Objects.requireNonNull(
                minimumSize, "minimumSize");

        if (!this.minimumSize.equals(newSize)) {
            this.minimumSize = newSize;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Sizing policies
    // ------------------------------------------------------------

    public final UiSizePolicy horizontalSizePolicy() {
        return horizontalSizePolicy;
    }

    public final void setHorizontalSizePolicy(UiSizePolicy policy) {
        UiSizePolicy newPolicy = Objects.requireNonNull(
                policy, "policy");

        if (horizontalSizePolicy != newPolicy) {
            horizontalSizePolicy = newPolicy;
            invalidateLayout();
        }
    }

    public final UiSizePolicy verticalSizePolicy() {
        return verticalSizePolicy;
    }

    public final void setVerticalSizePolicy(UiSizePolicy policy) {
        UiSizePolicy newPolicy = Objects.requireNonNull(
                policy, "policy");

        if (verticalSizePolicy != newPolicy) {
            verticalSizePolicy = newPolicy;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Layout invalidation
    // ------------------------------------------------------------

    /**
     * Returns whether this element requires layout.
     */
    public final boolean isLayoutDirty() {
        return layoutDirty;
    }

    /**
     * Marks this element and all ancestors as needing layout.
     *
     * This does not immediately perform layout.
     */
    public final void invalidateLayout() {
        layoutDirty = true;

        if (parent != null) {
            parent.invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Measurement
    // ------------------------------------------------------------

    public UiSize measure(UiSize available) {
        Objects.requireNonNull(available, "available");

        if (!visible) {
            return UiSize.ZERO;
        }

        return new UiSize(
                Math.max(
                        minimumSize.width(),
                        Math.min(
                                preferredSize.width(),
                                available.width())),
                Math.max(
                        minimumSize.height(),
                        Math.min(
                                preferredSize.height(),
                                available.height())));
    }

    // ------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------

    public void layout(UiRect rectangle) {
        bounds = Objects.requireNonNull(rectangle, "rectangle");

        // The element has now received its layout.
        layoutDirty = false;
    }
}
