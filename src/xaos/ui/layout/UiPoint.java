
package xaos.ui.layout;

/**
 * Represents a position in logical UI coordinates.
 *
 * Negative coordinates are allowed so elements can
 * be positioned partially outside their parent.
 */
public record UiPoint(int x, int y) {

    public static final UiPoint ZERO = new UiPoint(0, 0);

    public static UiPoint of(int x, int y) {
        return new UiPoint(x, y);
    }
}
