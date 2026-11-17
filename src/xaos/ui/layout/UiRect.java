
package xaos.ui.layout;

/**
 * Represents a rectangular area in logical UI coordinates.
 *
 * The right and bottom boundaries are exclusive.
 */
public record UiRect(int x, int y, int width, int height) {

    public static final UiRect EMPTY = new UiRect(0, 0, 0, 0);

    public UiRect {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "UI dimensions cannot be negative");
        }
    }

    /**
     * Checks whether a coordinate falls inside this rectangle.
     */
    public boolean contains(int pointX, int pointY) {
        return pointX >= x && pointY >= y
                && (long) pointX < (long) x + width
                && (long) pointY < (long) y + height;
    }

    /**
     * Returns the dimensions of this rectangle.
     */
    public UiSize size() {
        return new UiSize(width, height);
    }
}
