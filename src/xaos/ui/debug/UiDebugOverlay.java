
package xaos.ui.debug;

import org.lwjgl.opengl.GL11;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiRoot;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiRenderer;
import xaos.ui.rendering.UiSceneRenderer;
import xaos.ui.rendering.opengl.TownsUiRenderer;
import xaos.ui.rendering.opengl.TownsUiTexture;
import xaos.ui.theme.BasicUiTheme;
import xaos.ui.theme.ClassicTownsTheme;
import xaos.ui.theme.UiTheme;
import xaos.utils.TextureData;
import xaos.utils.UtilsGL;

/**
 * Temporary visual test for the new UI framework.
 *
 * This is not part of the public modding API.
 */
public final class UiDebugOverlay {

    public static final boolean ENABLED = true;

    /**
     * Temporary theme selection for development.
     *
     * true  = Original Towns artwork
     * false = Basic white-label theme
     */
    private static final boolean USE_CLASSIC_THEME = true;

    private static final UiTheme THEME =
            USE_CLASSIC_THEME
                    ? new ClassicTownsTheme()
                    : new BasicUiTheme();

    private static final UiWindow WINDOW = createWindow();
    private static final UiRoot ROOT = createRoot();

    private UiDebugOverlay() {
    }

    // ------------------------------------------------------------
    // Window creation
    // ------------------------------------------------------------

    private static UiWindow createWindow() {
        UiWindow window = new UiWindow("New UI Framework");

        window.setPosition(new UiPoint(50, 50));
        window.setPreferredSize(new UiSize(320, 160));

        return window;
    }

    private static UiRoot createRoot() {
        UiContainer desktop = new UiContainer();
        desktop.setLayoutStrategy(UiAbsoluteLayout.INSTANCE);
        desktop.add(WINDOW);

        return new UiRoot(desktop);
    }

    // ------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------

    public static void render() {
        int width = UtilsGL.getWidth();
        int height = UtilsGL.getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        ROOT.updateLayout(new UiSize(width, height));

        // Only load the original artwork when using
        // the classic Towns theme.
        TownsUiTexture atlas = null;

        if (USE_CLASSIC_THEME) {
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

        // Preserve the existing game's projection.
        int previousMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, height, 0, -1, 1);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        try (TownsUiRenderer renderer = new TownsUiRenderer(
                height,
                atlasId -> ClassicTownsTheme.UI2_ATLAS.equals(atlasId)
                        ? resolvedAtlas
                        : null)) {

            UiSceneRenderer.render(
                    ROOT,
                    renderer,
                    UiDebugOverlay::paint);

        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();

            GL11.glMatrixMode(previousMode);
        }
    }

    // ------------------------------------------------------------
    // Element painting
    // ------------------------------------------------------------

    private static void paint(
            UiElement element,
            UiRenderer renderer) {

        if (element == WINDOW) {

            THEME.paintWindow(WINDOW, renderer);

        } else if (element == WINDOW.titleBar()) {

            THEME.paintTitleBar(WINDOW, renderer);

        } else if (element == WINDOW.content()) {

            UiRect bounds = element.bounds();

            renderer.drawText(
                    "New UI renderer is working!",
                    new UiPoint(
                            bounds.x() + 10,
                            bounds.y() + 12),
                    THEME.textColor());
        }
    }
}
