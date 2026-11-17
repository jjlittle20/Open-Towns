
package xaos.ui.theme;

import java.util.Objects;
import xaos.ui.components.UiWindow;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.rendering.UiColor;
import xaos.ui.rendering.UiNineSlice;
import xaos.ui.rendering.UiRenderer;
import xaos.ui.rendering.UiSprite;

/**
 * Classic Towns UI theme.
 *
 * Uses sprite definitions from the original Towns
 * ui2.png artwork.
 *
 * Texture loading is handled by the rendering backend.
 */
public final class ClassicTownsTheme implements UiTheme {

    public static final String UI2_ATLAS = "classic-ui2";

    private static final UiNineSlice WINDOW_BACKGROUND =
            new UiNineSlice(
                    ui2(7, 2, 64, 64),
                    ui2(5, 2, 64, 32),
                    ui2(5, 4, 64, 32),
                    ui2(6, 3, 32, 32),
                    ui2(4, 3, 32, 32),
                    ui2(6, 2, 32, 32),
                    ui2(4, 2, 32, 32),
                    ui2(6, 4, 32, 32),
                    ui2(4, 4, 32, 32),
                    32,
                    32);

    private static UiSprite ui2(
            int tileX,
            int tileY,
            int width,
            int height) {

        return new UiSprite(
                UI2_ATLAS,
                new UiRect(
                        tileX * 64,
                        tileY * 32,
                        width,
                        height));
    }

    @Override
    public void paintWindow(
            UiWindow window,
            UiRenderer renderer) {

        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(renderer, "renderer");

        WINDOW_BACKGROUND.paint(
                renderer,
                window.bounds(),
                UiColor.WHITE);
    }

    @Override
    public void paintTitleBar(
            UiWindow window,
            UiRenderer renderer) {

        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(renderer, "renderer");

        UiRect bounds = window.titleBar().bounds();

        renderer.drawText(
                window.title(),
                new UiPoint(
                        Math.addExact(bounds.x(), 8),
                        Math.addExact(bounds.y(), 6)),
                textColor());
    }

    @Override
    public UiColor textColor() {
        return UiColor.WHITE;
    }
}
