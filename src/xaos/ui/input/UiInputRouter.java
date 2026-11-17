
package xaos.ui.input;

import java.util.List;
import java.util.Objects;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiRoot;

/**
 * Routes mouse interactions through the UI component tree.
 *
 * Frontmost elements receive input first.
 *
 * This class is independent of LWJGL and the game engine.
 * Coordinates use logical UI units.
 */
public final class UiInputRouter {

    private final UiRoot root;

    private UiElement pressedTarget;
    private UiButton hoveredButton;

    public UiInputRouter(UiRoot root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    // ------------------------------------------------------------
    // Mouse movement
    // ------------------------------------------------------------

    /**
     * Updates hover and pressed states.
     *
     * Returns true if the pointer is over an interactive
     * UI region or an existing UI press is being captured.
     */
    public boolean mouseMoved(int x, int y) {
        UiElement target = hitTest(x, y);

        updateHovered(target);

        if (pressedTarget instanceof UiButton button) {
            button.setPressed(target == button);
        }

        return target != null || pressedTarget != null;
    }

    // ------------------------------------------------------------
    // Mouse press
    // ------------------------------------------------------------

    /**
     * Begins a mouse interaction.
     *
     * Returns true if the UI consumed the press.
     */
    public boolean mousePressed(int x, int y) {
        clearPressed();

        UiElement target = hitTest(x, y);

        pressedTarget = target;
        updateHovered(target);

        if (target instanceof UiButton button) {
            button.setPressed(true);
        }

        return target != null;
    }

    // ------------------------------------------------------------
    // Mouse release
    // ------------------------------------------------------------

    /**
     * Completes a mouse interaction.
     *
     * A button activates only if the press began on that
     * button and the release occurs over the same button.
     *
     * Returns true if the UI handled either end of
     * the interaction.
     */
    public boolean mouseReleased(int x, int y) {
        UiElement target = hitTest(x, y);
        UiElement originalTarget = pressedTarget;

        pressedTarget = null;

        if (originalTarget instanceof UiButton button) {
            button.setPressed(false);
        }

        updateHovered(target);

        if (originalTarget instanceof UiButton button
                && target == button
                && button.enabled()) {

            button.activate();
        }

        return originalTarget != null || target != null;
    }

    // ------------------------------------------------------------
    // Cancellation
    // ------------------------------------------------------------

    /**
     * Cancels the current pointer interaction.
     *
     * Useful when the game loses focus or changes screens.
     */
    public void cancelPointer() {
        clearPressed();

        if (hoveredButton != null) {
            hoveredButton.setHovered(false);
            hoveredButton = null;
        }
    }

    private void clearPressed() {
        if (pressedTarget instanceof UiButton button) {
            button.setPressed(false);
        }

        pressedTarget = null;
    }

    // ------------------------------------------------------------
    // Hover management
    // ------------------------------------------------------------

    private void updateHovered(UiElement target) {
        UiButton nextButton = target instanceof UiButton button
                ? button
                : null;

        if (hoveredButton != nextButton) {
            if (hoveredButton != null) {
                hoveredButton.setHovered(false);
            }

            hoveredButton = nextButton;
        }

        if (hoveredButton != null) {
            hoveredButton.setHovered(true);
        }
    }

    // ------------------------------------------------------------
    // Hit testing
    // ------------------------------------------------------------

    private UiElement hitTest(int x, int y) {
        return hitTest(root.content(), x, y);
    }

    /**
     * Finds the frontmost interactive element at a position.
     */
    private UiElement hitTest(
            UiElement element,
            int x,
            int y) {

        if (!element.visible()
                || !element.bounds().contains(x, y)) {
            return null;
        }

        // Search children in reverse drawing order.
        if (element instanceof UiContainer container) {
            List<UiElement> children = container.children();

            for (int i = children.size() - 1; i >= 0; i--) {
                UiElement target = hitTest(
                        children.get(i), x, y);

                if (target != null) {
                    return target;
                }
            }
        }

        // Buttons receive direct interaction.
        if (element instanceof UiButton) {
            return element;
        }

        // A window blocks clicks from reaching
        // windows and game elements behind it.
        if (element instanceof UiWindow) {
            return element;
        }

        // Ordinary containers and labels do not
        // intercept mouse input themselves.
        return null;
    }
}
