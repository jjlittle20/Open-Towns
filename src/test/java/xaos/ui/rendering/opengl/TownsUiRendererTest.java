
package xaos.ui.rendering.opengl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import xaos.ui.layout.UiRect;

class TownsUiRendererTest {

    @Test
    void textureSourceMustFitWithinAtlas() {
        TownsUiTexture atlas =
                new TownsUiTexture(12, 1024, 1024);

        assertDoesNotThrow(
                () -> atlas.validateSource(
                        new UiRect(100, 200, 50, 60)));

        assertThrows(
                IllegalArgumentException.class,
                () -> atlas.validateSource(
                        new UiRect(1000, 1000, 50, 60)));

        assertThrows(
                IllegalArgumentException.class,
                () -> atlas.validateSource(
                        new UiRect(-1, 0, 16, 16)));
    }

    @Test
    void invalidTextureMetadataIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TownsUiTexture(0, 1024, 1024));

        assertThrows(
                IllegalArgumentException.class,
                () -> new TownsUiTexture(1, 0, 1024));
    }

    @Test
    void nestedClipsIntersect() {
        assertEquals(
                new UiRect(50, 40, 50, 60),
                TownsUiRenderer.intersect(
                        new UiRect(0, 0, 100, 100),
                        new UiRect(50, 40, 100, 100)));
    }

    @Test
    void nonOverlappingClipsHaveZeroArea() {
        assertEquals(
                new UiRect(150, 150, 0, 0),
                TownsUiRenderer.intersect(
                        new UiRect(0, 0, 100, 100),
                        new UiRect(150, 150, 20, 20)));
    }
}
