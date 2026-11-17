
package xaos.ui.theme;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import xaos.ui.components.UiWindow;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.rendering.UiColor;
import xaos.ui.rendering.UiRenderer;
import xaos.ui.rendering.UiSprite;

class ClassicTownsThemeTest {

    private static class RecordingRenderer implements UiRenderer {

        final List<UiSprite> sprites = new ArrayList<>();
        final List<String> texts = new ArrayList<>();

        @Override
        public void fillRect(UiRect bounds, UiColor color) {}

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

            sprites.add(sprite);
        }

        @Override
        public void pushClip(UiRect bounds) {}

        @Override
        public void popClip() {}
    }

    @Test
    void classicWindowUsesOriginalAtlas() {
        UiWindow window = new UiWindow("Inventory");
        window.layout(new UiRect(50, 50, 320, 160));

        RecordingRenderer renderer = new RecordingRenderer();
        UiTheme theme = new ClassicTownsTheme();

        theme.paintWindow(window, renderer);

        assertEquals(9, renderer.sprites.size());

        for (UiSprite sprite : renderer.sprites) {
            assertEquals(
                    ClassicTownsTheme.UI2_ATLAS,
                    sprite.atlasId());
        }
    }

    @Test
    void classicThemeDrawsWindowTitle() {
        UiWindow window = new UiWindow("Inventory");
        window.layout(new UiRect(50, 50, 320, 160));

        RecordingRenderer renderer = new RecordingRenderer();
        UiTheme theme = new ClassicTownsTheme();

        theme.paintTitleBar(window, renderer);

        assertEquals(List.of("Inventory"), renderer.texts);
        assertEquals(UiColor.WHITE, theme.textColor());
    }
}
