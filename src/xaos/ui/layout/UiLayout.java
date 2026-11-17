
package xaos.ui.layout;

import xaos.ui.core.UiContainer;

/**
 * Defines how a container measures and positions its children.
 *
 * Layout implementations must not depend on rendering or
 * game-specific logic.
 */
public interface UiLayout {

    /**
     * Calculates the desired size of a container.
     */
    UiSize measure(UiContainer container, UiSize available);

    /**
     * Positions the children within the container's bounds.
     */
    void layout(UiContainer container, UiRect bounds);
}
