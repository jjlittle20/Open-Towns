
package xaos.ui.theme;

import java.util.Objects;
import xaos.ui.components.UiWindow;
import xaos.ui.components.UiWindowAppearance;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.rendering.UiColor;
import xaos.ui.rendering.UiNineSlice;
import xaos.ui.rendering.UiRenderer;
import xaos.ui.rendering.UiSprite;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiButtonAppearance;
import xaos.ui.rendering.UiButtonPainter;

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

    /**
     * Original Towns gameplay button artwork.
     *
     * These source coordinates match graphics.ini:
     *
     * bottom_item = ui.png (0, 320, 64, 64)
     * bottom_item_sm = ui.png (0, 256, 64, 64)
     *
     * TownsUiPresentation already knows how to
     * resolve towns-legacy texture IDs.
     */
    private static final String LEGACY_UI_ATLAS = "towns-legacy:ui.png";

    private static final UiSprite MENU_ACTION_BACKGROUND = new UiSprite(
            LEGACY_UI_ATLAS,
            new UiRect(0, 320, 64, 64));

    private static final UiSprite MENU_SUBMENU_BACKGROUND = new UiSprite(
            LEGACY_UI_ATLAS,
            new UiRect(0, 256, 64, 64));

    private static final UiNineSlice WINDOW_BACKGROUND = new UiNineSlice(
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

    /**
     * Original Towns bottom-panel background.
     *
     * graphics.ini:
     * [bottom_panel]
     * TILE_X = 2
     * TILE_Y = 0
     * TILE_WIDTH = 736
     * TILE_HEIGHT = 64
     * TEXTURE_FILE = ui.png
     *
     * Legacy atlas cells are 64x32.
     */
    private static final UiSprite BOTTOM_TOOLBAR_BACKGROUND = new UiSprite(
            "towns-legacy:ui.png",
            new UiRect(128, 0, 736, 64));

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

        if (window.appearance() == UiWindowAppearance.BOTTOM_TOOLBAR) {

            UiRect bounds = window.bounds();

            if (bounds.width() > 0
                    && bounds.height() > 0) {

                renderer.drawSprite(
                        BOTTOM_TOOLBAR_BACKGROUND,
                        bounds,
                        UiColor.WHITE);
            }

            return;
        }

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

    @Override
    public void paintButton(
            UiButton button,
            UiRenderer renderer) {

        if (button.appearance() != UiButtonAppearance.STANDARD) {

            UiRect bounds = button.bounds();

            if (!button.visible()
                    || bounds.width() <= 0
                    || bounds.height() <= 0) {
                return;
            }

            // The theme owns the background.
            // Mods only supply their action's icon.
            UiSprite background = button.appearance() == UiButtonAppearance.MENU_SUBMENU
                    ? MENU_SUBMENU_BACKGROUND
                    : MENU_ACTION_BACKGROUND;

            renderer.drawSprite(
                    background,
                    bounds,
                    UiColor.WHITE);

            // The original circle stays visible
            // even when the action is unavailable.
            //
            // Only the content is tinted to reflect
            // the button's interaction state.
            UiColor foreground;

            if (!button.enabled()) {
                foreground = UiColor.rgb(145, 140, 135);

            } else if (button.pressed()) {
                foreground = UiColor.rgb(255, 215, 135);

            } else if (button.hovered()
                    || button.selected()) {
                foreground = UiColor.rgb(255, 240, 190);

            } else {
                foreground = UiColor.WHITE;
            }

            // Draw icon/text without adding a
            // second rectangular background.
            UiButtonPainter.paint(
                    button,
                    renderer,
                    UiColor.BLACK,
                    UiColor.BLACK,
                    foreground,
                    false);

            return;
        }

        UiColor background;

        if (!button.enabled()) {
            background = UiColor.rgb(75, 70, 60);
        } else if (button.pressed()) {
            background = UiColor.rgb(85, 65, 40);
        } else if (button.selected()) {
            background = UiColor.rgb(125, 95, 50);
        } else if (button.hovered()) {
            background = UiColor.rgb(145, 115, 75);
        } else {
            background = UiColor.rgb(110, 85, 55);
        }

        UiColor foreground = !button.enabled()
                ? UiColor.rgb(150, 145, 135)
                : button.hovered() || button.selected()
                        ? UiColor.rgb(255, 230, 160)
                        : textColor();

        UiButtonPainter.paint(
                button,
                renderer,
                background,
                UiColor.rgb(60, 45, 30),
                foreground);
    }

}
