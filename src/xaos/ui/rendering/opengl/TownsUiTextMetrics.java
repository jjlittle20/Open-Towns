
package xaos.ui.rendering.opengl;

import java.util.Objects;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;
import xaos.utils.UtilFont;

/**
 * Measures text using Towns' existing bitmap font.
 */
public final class TownsUiTextMetrics implements UiTextMetrics {

    public static final TownsUiTextMetrics INSTANCE =
            new TownsUiTextMetrics();

    private TownsUiTextMetrics() {}

    @Override
    public UiSize measure(String text) {
        Objects.requireNonNull(text, "text");

        return new UiSize(
                UtilFont.getWidth(text),
                UtilFont.MAX_HEIGHT);
    }
}
