
package xaos.ui.rendering;

import xaos.ui.components.UiSlider;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;

/**
 * Backend-independent horizontal slider painter.
 *
 * Uses primitive rectangles and text so it works
 * with both the Classic and Basic themes.
 */
public final class UiSliderPainter {

    private UiSliderPainter() {
    }

    public static void paint(
            UiSlider slider,
            UiRenderer renderer,
            UiColor textColor) {

        UiRect bounds = slider.bounds();

        if (bounds.width() <= 0
                || bounds.height() <= 0) {
            return;
        }

        // Current label and value.
        renderer.drawText(
                slider.text(),
                new UiPoint(
                        bounds.x() + 12,
                        bounds.y() + 5),
                textColor);

        // Slider track.
        UiRect track = slider.trackBounds();

        renderer.fillRect(
                track,
                UiColor.rgb(65, 70, 80));

        // Filled portion of the track.
        UiRect thumb = slider.thumbBounds();

        int filledWidth = Math.max(
                0,
                Math.min(
                        track.width(),
                        thumb.x() + thumb.width() / 2
                                - track.x()));

        if (filledWidth > 0) {
            renderer.fillRect(
                    new UiRect(
                            track.x(),
                            track.y(),
                            filledWidth,
                            track.height()),
                    UiColor.rgb(90, 155, 205));
        }

        // Draggable thumb.
        UiColor thumbColor = slider.enabled()
                ? slider.pressed()
                        ? UiColor.rgb(195, 225, 255)
                        : UiColor.rgb(170, 190, 210)
                : UiColor.rgb(100, 105, 115);

        renderer.fillRect(
                thumb,
                thumbColor);
    }
}
