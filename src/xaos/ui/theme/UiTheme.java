
package xaos.ui.theme;

import xaos.ui.components.UiWindow;
import xaos.ui.rendering.UiColor;
import xaos.ui.rendering.UiRenderer;
import xaos.ui.components.UiButton;

/**
 * Defines how UI components are visually presented.
 *
 * Themes do not control layout, input handling or
 * game logic.
 */
public interface UiTheme {

    /**
     * Draws a window's background and borders.
     */
    void paintWindow(UiWindow window, UiRenderer renderer);

    /**
     * Draws a window's title bar.
     */
    void paintTitleBar(UiWindow window, UiRenderer renderer);

    /**
     * Returns the default text colour.
     */
    UiColor textColor();

    /**
     * Colour used to highlight keyboard focus.
     *
     * Themes may override this to provide their
     * own visual appearance.
     */
    default UiColor focusColor() {
        return UiColor.rgb(255, 220, 90);
    }

    /**
     * Draws a button using the theme's appearance.
     *
     * Must account for enabled, hovered, pressed
     * and selected states.
     */
    void paintButton(UiButton button, UiRenderer renderer);

    /**
     * Tooltip colours may be overridden by themes.
     */
    default UiColor tooltipBackgroundColor() {
        return UiColor.rgba(25, 29, 36, 245);
    }

    default UiColor tooltipBorderColor() {
        return UiColor.rgb(125, 135, 150);
    }

    default UiColor tooltipTextColor() {
        return UiColor.WHITE;
    }

}
