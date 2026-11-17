
package xaos.ui.theme;

import java.util.Objects;
import xaos.ui.components.UiWindow;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.rendering.UiColor;
import xaos.ui.rendering.UiRenderer;
import xaos.ui.components.UiButton;
import xaos.ui.rendering.UiButtonPainter;

/**
 * Basic artwork-independent UI theme.
 *
 * Uses simple coloured rectangles rather than
 * sprites from the original Towns artwork.
 *
 * Intended as a fallback and starting point for
 * future openly licensed UI themes.
 */
public final class BasicUiTheme implements UiTheme {

    private static final UiColor BACKGROUND = UiColor.rgb(45, 48, 55);

    private static final UiColor BORDER = UiColor.rgb(110, 115, 125);

    private static final UiColor TITLE_BACKGROUND = UiColor.rgb(65, 72, 85);

    private static final UiColor TEXT = UiColor.rgb(240, 240, 240);

    private static final int BORDER_SIZE = 2;

    // ------------------------------------------------------------
    // Window
    // ------------------------------------------------------------

    @Override
    public void paintWindow(
            UiWindow window,
            UiRenderer renderer) {

        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(renderer, "renderer");

        UiRect bounds = window.bounds();

        if (bounds.width() == 0 || bounds.height() == 0) {
            return;
        }

        // Draw the window's outer border.
        renderer.fillRect(bounds, BORDER);

        // Draw the inner background.
        if (bounds.width() > BORDER_SIZE * 2
                && bounds.height() > BORDER_SIZE * 2) {

            renderer.fillRect(
                    new UiRect(
                            bounds.x() + BORDER_SIZE,
                            bounds.y() + BORDER_SIZE,
                            bounds.width() - BORDER_SIZE * 2,
                            bounds.height() - BORDER_SIZE * 2),
                    BACKGROUND);
        }
    }

    // ------------------------------------------------------------
    // Title bar
    // ------------------------------------------------------------

    @Override
    public void paintTitleBar(
            UiWindow window,
            UiRenderer renderer) {

        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(renderer, "renderer");

        UiRect bounds = window.titleBar().bounds();

        renderer.fillRect(bounds, TITLE_BACKGROUND);

        renderer.drawText(
                window.title(),
                new UiPoint(
                        Math.addExact(bounds.x(), 8),
                        Math.addExact(bounds.y(), 6)),
                textColor());
    }

    // ------------------------------------------------------------
    // Text
    // ------------------------------------------------------------

    @Override
    public UiColor textColor() {
        return TEXT;
    }

    @Override
    public void paintButton(
            UiButton button,
            UiRenderer renderer) {

        UiColor background;

        if (!button.enabled()) {
            background = UiColor.rgb(55, 55, 60);
        } else if (button.pressed()) {
            background = UiColor.rgb(35, 90, 135);
        } else if (button.selected()) {
            background = UiColor.rgb(55, 105, 150);
        } else if (button.hovered()) {
            background = UiColor.rgb(85, 95, 115);
        } else {
            background = UiColor.rgb(65, 72, 85);
        }

        UiColor foreground = !button.enabled()
                ? UiColor.rgb(140, 140, 145)
                : button.pressed()
                        ? UiColor.rgb(205, 225, 255)
                        : button.hovered() || button.selected()
                                ? UiColor.rgb(255, 245, 195)
                                : textColor();

        UiButtonPainter.paint(
                button,
                renderer,
                background,
                UiColor.rgb(115, 120, 130),
                foreground);
    }

}
