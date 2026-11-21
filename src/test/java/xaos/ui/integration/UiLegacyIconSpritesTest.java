
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import xaos.ui.layout.UiRect;
import xaos.ui.rendering.UiSprite;

import xaos.ui.integration.UiGameplayMenuResolver.IconReference;
import xaos.ui.integration.UiGameplayMenuResolver.IconType;

class UiLegacyIconSpritesTest {

    private final Map<String, String> graphics = Map.of(
            "[ui_rwalls]TEXTURE_FILE", "ui.png",
            "[ui_rwalls]TILE_X", "5",
            "[ui_rwalls]TILE_Y", "12",
            "[ui_rwalls]TILE_WIDTH", "64",
            "[ui_rwalls]TILE_HEIGHT", "64",

            "[logwall]TEXTURE_FILE", "walls.png",
            "[logwall]TILE_X", "11",
            "[logwall]TILE_Y", "6",
            "[logwall]TILE_HEIGHT", "64");

    private final UiLegacyIconSprites resolver = new UiLegacyIconSprites(graphics::get);

    @Test
    void resolvesOriginalUiIcons() {

        UiSprite sprite = resolver.resolve(
                new IconReference(
                        "ui_rwalls",
                        IconType.UI));

        assertNotNull(sprite);

        assertEquals(
                "towns-legacy:ui.png",
                sprite.atlasId());

        assertEquals(
                new UiRect(320, 384, 64, 64),
                sprite.source());
    }

    @Test
    void resolvesOriginalItemArtwork() {

        UiSprite sprite = resolver.resolve(
                new IconReference(
                        "logwall",
                        IconType.ITEM));

        assertNotNull(sprite);

        assertEquals(
                "towns-legacy:walls.png",
                sprite.atlasId());

        assertEquals(
                new UiRect(704, 192, 64, 64),
                sprite.source());
    }

    @Test
    void missingIconDoesNotThrow() {

        assertNull(resolver.resolve(
                new IconReference(
                        "missing",
                        IconType.UI)));
    }

    @Test
    void rejectsInvalidTexturePaths() {

        assertNull(
                UiLegacyIconSprites.textureFile(
                        "towns-legacy:../other.png"));

        assertEquals(
                "walls.png",
                UiLegacyIconSprites.textureFile(
                        "towns-legacy:walls.png"));
    }
}
