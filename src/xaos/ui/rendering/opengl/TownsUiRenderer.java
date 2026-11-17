
package xaos.ui.rendering.opengl;

import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Function;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import xaos.main.Game;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.rendering.UiColor;
import xaos.ui.rendering.UiRenderer;
import xaos.ui.rendering.UiSprite;
import xaos.utils.CharDef;
import xaos.utils.UtilFont;
import xaos.utils.UtilsGL;

/**
 * OpenGL compatibility renderer for the new UI framework.
 *
 * Use on the game's rendering thread, with an active OpenGL
 * context and top-left, 1:1 UI projection.
 *
 * Close after drawing to restore OpenGL state.
 */
public final class TownsUiRenderer implements UiRenderer, AutoCloseable {

    private final int framebufferHeight;
    private final Function<String, TownsUiTexture> atlasResolver;
    private final Deque<UiRect> clipStack = new ArrayDeque<>();
    private final UiRect inheritedClip;

    private boolean closed;

    public TownsUiRenderer(
            int framebufferHeight,
            Function<String, TownsUiTexture> atlasResolver) {

        if (framebufferHeight < 0) {
            throw new IllegalArgumentException(
                    "Framebuffer height cannot be negative");
        }

        this.framebufferHeight = framebufferHeight;
        this.atlasResolver = Objects.requireNonNull(
                atlasResolver, "atlasResolver");

        // Preserve any existing legacy scissor region.
        if (GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
            IntBuffer box = BufferUtils.createIntBuffer(4);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);

            inheritedClip = new UiRect(
                    box.get(0),
                    Math.toIntExact(
                            (long) framebufferHeight
                                    - box.get(1) - box.get(3)),
                    box.get(2),
                    box.get(3));
        } else {
            inheritedClip = null;
        }

