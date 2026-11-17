
package xaos.ui.rendering;

import java.util.Objects;
import xaos.ui.layout.UiRect;

/**
 * Identifies a region within a texture atlas.
 *
 * The atlas ID is a logical resource name, not an
 * OpenGL texture ID.
 *
 * Source coordinates are measured in image pixels.
 */
public record UiSprite(String atlasId, UiRect source) {

    public UiSprite {
        Objects.requireNonNull(atlasId, "atlasId");
        Objects.requireNonNull(source, "source");

        if (atlasId.isBlank()) {
            throw new IllegalArgumentException(
                    "Atlas ID cannot be blank");
        }

        if (source.x() < 0 || source.y() < 0
                || source.width() <= 0
                || source.height() <= 0) {

            throw new IllegalArgumentException(
                    "Sprite source must have positive dimensions "
                            + "and non-negative coordinates");
        }
    }
}
