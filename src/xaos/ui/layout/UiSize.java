
package xaos.ui.layout;

/**
 * Represents the width and height of a UI element.
 *
 * Uses logical UI units rather than physical screen pixels.
 */
public record UiSize(int width, int height) {

    public static final UiSize ZERO = new UiSize(0, 0);

    public UiSize {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "UI size cannot have negative dimensions");
        }
    }

    /**
     * Returns a new size with the specified width and height.
     */
    public static UiSize of(int width, int height) {
        return new UiSize(width, height);
    }
}
