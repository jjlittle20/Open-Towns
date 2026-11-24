
package xaos.ui.layout;

/**
 * Calculates the size and position of a
 * Classic Towns bottom-toolbar submenu.
 *
 * Uses the original 64px icons and 16px
 * border/spacing from UIPanel.
 *
 * Independent of OpenGL and gameplay.
 */
public final class UiBottomPopupPlacement {

    public static final int ICON_SIZE = 64;
    public static final int SPACING = 16;

    private UiBottomPopupPlacement() {
    }

    public record Result(
            UiRect bounds,
            int columns,
            int rows,
            boolean requiresScrolling) {
    }

    /**
     * Places a submenu above the bottom toolbar.
     *
     * @param itemCount   total entries, including Back
     * @param leftX       desired left edge of the popup
     * @param rightLimitX furthest available right edge
     * @param toolbarTopY top edge of the bottom toolbar
     */
    public static Result calculate(
            int itemCount,
            int leftX,
            int rightLimitX,
            int toolbarTopY) {

        if (itemCount < 1) {
            throw new IllegalArgumentException(
                    "Popup requires at least one item");
        }

        if (leftX < 0
                || rightLimitX <= leftX
                || toolbarTopY < 0) {

            throw new IllegalArgumentException(
                    "Invalid popup placement area");
        }

        int step = ICON_SIZE + SPACING;

        int availableWidth = rightLimitX - leftX;

        // Original Towns calculates columns from
        // the available width, then limits them
        // to the number of actual menu entries.
        int maxColumns = Math.max(
                1,
                (availableWidth - SPACING) / step);

        int columns = Math.min(
                itemCount,
                maxColumns);

        int rows = (itemCount + columns - 1)
                / columns;

        int width = columns * step + SPACING;
        int naturalHeight = rows * step + SPACING;

        // The gap separating the popup from
        // the toolbar is also 16 pixels.
        int availableHeight = Math.max(
                0,
                toolbarTopY - SPACING);

        int height = Math.min(
                naturalHeight,
                availableHeight);

        // Normally aligned with the toolbar's
        // left edge. Shift left only if needed
        // to avoid crossing the right boundary.
        int x = Math.max(
                0,
                Math.min(
                        leftX,
                        rightLimitX - width));

        int y = Math.max(
                0,
                toolbarTopY - SPACING - height);

        return new Result(
                new UiRect(x, y, width, height),
                columns,
                rows,
                height < naturalHeight);
    }
}
