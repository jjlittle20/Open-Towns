
package xaos.ui.rendering;

import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;

/**
 * Backend-independent drawing operations for the UI.
 *
 * All destination coordinates use logical UI units
 * with the origin at the top-left.
 *
 * Implementations handle scaling, texture binding
 * and the underlying graphics API.
 */
public interface UiRenderer {

    /**
     * Draws a filled rectangle.
     */
    void fillRect(UiRect bounds, UiColor color);

    /**
     * Draws text starting at the supplied top-left position.
     */
    void drawText(
            String text,
            UiPoint position,
            UiColor color);

    /**
     * Draws a sprite into the destination rectangle.
     *
     * Tint can also control the sprite's opacity.
     */
    void drawSprite(
            UiSprite sprite,
            UiRect destination,
            UiColor tint);

    /**
     * Restricts drawing to a rectangular area.
     *
     * Nested clips must intersect with the current clip.
     */
    void pushClip(UiRect bounds);

    /**
     * Restores the previous clipping region.
     */
    void popClip();
}
