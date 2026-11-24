
package xaos.ui.layout;

import java.util.Objects;

import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;

/**
 * A row-major grid of equally sized cells.
 *
 * Uses the largest measured child dimensions
 * as the size of every cell.
 *
 * Spacing and padding come from UiContainer.
 */
public final class UiGridLayout implements UiLayout {

        private final int columns;
        private final boolean centerHorizontally;

        public UiGridLayout(int columns) {
                this(columns, false);
        }

        public UiGridLayout(
                        int columns,
                        boolean centerHorizontally) {

                if (columns < 1) {
                        throw new IllegalArgumentException(
                                        "columns must be >= 1");
                }

                this.columns = columns;
                this.centerHorizontally = centerHorizontally;
        }

        public boolean centerHorizontally() {
                return centerHorizontally;
        }

        public int columns() {
                return columns;
        }

        @Override
        public UiSize measure(
                        UiContainer container,
                        UiSize available) {

                Objects.requireNonNull(container, "container");
                Objects.requireNonNull(available, "available");

                UiInsets padding = container.padding();

                Cell cell = cellSize(
                                container,
                                Math.max(
                                                0,
                                                available.width()
                                                                - padding.horizontal()));

                int rows = (cell.count + columns - 1)
                                / columns;

                int width = cell.count == 0
                                ? 0
                                : columns * cell.width
                                                + (columns - 1)
                                                                * container.spacing();

                int height = rows == 0
                                ? 0
                                : rows * cell.height
                                                + (rows - 1)
                                                                * container.spacing();

                return new UiSize(
                                Math.max(
                                                container.minimumSize().width(),
                                                Math.max(
                                                                container.preferredSize().width(),
                                                                width + padding.horizontal())),
                                Math.max(
                                                container.minimumSize().height(),
                                                Math.max(
                                                                container.preferredSize().height(),
                                                                height + padding.vertical())));
        }

        @Override
        public void layout(
                        UiContainer container,
                        UiRect bounds) {

                Objects.requireNonNull(container, "container");
                Objects.requireNonNull(bounds, "bounds");

                UiInsets padding = container.padding();

                Cell cell = cellSize(
                                container,
                                Math.max(
                                                0,
                                                bounds.width()
                                                                - padding.horizontal()));

                // Centre the entire grid, not each row separately.
                // This preserves the original two-column layout
                // even when the last row contains one button.
                int innerWidth = Math.max(
                                0,
                                bounds.width() - padding.horizontal());

                int gridWidth = columns * cell.width
                                + (columns - 1)
                                                * container.spacing();

                int offsetX = centerHorizontally
                                ? Math.max(0, (innerWidth - gridWidth) / 2)
                                : 0;

                int index = 0;

                for (UiElement child : container.children()) {

                        if (!child.visible()) {

                                child.layout(new UiRect(
                                                bounds.x(),
                                                bounds.y(),
                                                0,
                                                0));

                                continue;
                        }

                        int column = index % columns;
                        int row = index / columns;

                        int x = bounds.x()
                                        + padding.left()
                                        + offsetX
                                        + column * (cell.width
                                                        + container.spacing());

                        int y = bounds.y() + padding.top()
                                        + row * (cell.height
                                                        + container.spacing());

                        child.layout(new UiRect(
                                        x,
                                        y,
                                        cell.width,
                                        cell.height));

                        index++;
                }
        }

        private static Cell cellSize(
                        UiContainer container,
                        int availableWidth) {

                int width = 0;
                int height = 0;
                int count = 0;

                UiSize childAvailable = new UiSize(
                                availableWidth,
                                Integer.MAX_VALUE);

                for (UiElement child : container.children()) {

                        if (!child.visible()) {
                                continue;
                        }

                        UiSize desired = child.measure(childAvailable);

                        width = Math.max(
                                        width,
                                        desired.width());

                        height = Math.max(
                                        height,
                                        desired.height());

                        count++;
                }

                return new Cell(width, height, count);
        }

        private record Cell(
                        int width,
                        int height,
                        int count) {
        }
}
