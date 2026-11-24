
package xaos.ui.theme;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import xaos.ui.components.UiButton;
import xaos.ui.components.UiButtonAppearance;
import xaos.ui.components.UiWindow;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiColor;
import xaos.ui.rendering.UiRenderer;
import xaos.ui.rendering.UiSprite;

class ClassicTownsThemeTest {

    private static class RecordingRenderer implements UiRenderer {

        final List<UiSprite> sprites = new ArrayList<>();
        final List<String> texts = new ArrayList<>();
        final List<UiRect> spriteDestinations = new ArrayList<>();

        @Override
        public void fillRect(UiRect bounds, UiColor color) {
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

            sprites.add(sprite);
            spriteDestinations.add(destination);
        }

        @Override
        public void pushClip(UiRect bounds) {
        }

        @Override
        public void popClip() {
        }
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

    @Test
    void classicActionButtonUsesOriginalCircle() {

        UiButton button = new UiButton(
                "",
                text -> new UiSize(
                        text.length() * 8, 16));

        button.setAppearance(
                UiButtonAppearance.MENU_ACTION);

        button.setPadding(UiInsets.NONE);

        button.setIcon(
                new UiSprite(
                        "test-icons",
                        new UiRect(0, 0, 32, 32)));

        button.setIconSize(
                new UiSize(32, 32));

        button.layout(
                new UiRect(10, 20, 64, 64));

        RecordingRenderer renderer = new RecordingRenderer();

        new ClassicTownsTheme()
                .paintButton(button, renderer);

        assertEquals(2, renderer.sprites.size());

        // Background must be the original circle
        // from ui.png.
        UiSprite background = renderer.sprites.get(0);

        assertEquals(
                "towns-legacy:ui.png",
                background.atlasId());

        assertEquals(
                new UiRect(0, 320, 64, 64),
                background.source());

        assertEquals(
                new UiRect(10, 20, 64, 64),
                renderer.spriteDestinations.get(0));

        // Icon must render over the background,
        // centred inside the 64x64 button.
        assertEquals(
                "test-icons",
                renderer.sprites.get(1).atlasId());

        assertEquals(
                new UiRect(26, 36, 32, 32),
                renderer.spriteDestinations.get(1));
    }

    @Test
    void classicSubmenuUsesDifferentCircle() {

        UiButton button = new UiButton(
                "",
                text -> new UiSize(0, 16));

        button.setAppearance(
                UiButtonAppearance.MENU_SUBMENU);

        button.layout(
                new UiRect(0, 0, 64, 64));

        RecordingRenderer renderer = new RecordingRenderer();

        new ClassicTownsTheme()
                .paintButton(button, renderer);

        assertEquals(1, renderer.sprites.size());

        assertEquals(
                new UiRect(0, 256, 64, 64),
                renderer.sprites.get(0).source());
    }

    @Test
    void ordinaryButtonsDoNotUseMenuCircles() {

        UiButton button = new UiButton(
                "Settings",
                text -> new UiSize(
                        text.length() * 8, 16));

        button.layout(
                new UiRect(0, 0, 120, 32));

        RecordingRenderer renderer = new RecordingRenderer();

        new ClassicTownsTheme()
                .paintButton(button, renderer);

        assertEquals(
                UiButtonAppearance.STANDARD,
                button.appearance());

        assertTrue(renderer.sprites.isEmpty());

        assertEquals(
                List.of("Settings"),
                renderer.texts);
    }

}
