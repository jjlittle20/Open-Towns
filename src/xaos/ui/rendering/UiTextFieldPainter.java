
package xaos.ui.rendering;

import java.util.Objects;
import xaos.ui.components.UiTextField;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;

/**
 * Backend-independent rendering for a single-line
 * text field.
 *
 * Draws the background, text, placeholder and caret.
 */
public final class UiTextFieldPainter {

    private static final UiColor BACKGROUND =
            UiColor.rgb(35, 38, 44);

    private static final UiColor DISABLED_BACKGROUND =
            UiColor.rgb(50, 50, 55);

    private static final UiColor BORDER =
            UiColor.rgb(110, 115, 125);

    private static final UiColor PLACEHOLDER =
            UiColor.rgb(155, 155, 160);

    private UiTextFieldPainter() {}

    public static void paint(
            UiTextField field,
            UiRenderer renderer,
            boolean focused,
            UiColor textColor,
            UiColor focusColor) {

        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(textColor, "textColor");
        Objects.requireNonNull(focusColor, "focusColor");

        UiRect bounds = field.bounds();

        if (!field.visible()
                || bounds.width() < 2
                || bounds.height() < 2) {
            return;
        }

        // --------------------------------------------------------
        // Background and border
        // --------------------------------------------------------

        boolean active = focused && field.enabled();

        renderer.fillRect(
                bounds,
                active ? focusColor : BORDER);

        renderer.fillRect(
                new UiRect(
                        bounds.x() + 1,
                        bounds.y() + 1,
                        bounds.width() - 2,
                        bounds.height() - 2),
                field.enabled()
                        ? BACKGROUND
                        : DISABLED_BACKGROUND);

        // --------------------------------------------------------
        // Text viewport
        // --------------------------------------------------------

        UiInsets padding = field.padding();

        int innerWidth = Math.max(
                0, bounds.width() - padding.horizontal());

        int innerHeight = Math.max(
                0, bounds.height() - padding.vertical());

        if (innerWidth == 0 || innerHeight == 0) {
            return;
        }

        UiRect clip = new UiRect(
                bounds.x() + padding.left(),
                bounds.y() + padding.top(),
                innerWidth,
                innerHeight);

        UiSize fontSize =
                field.textMetrics().measure("Mg");

        int textY = clip.y()
                + Math.max(
                        0,
                        (clip.height() - fontSize.height()) / 2);

        // --------------------------------------------------------
        // Draw text and caret inside the field
        // --------------------------------------------------------

        renderer.pushClip(clip);

        try {

            if (!field.text().isEmpty()) {

                renderer.drawText(
                        field.text(),
                        new UiPoint(
                                field.textDrawX(),
                                textY),
                        field.enabled()
                                ? textColor
                                : PLACEHOLDER);

            } else if (!field.placeholder().isEmpty()) {

                renderer.drawText(
                        field.placeholder(),
                        new UiPoint(
                                field.textDrawX(),
                                textY),
                        PLACEHOLDER);
            }

            // Draw a stationary caret while focused.
            // Blinking can be added separately later.
            if (active) {

                int caretHeight = Math.min(
                        fontSize.height(),
                        clip.height());

                int caretX = Math.max(
                        clip.x(),
                        Math.min(
                                field.caretDrawX(),
                                clip.x() + clip.width() - 1));

                if (caretHeight > 0) {
                    renderer.fillRect(
                            new UiRect(
                                    caretX,
                                    textY,
                                    1,
                                    caretHeight),
                            focusColor);
                }
            }

        } finally {
            renderer.popClip();
        }
    }
}
