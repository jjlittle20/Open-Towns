
package xaos.ui.rendering;

import java.util.ArrayList;
import java.util.List;

import xaos.ui.components.UiButton;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.theme.UiTheme;

/**
 * Renders tooltips over the completed UI scene.
 *
 * Does not participate in component layout or
 * mouse hit testing.
 */
public final class UiTooltipPainter {

    private static final int PADDING = 8;
    private static final int SCREEN_MARGIN = 8;
    private static final int MAX_TEXT_WIDTH = 420;
    private static final int MAX_LINES = 18;

    private UiTooltipPainter() {
    }

    public static void paint(
            UiButton button,
            UiPoint pointer,
            UiSize viewport,
            UiRenderer renderer,
            UiTheme theme) {

        if (button == null
                || button.tooltip() == null
                || button.tooltip().isBlank()
                || button.tooltipMetrics() == null) {

            return;
        }

        if (viewport.width() < 80
                || viewport.height() < 40) {
            return;
        }

        UiTextMetrics metrics = button.tooltipMetrics();

        int maxWidth = Math.min(
                MAX_TEXT_WIDTH,
                viewport.width()
                        - SCREEN_MARGIN * 2
                        - PADDING * 2);

        List<String> lines = wrap(
                button.tooltip(),
                metrics,
                maxWidth);

        int lineHeight = Math.max(
                16,
                metrics.measure("Ag").height() + 4);

        int maxVisibleLines = Math.min(
                MAX_LINES,
                Math.max(
                        1,
                        (viewport.height()
                                - SCREEN_MARGIN * 2
                                - PADDING * 2)
                                / lineHeight));

        if (lines.size() > maxVisibleLines) {

            lines = new ArrayList<>(
                    lines.subList(0, maxVisibleLines));

            lines.set(
                    lines.size() - 1,
                    "...");
        }

        int textWidth = 0;

        for (String line : lines) {
            textWidth = Math.max(
                    textWidth,
                    metrics.measure(line).width());
        }

        int width = Math.min(
                viewport.width() - SCREEN_MARGIN * 2,
                Math.max(
                        80,
                        textWidth + PADDING * 2));

        int height = lines.size() * lineHeight
                + PADDING * 2;

        // Prefer positioning near the pointer,
        // but reverse direction if necessary.
        int x = pointer.x() + 14;
        int y = pointer.y() + 18;

        if (x + width > viewport.width() - SCREEN_MARGIN) {

            x = pointer.x() - width - 12;
        }

        if (y + height > viewport.height() - SCREEN_MARGIN) {

            y = pointer.y() - height - 12;
        }

        x = clamp(
                x,
                SCREEN_MARGIN,
                viewport.width() - width - SCREEN_MARGIN);

        y = clamp(
                y,
                SCREEN_MARGIN,
                viewport.height() - height - SCREEN_MARGIN);

        UiRect bounds = new UiRect(x, y, width, height);

        // Border.
        renderer.fillRect(
                bounds,
                theme.tooltipBorderColor());

        // Background.
        renderer.fillRect(
                new UiRect(
                        x + 1,
                        y + 1,
                        width - 2,
                        height - 2),
                theme.tooltipBackgroundColor());

        // Text.
        for (int i = 0; i < lines.size(); i++) {

            renderer.drawText(
                    lines.get(i),
                    new UiPoint(
                            x + PADDING,
                            y + PADDING
                                    + i * lineHeight),
                    theme.tooltipTextColor());
        }
    }

    private static int clamp(
            int value,
            int min,
            int max) {

        return Math.max(min, Math.min(max, value));
    }

    /**
     * Wraps text using the supplied font's
     * actual measured pixel widths.
     */
    private static List<String> wrap(
            String text,
            UiTextMetrics metrics,
            int maxWidth) {

        List<String> result = new ArrayList<>();

        for (String paragraph : text.split("\\R", -1)) {

            if (paragraph.isBlank()) {
                result.add("");
                continue;
            }

            StringBuilder line = new StringBuilder();

            for (String word : paragraph.trim().split("\\s+")) {

                String candidate = line.isEmpty()
                        ? word
                        : line + " " + word;

                if (metrics.measure(candidate).width() <= maxWidth) {

                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }

                if (!line.isEmpty()) {
                    result.add(line.toString());
                    line.setLength(0);
                }

                // Break exceptionally long tokens,
                // such as unbroken definition IDs.
                for (int i = 0; i < word.length(); i++) {

                    char character = word.charAt(i);

                    String attempted = line.toString() + character;

                    if (!line.isEmpty()
                            && metrics.measure(attempted)
                                    .width() > maxWidth) {

                        result.add(line.toString());
                        line.setLength(0);
                    }

                    line.append(character);
                }
            }

            result.add(line.toString());
        }

        return result;
    }
}
