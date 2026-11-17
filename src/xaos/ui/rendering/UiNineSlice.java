
package xaos.ui.rendering;

import java.util.Objects;
import xaos.ui.layout.UiRect;

/**
 * Draws a resizable panel using nine sprite regions.
 *
 * Corners retain their proportions while the edges
 * and centre stretch to fill the destination.
 */
public record UiNineSlice(
        UiSprite center,
        UiSprite north,
        UiSprite south,
        UiSprite east,
        UiSprite west,
        UiSprite northEast,
        UiSprite northWest,
        UiSprite southEast,
        UiSprite southWest,
        int borderWidth,
        int borderHeight) {

    public UiNineSlice {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(north, "north");
        Objects.requireNonNull(south, "south");
        Objects.requireNonNull(east, "east");
        Objects.requireNonNull(west, "west");
        Objects.requireNonNull(northEast, "northEast");
        Objects.requireNonNull(northWest, "northWest");
        Objects.requireNonNull(southEast, "southEast");
        Objects.requireNonNull(southWest, "southWest");

        if (borderWidth <= 0 || borderHeight <= 0) {
            throw new IllegalArgumentException(
                    "Border dimensions must be positive");
        }
    }

    public void paint(
            UiRenderer renderer,
            UiRect bounds,
            UiColor tint) {

        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(tint, "tint");

        if (bounds.width() == 0 || bounds.height() == 0) {
            return;
        }

        // Reduce borders if the window is unusually small.
        int left = Math.min(borderWidth, bounds.width() / 2);
        int right = Math.min(borderWidth, bounds.width() - left);

        int top = Math.min(borderHeight, bounds.height() / 2);
        int bottom = Math.min(borderHeight, bounds.height() - top);

        int middleWidth = bounds.width() - left - right;
        int middleHeight = bounds.height() - top - bottom;

        int x0 = bounds.x();
        int x1 = Math.addExact(x0, left);
        int x2 = Math.addExact(x1, middleWidth);

        int y0 = bounds.y();
        int y1 = Math.addExact(y0, top);
        int y2 = Math.addExact(y1, middleHeight);

        // Centre.
        draw(renderer, center,
                new UiRect(x1, y1, middleWidth, middleHeight), tint);

        // Edges.
        draw(renderer, north,
                new UiRect(x1, y0, middleWidth, top), tint);

        draw(renderer, south,
                new UiRect(x1, y2, middleWidth, bottom), tint);

        draw(renderer, east,
                new UiRect(x2, y1, right, middleHeight), tint);

        draw(renderer, west,
                new UiRect(x0, y1, left, middleHeight), tint);

        // Corners.
        draw(renderer, northEast,
                new UiRect(x2, y0, right, top), tint);

        draw(renderer, northWest,
                new UiRect(x0, y0, left, top), tint);

        draw(renderer, southEast,
                new UiRect(x2, y2, right, bottom), tint);

        draw(renderer, southWest,
                new UiRect(x0, y2, left, bottom), tint);
    }

    private static void draw(
            UiRenderer renderer,
            UiSprite sprite,
            UiRect bounds,
            UiColor tint) {

        if (bounds.width() > 0 && bounds.height() > 0) {
            renderer.drawSprite(sprite, bounds, tint);
        }
    }
}
