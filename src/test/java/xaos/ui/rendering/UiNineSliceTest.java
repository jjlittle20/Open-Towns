
package xaos.ui.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;

class UiNineSliceTest {

    private static class RecordingRenderer implements UiRenderer {

        final List<UiRect> destinations = new ArrayList<>();

        @Override
        public void fillRect(UiRect bounds, UiColor color) {}

        @Override
        public void drawText(
                String text, UiPoint position, UiColor color) {}

        @Override
        public void drawSprite(
                UiSprite sprite, UiRect destination, UiColor tint) {
            destinations.add(destination);
        }

        @Override
        public void pushClip(UiRect bounds) {}

        @Override
        public void popClip() {}
    }

    @Test
    void nineSlicePositionsAllRegions() {
        UiSprite sprite = new UiSprite(
                "test",
                new UiRect(0, 0, 32, 32));

        UiNineSlice slice = new UiNineSlice(
                sprite, sprite, sprite,
                sprite, sprite, sprite,
                sprite, sprite, sprite,
                32, 32);

        RecordingRenderer renderer = new RecordingRenderer();

        slice.paint(
                renderer,
                new UiRect(10, 20, 300, 180),
                UiColor.WHITE);

        assertEquals(
                List.of(
                        new UiRect(42, 52, 236, 116),
                        new UiRect(42, 20, 236, 32),
                        new UiRect(42, 168, 236, 32),
                        new UiRect(278, 52, 32, 116),
                        new UiRect(10, 52, 32, 116),
                        new UiRect(278, 20, 32, 32),
                        new UiRect(10, 20, 32, 32),
                        new UiRect(278, 168, 32, 32),
                        new UiRect(10, 168, 32, 32)),
                renderer.destinations);
    }
}
