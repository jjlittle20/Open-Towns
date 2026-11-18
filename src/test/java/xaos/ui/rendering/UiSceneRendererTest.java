
package xaos.ui.rendering;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiRoot;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;

class UiSceneRendererTest {

    private static class RecordingRenderer implements UiRenderer {

        final List<String> commands = new ArrayList<>();

        @Override
        public void fillRect(UiRect bounds, UiColor color) {
            commands.add("rectangle");
        }

        @Override
        public void drawText(
                String text, UiPoint position, UiColor color) {
            commands.add("text:" + text);
        }

        @Override
        public void drawSprite(
                UiSprite sprite, UiRect destination, UiColor tint) {
            commands.add("sprite:" + sprite.atlasId());
        }

        @Override
        public void pushClip(UiRect bounds) {
            commands.add("pushClip");
        }

        @Override
        public void popClip() {
            commands.add("popClip");
        }
    }

    @Test
    void elementsRenderInTreeOrder() {
        UiContainer container = new UiContainer();
        UiElement first = new UiElement();
        UiElement second = new UiElement();

        container.add(first);
        container.add(second);

        UiRoot root = new UiRoot(container);
        root.updateLayout(new UiSize(800, 600));

        List<UiElement> painted = new ArrayList<>();

        UiSceneRenderer.render(
                root,
                new RecordingRenderer(),
                (element, renderer) -> painted.add(element));

        assertEquals(
                List.of(container, first, second),
                painted);
    }

    @Test
    void hiddenElementsAndTheirChildrenAreSkipped() {
        UiContainer rootContainer = new UiContainer();
        UiContainer hidden = new UiContainer();
        UiElement child = new UiElement();

        hidden.add(child);
        hidden.setVisible(false);
        rootContainer.add(hidden);

        UiRoot root = new UiRoot(rootContainer);
        root.updateLayout(new UiSize(800, 600));

        List<UiElement> painted = new ArrayList<>();

        UiSceneRenderer.render(
                root,
                new RecordingRenderer(),
                (element, renderer) -> painted.add(element));

        assertEquals(List.of(rootContainer), painted);
    }

    @Test
    void bringToFrontChangesRenderingOrder() {
        UiContainer container = new UiContainer();

        UiElement first = new UiElement();
        UiElement second = new UiElement();

        container.add(first);
        container.add(second);

        UiRoot root = new UiRoot(container);
        root.updateLayout(new UiSize(800, 600));

        container.bringToFront(first);
        root.updateLayout(new UiSize(800, 600));

        List<UiElement> painted = new ArrayList<>();

        UiSceneRenderer.render(
                root,
                new RecordingRenderer(),
                (element, renderer) -> painted.add(element));

        assertEquals(
                List.of(container, second, first),
                painted);
    }

    @Test
    void painterCanIssueDrawingCommands() {
        UiElement element = new UiElement();
        UiRoot root = new UiRoot(element);

        root.updateLayout(new UiSize(200, 100));

        RecordingRenderer renderer = new RecordingRenderer();

        UiSceneRenderer.render(
                root,
                renderer,
                (current, graphics) -> {
                    graphics.fillRect(
                            current.bounds(), UiColor.BLACK);

                    graphics.drawText(
                            "Hello",
                            new UiPoint(10, 10),
                            UiColor.WHITE);
                });

        assertEquals(
                List.of("rectangle", "text:Hello"),
                renderer.commands);
    }

    @Test
    void renderingValuesAreValidated() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UiColor.rgb(256, 0, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> new UiSprite(
                        "",
                        new UiRect(0, 0, 16, 16)));

        assertEquals(
                UiColor.WHITE,
                UiColor.rgb(255, 255, 255));
    }

    @Test
    void clippedContainerWrapsChildRendering() {
        UiContainer desktop = new UiContainer();

        UiContainer clipped = new UiContainer();
        clipped.setClipChildren(true);
        clipped.setPreferredSize(new UiSize(100, 80));

        UiElement child = new UiElement();
        child.setPreferredSize(new UiSize(50, 30));

        clipped.add(child);
        desktop.add(clipped);

        UiRoot root = new UiRoot(desktop);
        root.updateLayout(new UiSize(400, 300));

        RecordingRenderer renderer = new RecordingRenderer();

        UiSceneRenderer.render(
                root,
                renderer,
                (element, graphics) -> {
                    if (element == clipped) {
                        graphics.drawText(
                                "container",
                                new UiPoint(0, 0),
                                UiColor.WHITE);
                    }

                    if (element == child) {
                        graphics.drawText(
                                "child",
                                new UiPoint(0, 0),
                                UiColor.WHITE);
                    }
                });

        assertEquals(
                List.of(
                        "text:container",
                        "pushClip",
                        "text:child",
                        "popClip"),
                renderer.commands);
    }

    @Test
    void clippingIsRestoredWhenPainterThrows() {
        UiContainer container = new UiContainer();
        container.setClipChildren(true);

        UiElement child = new UiElement();
        container.add(child);

        UiRoot root = new UiRoot(container);
        root.updateLayout(new UiSize(400, 300));

        RecordingRenderer renderer = new RecordingRenderer();

        assertThrows(
                IllegalStateException.class,
                () -> UiSceneRenderer.render(
                        root,
                        renderer,
                        (element, graphics) -> {
                            if (element == child) {
                                throw new IllegalStateException(
                                        "Simulated painting failure");
                            }
                        }));

        assertEquals(
                List.of("pushClip", "popClip"),
                renderer.commands);
    }

}
