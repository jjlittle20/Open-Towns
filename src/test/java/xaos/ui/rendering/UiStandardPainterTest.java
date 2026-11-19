
package xaos.ui.rendering;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.theme.BasicUiTheme;
import xaos.ui.theme.ClassicTownsTheme;

class UiStandardPainterTest {

    @Test
    void rendersGameWindowWithBothThemes() {

        UiSystem system = new UiSystem();

        system.themes().register(
                "basic",
                new BasicUiTheme());

        system.themes().register(
                "classic-towns",
                new ClassicTownsTheme());

        UiWindow window = new UiWindow("Inventory");

        window.setPreferredSize(new UiSize(300, 200));
        window.setPosition(new UiPoint(50, 50));

        window.content().add(new UiLabel(
                "Iron Ore",
                text -> new UiSize(text.length() * 7, 16)));

        system.windows().register(window);
        system.updateLayout(new UiSize(800, 600));

        UiStandardPainter painter =
                new UiStandardPainter(system);

        // Basic theme uses filled rectangles.
        system.themes().select("basic");

        RecordingRenderer basic =
                new RecordingRenderer();

        system.render(basic, painter);

        assertTrue(basic.filledRectangles > 0);
        assertTrue(basic.texts.contains("Inventory"));
        assertTrue(basic.texts.contains("Iron Ore"));

        // Classic theme uses the Towns sprite atlas.
        system.themes().select("classic-towns");

        RecordingRenderer classic =
                new RecordingRenderer();

        system.render(classic, painter);

        assertTrue(classic.sprites > 0);
        assertTrue(classic.texts.contains("Inventory"));
        assertTrue(classic.texts.contains("Iron Ore"));
    }

    /**
     * Simple renderer for testing without OpenGL.
     */
    private static final class RecordingRenderer
            implements UiRenderer {

        int filledRectangles;
        int sprites;

        final List<String> texts = new ArrayList<>();

        @Override
        public void fillRect(
                UiRect bounds,
                UiColor color) {

            filledRectangles++;
        }

        @Override
        public void drawText(
                String text,
                UiPoint position,
                UiColor color) {

            texts.add(text);
        }

        @Override
        public void drawSprite(
                UiSprite sprite,
                UiRect destination,
                UiColor tint) {

            sprites++;
        }

        @Override
        public void pushClip(UiRect bounds) {
            // No OpenGL context required.
        }

        @Override
        public void popClip() {
            // No OpenGL context required.
        }
    }
}
