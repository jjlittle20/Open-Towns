
package xaos.ui.rendering.opengl;

import java.util.Objects;
import org.lwjgl.opengl.GL11;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiStandardPainter;
import xaos.ui.theme.ClassicTownsTheme;
import xaos.utils.TextureData;
import xaos.utils.UtilsGL;

/**
 * OpenGL presentation adapter for the permanent
 * Open Towns UI runtime.
 *
 * Owns OpenGL setup, but not UI components,
 * window state or input handling.
 *
 * Must be called on the game's rendering thread.
 */
public final class TownsUiPresentation {

    private static final String CLASSIC_THEME =
            "classic-towns";

    private final UiSystem system;
    private final UiStandardPainter painter;

    public TownsUiPresentation(UiSystem system) {

        this.system = Objects.requireNonNull(
                system, "system");

        this.painter = new UiStandardPainter(system);
    }

    /**
     * Renders the UI using the current viewport
     * and the active theme.
     */
    public void render() {

        int width = UtilsGL.getWidth();
        int height = UtilsGL.getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        // Update window layouts and viewport constraints.
        system.updateLayout(new UiSize(width, height));

        // --------------------------------------------------------
        // Resolve original Towns artwork
        // --------------------------------------------------------

        TownsUiTexture atlas = null;

        if (CLASSIC_THEME.equals(
                system.themes().activeId())) {

            TextureData texture = UtilsGL.loadTexture(
                    "ui2.png",
                    GL11.GL_REPLACE);

            if (texture == null) {
                return;
            }

            atlas = new TownsUiTexture(
                    texture.getTextureID(),
                    texture.getWidth(),
                    texture.getHeight());
        }

        final TownsUiTexture resolvedAtlas = atlas;

        // --------------------------------------------------------
        // Set up top-left UI projection
        // --------------------------------------------------------

        int previousMode = GL11.glGetInteger(
                GL11.GL_MATRIX_MODE);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(
                0, width,
                height, 0,
                -1, 1);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        // --------------------------------------------------------
        // Render
        // --------------------------------------------------------

        try (TownsUiRenderer renderer =
                new TownsUiRenderer(
                        height,
                        atlasId ->
                                ClassicTownsTheme.UI2_ATLAS
                                        .equals(atlasId)
                                        ? resolvedAtlas
                                        : null)) {

            system.render(renderer, painter);

        } finally {

            // Restore the original game's matrix state.
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();

            GL11.glMatrixMode(previousMode);
        }
    }
}
