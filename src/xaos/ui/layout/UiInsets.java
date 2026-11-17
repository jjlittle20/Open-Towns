
package xaos.ui.layout;

/**
 * Represents padding around UI content in logical UI units.
 */
public record UiInsets(int top, int right, int bottom, int left) {

    public static final UiInsets NONE = new UiInsets(0, 0, 0, 0);

    public UiInsets {
        if (top < 0 || right < 0 || bottom < 0 || left < 0) {
            throw new IllegalArgumentException(
                    "UI padding cannot be negative");
        }
    }

    /**
     * Creates equal padding on all four sides.
     */
    public static UiInsets all(int amount) {
        return new UiInsets(amount, amount, amount, amount);
    }

    public int horizontal() {
        return Math.addExact(left, right);
    }

    public int vertical() {
        return Math.addExact(top, bottom);
    }
}
