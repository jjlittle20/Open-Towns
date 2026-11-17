
package xaos.ui.rendering;

import xaos.ui.core.UiElement;

/**
 * Paints one UI element.
 *
 * Does not paint child elements. Traversal is handled
 * separately by UiSceneRenderer.
 */
@FunctionalInterface
public interface UiElementPainter {

    void paint(UiElement element, UiRenderer renderer);
}
