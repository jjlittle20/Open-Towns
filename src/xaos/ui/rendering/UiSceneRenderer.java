
package xaos.ui.rendering;

import java.util.Objects;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiRoot;

/**
 * Traverses the UI component tree in drawing order.
 *
 * Parents are painted before children.
 * Children are painted in their container order,
 * with later children appearing above earlier ones.
 */
public final class UiSceneRenderer {

    private UiSceneRenderer() {}

    /**
     * Paints the visible component tree.
     *
     * UiRoot.updateLayout() should be called before
     * rendering to ensure bounds are current.
     */
    public static void render(
            UiRoot root,
            UiRenderer renderer,
            UiElementPainter painter) {

        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(painter, "painter");

        paintElement(root.content(), renderer, painter);
    }

    private static void paintElement(
            UiElement element,
            UiRenderer renderer,
            UiElementPainter painter) {

        if (!element.visible()) {
            return;
        }

        // Paint this element first.
        painter.paint(element, renderer);

        // Then paint its children in stacking order.
        if (element instanceof UiContainer container) {
            for (UiElement child : container.children()) {
                paintElement(child, renderer, painter);
            }
        }
    }
}
