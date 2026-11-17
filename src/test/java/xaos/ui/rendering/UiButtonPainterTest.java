
package xaos.ui.rendering;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import xaos.ui.components.UiButton;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;

class UiButtonPainterTest {

    private static class RecordingRenderer implements UiRenderer {

        final List<UiRect> rectangles = new ArrayList<>();
        final List<UiRect> sprites = new ArrayList<>();
        final List<UiPoint> textPositions = new ArrayList<>();
        int clipsPushed;
        int clipsPopped;

        @Override
        public void fillRect(UiRect bounds, UiColor color) {
            rectangles.add(bounds);
        }

        @Override
        public void drawText(
                String text, UiPoint position, UiColor color) {
            textPositions.add(position);
        }

        @Override
        public void drawSprite(
                UiSprite sprite, UiRect destination, UiColor tint) {
            sprites.add(destination);
        }

        @Override
        public void pushClip(UiRect bounds) {
            clipsPushed++;
        }

        @Override
        public void popClip() {
            clipsPopped++;
        }
    }

    @Test
    void iconOnlyButtonDoesNotRequireBackground() {
        UiSprite icon = new UiSprite(
                "test",
                new UiRect(0, 0, 16, 16));

        UiButton button = new UiButton(icon);
        button.setPadding(xaos.ui.layout.UiInsets.NONE);
        button.layout(new UiRect(10, 20, 16, 16));

        RecordingRenderer renderer = new RecordingRenderer();

        UiButtonPainter.paint(
                button, renderer,
                UiColor.BLACK, UiColor.BLACK, UiColor.WHITE);

        assertTrue(renderer.rectangles.isEmpty());

        assertEquals(
                List.of(new UiRect(10, 20, 16, 16)),
                renderer.sprites);

        assertEquals(1, renderer.clipsPushed);
        assertEquals(1, renderer.clipsPopped);
    }

    @Test
    void mixedButtonPositionsIconBeforeText() {
        UiSprite icon = new UiSprite(
                "test",
                new UiRect(0, 0, 16, 16));

        UiTextMetrics metrics =
                text -> new UiSize(text.length() * 7, 16);

        UiButton button = new UiButton("Go", icon, metrics);

        button.layout(new UiRect(10, 20, 90, 40));

        RecordingRenderer renderer = new RecordingRenderer();

        UiButtonPainter.paint(
                button, renderer,
                UiColor.BLACK, UiColor.BLACK, UiColor.WHITE);

        assertEquals(
                List.of(new UiRect(37, 32, 16, 16)),
                renderer.sprites);

        assertEquals(
                List.of(new UiPoint(59, 32)),
                renderer.textPositions);
    }
}
