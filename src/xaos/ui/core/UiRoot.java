
package xaos.ui.core;

import java.util.Objects;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;

/**
 * Coordinates layout updates for a UI component tree.
 *
 * The root element occupies the entire logical viewport.
 * Rendering and input handling are managed separately.
 */
public final class UiRoot {

    private final UiElement content;

    private UiSize viewportSize = UiSize.ZERO;
    private boolean hasLayout;

    // ------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------

    public UiRoot(UiElement content) {
        this.content = Objects.requireNonNull(
                content, "content");

        if (content.parent() != null) {
            throw new IllegalArgumentException(
                    "Root content must not have a parent");
        }
    }

    // ------------------------------------------------------------
    // Properties
    // ------------------------------------------------------------

    public UiElement content() {
        return content;
    }

    public UiSize viewportSize() {
        return viewportSize;
    }

    // ------------------------------------------------------------
    // Layout updates
    // ------------------------------------------------------------

    /**
     * Updates layout if the viewport changed or
     * the component tree has been invalidated.
     *
     * @param newViewportSize viewport dimensions in logical UI units
     * @return true if layout was performed
     */
    public boolean updateLayout(UiSize newViewportSize) {
        Objects.requireNonNull(newViewportSize, "newViewportSize");

        boolean viewportChanged =
                !viewportSize.equals(newViewportSize);

        if (hasLayout
                && !viewportChanged
                && !content.isLayoutDirty()) {

            return false;
        }

        viewportSize = newViewportSize;

        content.layout(new UiRect(
                0,
                0,
                viewportSize.width(),
                viewportSize.height()));

        hasLayout = true;

        return true;
    }
}
