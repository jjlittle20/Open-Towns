
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
import xaos.ui.theme.UiThemeRegistry;
import xaos.utils.TextureData;
import xaos.utils.UtilsGL;
import xaos.ui.components.UiLabel;
import xaos.ui.layout.UiInsets;
import xaos.ui.rendering.opengl.TownsUiTextMetrics;
import xaos.ui.components.UiButton;
import xaos.ui.rendering.opengl.TownsUiTextMetrics;

/**
 * Temporary visual test for the new UI framework.
 *
 * This is not part of the public modding API.
 */
public final class UiDebugOverlay {

    public static final boolean ENABLED = true;

    private static final String CLASSIC_THEME = "classic-towns";
    private static final String BASIC_THEME = "basic";

    // Change this to select the starting theme.
    private static final String STARTING_THEME = CLASSIC_THEME;

    private static final UiThemeRegistry THEMES = createThemes();

    private static final UiWindow WINDOW = createWindow();
    private static final UiRoot ROOT = createRoot();

    private UiDebugOverlay() {
    }

    // ------------------------------------------------------------
    // Theme registration
    // ------------------------------------------------------------

    private static UiThemeRegistry createThemes() {
        UiThemeRegistry registry = new UiThemeRegistry();

        registry.register(
                CLASSIC_THEME,
                new ClassicTownsTheme());

        registry.register(
                BASIC_THEME,
                new BasicUiTheme());

        registry.select(STARTING_THEME);

        return registry;
    }

    /**
     * Allows the debug overlay to change themes.
     *
     * Must be called on the UI/rendering thread.
     */
    public static void setTheme(String themeId) {
        THEMES.select(themeId);
    }

    // ------------------------------------------------------------
    // Window creation
    // ------------------------------------------------------------

    private static UiWindow createWindow() {
        UiWindow window = new UiWindow("New UI Framework");

        window.setPosition(new UiPoint(50, 50));
        window.setPreferredSize(new UiSize(320, 270));

        window.content().setPadding(
                xaos.ui.layout.UiInsets.all(10));

        window.content().setSpacing(6);

        window.content().add(new xaos.ui.components.UiLabel(
                "New UI renderer is working!",
                TownsUiTextMetrics.INSTANCE));

        window.content().add(new xaos.ui.components.UiLabel(
                "Button rendering preview:",
                TownsUiTextMetrics.INSTANCE));

        UiButton normal = new UiButton(
                "Normal Button",
                TownsUiTextMetrics.INSTANCE);

        UiButton selected = new UiButton(
                "Selected Button",
                TownsUiTextMetrics.INSTANCE);

        selected.setSelected(true);

        UiButton disabled = new UiButton(
                "Disabled Button",
                TownsUiTextMetrics.INSTANCE);

        disabled.setEnabled(false);

        window.content().add(normal);
        window.content().add(selected);
        window.content().add(disabled);

        return window;
    }

    private static UiRoot createRoot() {
        UiContainer desktop = new UiContainer();

        desktop.setLayoutStrategy(
                UiAbsoluteLayout.INSTANCE);

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

        TownsUiTexture atlas = null;

        if (CLASSIC_THEME.equals(THEMES.activeId())) {
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

        UiTheme theme = THEMES.activeTheme();

        if (element == WINDOW) {

            theme.paintWindow(WINDOW, renderer);

        } else if (element == WINDOW.titleBar()) {

            theme.paintTitleBar(WINDOW, renderer);

        } else if (element instanceof UiLabel label) {

            UiRect bounds = label.bounds();

            renderer.drawText(
                    label.text(),
                    new UiPoint(bounds.x(), bounds.y()),
                    theme.textColor());
        } else if (element instanceof UiButton button) {
            theme.paintButton(button, renderer);
        }
    }

}
