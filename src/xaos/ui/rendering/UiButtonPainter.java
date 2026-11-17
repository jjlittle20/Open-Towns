
package xaos.ui.rendering;

import java.util.Objects;
import xaos.ui.components.UiButton;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;

/**
 * Shared rendering logic for text, icon and mixed buttons.
 *
 * Themes supply colours and can eventually replace this
 * painter with their own sprite-based button artwork.
 */
public final class UiButtonPainter {

    private UiButtonPainter() {}

    public static void paint(
            UiButton button,
            UiRenderer renderer,
            UiColor background,
            UiColor border,
            UiColor foreground) {

        Objects.requireNonNull(button, "button");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(background, "background");
        Objects.requireNonNull(border, "border");
        Objects.requireNonNull(foreground, "foreground");

        UiRect bounds = button.bounds();

        if (!button.visible()
                || bounds.width() == 0
                || bounds.height() == 0) {
            return;
        }

        // Draw the button background when requested.
        if (button.backgroundVisible()) {
            renderer.fillRect(bounds, border);

            if (bounds.width() > 2 && bounds.height() > 2) {
                renderer.fillRect(
                        new UiRect(
                                bounds.x() + 1,
                                bounds.y() + 1,
                                bounds.width() - 2,
                                bounds.height() - 2),
                        background);
            }
        }

        UiInsets padding = button.padding();

        int innerWidth = Math.max(
                0, bounds.width() - padding.horizontal());

        int innerHeight = Math.max(
                0, bounds.height() - padding.vertical());

        UiSize textSize = button.measuredTextSize();

        UiSprite icon = button.icon();

        UiSize iconSize = icon == null
                ? UiSize.ZERO
                : button.iconSize();

        int gap = icon != null && !button.text().isEmpty()
                ? button.contentSpacing()
                : 0;

        int contentWidth = Math.addExact(
                Math.addExact(iconSize.width(), textSize.width()),
                gap);

        int contentX = bounds.x() + padding.left()
                + Math.max(0, (innerWidth - contentWidth) / 2);

        int contentY = bounds.y() + padding.top();

        // Prevent oversized content from drawing outside
        // the button's allocated area.
        renderer.pushClip(bounds);

        try {
            if (icon != null
                    && iconSize.width() > 0
                    && iconSize.height() > 0) {

                int iconY = contentY
                        + Math.max(
                                0,
                                (innerHeight - iconSize.height()) / 2);

                renderer.drawSprite(
                        icon,
                        new UiRect(
                                contentX,
                                iconY,
                                iconSize.width(),
                                iconSize.height()),
                        foreground);
            }

            if (!button.text().isEmpty()) {
                int textX = contentX + iconSize.width() + gap;

                int textY = contentY
                        + Math.max(
                                0,
                                (innerHeight - textSize.height()) / 2);

                renderer.drawText(
                        button.text(),
                        new UiPoint(textX, textY),
                        foreground);
            }
        } finally {
            renderer.popClip();
        }
    }
}
