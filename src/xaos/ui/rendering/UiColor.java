
package xaos.ui.rendering;

/**
 * An immutable RGBA colour.
 *
 * Each channel uses an integer from 0 to 255.
 */
public record UiColor(int red, int green, int blue, int alpha) {

    public static final UiColor WHITE =
            new UiColor(255, 255, 255, 255);

    public static final UiColor BLACK =
            new UiColor(0, 0, 0, 255);

    public static final UiColor TRANSPARENT =
            new UiColor(0, 0, 0, 0);

    public UiColor {
        validate(red);
        validate(green);
        validate(blue);
        validate(alpha);
    }

    public static UiColor rgb(int red, int green, int blue) {
        return new UiColor(red, green, blue, 255);
    }

    public static UiColor rgba(
            int red, int green, int blue, int alpha) {

        return new UiColor(red, green, blue, alpha);
    }

    private static void validate(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(
                    "Colour channels must be between 0 and 255");
        }
    }
}
