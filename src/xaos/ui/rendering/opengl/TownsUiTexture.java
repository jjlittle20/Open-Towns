
package xaos.ui.rendering.opengl;

import java.util.Objects;
import xaos.ui.layout.UiRect;

/** Identifies a texture loaded by Towns and its pixel dimensions. */
public record TownsUiTexture(int textureId, int width, int height) {

    public TownsUiTexture {
        if (textureId <= 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "A texture needs a positive OpenGL ID, width and height");
        }
    }

    /** Prevents sampling outside a texture atlas. */
    public void validateSource(UiRect source) {
        Objects.requireNonNull(source, "source");
        if (source.x() < 0 || source.y() < 0
                || source.width() <= 0 || source.height() <= 0
                || (long) source.x() + source.width() > width
                || (long) source.y() + source.height() > height) {
            throw new IllegalArgumentException(
                    "Sprite is outside its texture atlas");
        }
    }
}
