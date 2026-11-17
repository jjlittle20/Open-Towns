
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
     * Draws a button using the theme's appearance.
     *
     * Must account for enabled, hovered, pressed
     * and selected states.
     */
    void paintButton(UiButton button, UiRenderer renderer);

}
