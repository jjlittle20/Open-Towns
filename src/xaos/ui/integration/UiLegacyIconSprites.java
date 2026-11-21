
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.Function;

import xaos.Towns;
import xaos.property.PropertyFile;

import xaos.ui.layout.UiRect;
import xaos.ui.rendering.UiSprite;

import xaos.ui.integration.UiGameplayMenuResolver.IconReference;

/**
 * Resolves original Towns icon IDs into UiSprites.
 *
 * Uses graphics.ini rather than duplicating
 * texture names or tile positions in menu XML.
 *
 * Does not require an OpenGL context.
 */
public final class UiLegacyIconSprites {

    public static final String ATLAS_PREFIX = "towns-legacy:";

    // Original Towns atlases contain a 16 x 32
    // grid of 64 x 32 pixel cells.
    private static final int CELL_WIDTH = 64;
    private static final int CELL_HEIGHT = 32;

    private static final int ATLAS_WIDTH = 1024;
    private static final int ATLAS_HEIGHT = 1024;

    private final Function<String, String> graphics;

    public UiLegacyIconSprites(
            Function<String, String> graphics) {

        this.graphics = Objects.requireNonNull(
                graphics, "graphics");
    }

    /**
     * Uses the game's currently loaded graphics.ini.
     */
    public static UiLegacyIconSprites fromGraphicsIni() {

        return new UiLegacyIconSprites(
                key -> Towns.getPropertiesString(
                        PropertyFile.PROPERTY_FILE_GRAPHICS,
                        key));
    }

    /**
     * Returns null when the icon is not present
     * in the original graphics definitions.
     */
    public UiSprite resolve(IconReference reference) {

        if (reference == null
                || reference.id() == null
                || reference.id().isBlank()
                || reference.type() == null) {

            return null;
        }

        String prefix = "[" + reference.id() + "]";

        String textureFile = graphics.apply(
                prefix + "TEXTURE_FILE");

        if (!validTextureFile(textureFile)) {
            return null;
        }

        int tileX = propertyInt(
                prefix + "TILE_X", -1);

        int tileY = propertyInt(
                prefix + "TILE_Y", -1);

        int width = propertyInt(
                prefix + "TILE_WIDTH", CELL_WIDTH);

        int height = propertyInt(
                prefix + "TILE_HEIGHT", CELL_HEIGHT);

        if (tileX < 0
                || tileY < 0
                || width <= 0
                || height <= 0) {

            return null;
        }

        long x = (long) tileX * CELL_WIDTH;
        long y = (long) tileY * CELL_HEIGHT;

        // Do not construct a sprite outside
        // the original Towns texture atlas.
        if (x + width > ATLAS_WIDTH
                || y + height > ATLAS_HEIGHT) {

            return null;
        }

        return new UiSprite(
                ATLAS_PREFIX + textureFile,
                new UiRect(
                        (int) x,
                        (int) y,
                        width,
                        height));
    }

    /**
     * Extracts a validated legacy texture filename
     * from a sprite atlas ID.
     *
     * Returns null for non-legacy atlases.
     */
    public static String textureFile(String atlasId) {

        if (atlasId == null
                || !atlasId.startsWith(ATLAS_PREFIX)) {

            return null;
        }

        String file = atlasId.substring(
                ATLAS_PREFIX.length());

        return validTextureFile(file)
                ? file
                : null;
    }

    private int propertyInt(
            String key,
            int fallback) {

        String value = graphics.apply(key);

        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Integer.parseInt(value.trim());

        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean validTextureFile(
            String filename) {

        return filename != null
                && filename.matches(
                        "[A-Za-z0-9_-]+\\.png");
    }
}