        // Preserve the OpenGL state used by the legacy UI.
        GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT
                        | GL11.GL_CURRENT_BIT
                        | GL11.GL_COLOR_BUFFER_BIT
                        | GL11.GL_TEXTURE_BIT
                        | GL11.GL_SCISSOR_BIT);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ------------------------------------------------------------
    // Rectangles
    // ------------------------------------------------------------

    @Override
    public void fillRect(UiRect bounds, UiColor color) {
        ensureOpen();

        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(color, "color");

        if (bounds.width() == 0 || bounds.height() == 0) {
            return;
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        setColor(color);

        GL11.glBegin(GL11.GL_QUADS);

        GL11.glVertex2i(bounds.x(), bounds.y());

        GL11.glVertex2i(
                Math.addExact(bounds.x(), bounds.width()),
                bounds.y());

        GL11.glVertex2i(
                Math.addExact(bounds.x(), bounds.width()),
                Math.addExact(bounds.y(), bounds.height()));

        GL11.glVertex2i(
                bounds.x(),
                Math.addExact(bounds.y(), bounds.height()));

        GL11.glEnd();
    }

    // ------------------------------------------------------------
    // Text
    // ------------------------------------------------------------

    @Override
    public void drawText(
            String text,
            UiPoint position,
            UiColor color) {

        ensureOpen();

        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(color, "color");

        if (text.isEmpty()) {
            return;
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                Game.TEXTURE_FONT_ID);

        GL11.glTexEnvi(
                GL11.GL_TEXTURE_ENV,
                GL11.GL_TEXTURE_ENV_MODE,
                GL11.GL_MODULATE);

        setColor(color);

        // Uses the existing Towns bitmap font.
        // Draws glyphs directly to preserve colour alpha.
        int x = position.x();

        GL11.glBegin(GL11.GL_QUADS);

        for (int i = 0; i < text.length(); i++) {
            CharDef glyph = UtilFont.getCharDef(text.charAt(i));

            if (glyph == null) {
                continue;
            }

            int y = Math.addExact(
                    position.y(), glyph.yoffset);

            UtilsGL.drawTexture(
                    x,
                    y,
                    Math.addExact(x, glyph.width),
                    Math.addExact(y, glyph.height),
                    glyph.xTex,
                    glyph.yTex,
                    glyph.xTex + glyph.widthTex,
                    glyph.yTex + glyph.heightTex);

            x = Math.addExact(x, glyph.xadvance);
        }

        GL11.glEnd();
    }

    // ------------------------------------------------------------
    // Sprites
    // ------------------------------------------------------------

    @Override
    public void drawSprite(
            UiSprite sprite,
            UiRect destination,
            UiColor tint) {

        ensureOpen();

        Objects.requireNonNull(sprite, "sprite");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(tint, "tint");

        if (destination.width() == 0 || destination.height() == 0) {
            return;
        }

        TownsUiTexture texture = Objects.requireNonNull(
                atlasResolver.apply(sprite.atlasId()),
                "Unknown UI atlas: " + sprite.atlasId());

        texture.validateSource(sprite.source());

        UiRect source = sprite.source();

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                texture.textureId());

        GL11.glTexEnvi(
                GL11.GL_TEXTURE_ENV,
                GL11.GL_TEXTURE_ENV_MODE,
                GL11.GL_MODULATE);

        setColor(tint);

        GL11.glBegin(GL11.GL_QUADS);

        UtilsGL.drawTexture(
                destination.x(),
                destination.y(),
                Math.addExact(
                        destination.x(), destination.width()),
                Math.addExact(
                        destination.y(), destination.height()),
                (float) source.x() / texture.width(),
                (float) source.y() / texture.height(),
                (float) ((long) source.x() + source.width())
                        / texture.width(),
                (float) ((long) source.y() + source.height())
                        / texture.height());

        GL11.glEnd();
    }

    // ------------------------------------------------------------
    // Clipping
    // ------------------------------------------------------------

    @Override
    public void pushClip(UiRect bounds) {
        ensureOpen();
        Objects.requireNonNull(bounds, "bounds");

        UiRect previous = clipStack.isEmpty()
                ? inheritedClip
                : clipStack.peek();

        UiRect applied = previous == null
                ? bounds
                : intersect(previous, bounds);

        clipStack.push(applied);
        applyClip(applied);
    }

    @Override
    public void popClip() {
        ensureOpen();

        if (clipStack.isEmpty()) {
            throw new IllegalStateException(
                    "No UI clipping region to pop");
        }

        clipStack.pop();

        UiRect previous = clipStack.isEmpty()
                ? inheritedClip
                : clipStack.peek();

        if (previous == null) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            applyClip(previous);
        }
    }

    private void applyClip(UiRect bounds) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        GL11.glScissor(
                bounds.x(),
                Math.toIntExact(
                        (long) framebufferHeight
                                - bounds.y() - bounds.height()),
                bounds.width(),
                bounds.height());
    }

    /**
     * Calculates the intersection of two clipping regions.
     */
    static UiRect intersect(UiRect a, UiRect b) {
        long left = Math.max(a.x(), b.x());
        long top = Math.max(a.y(), b.y());

        long right = Math.max(
                left,
                Math.min(
                        (long) a.x() + a.width(),
                        (long) b.x() + b.width()));

        long bottom = Math.max(
                top,
                Math.min(
                        (long) a.y() + a.height(),
                        (long) b.y() + b.height()));

        return new UiRect(
                Math.toIntExact(left),
                Math.toIntExact(top),
                Math.toIntExact(right - left),
                Math.toIntExact(bottom - top));
    }

    // ------------------------------------------------------------
    // Helpers and cleanup
    // ------------------------------------------------------------

    private static void setColor(UiColor color) {
        GL11.glColor4f(
                color.red() / 255f,
                color.green() / 255f,
                color.blue() / 255f,
                color.alpha() / 255f);
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                    "TownsUiRenderer is already closed");
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        boolean unmatchedClips = !clipStack.isEmpty();
        clipStack.clear();

        GL11.glPopAttrib();

        if (unmatchedClips) {
            throw new IllegalStateException(
                    "Unbalanced pushClip/popClip calls");
        }
    }
}
