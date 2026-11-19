
package xaos.ui.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiFocusable;
import xaos.ui.core.UiRoot;

/**
 * Manages keyboard focus within a UI root.
 *
 * Only one control can be focused at a time.
 *
 * Floating windows act as independent focus scopes.
 * Tab navigation stays inside the active window.
 */
public final class UiFocusManager {

    private final UiRoot root;

    private UiElement focused;
    private UiWindow activeWindow;

    public UiFocusManager(UiRoot root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    // ------------------------------------------------------------
    // Current focus
    // ------------------------------------------------------------

    public UiElement focusedElement() {

        if (!canFocus(focused)) {
            focused = null;
        }

        return focused;
    }

    public boolean isFocused(UiElement element) {
        return element != null
                && focusedElement() == element;
    }

    // ------------------------------------------------------------
    // Active window
    // ------------------------------------------------------------

    /**
     * Returns the active keyboard window, or null.
     *
     * A closed, hidden or detached window automatically
     * loses its active scope.
     */
    public UiWindow activeWindow() {

        if (activeWindow != null
                && !belongsToVisibleTree(activeWindow)) {

            activeWindow = null;
            focused = null;
        }

        return activeWindow;
    }

    /**
     * Selects the keyboard scope containing a mouse target.
     *
     * Clicking a different window changes the scope and
     * clears the previous focus.
     *
     * Clicking within the same window preserves the scope.
     */
    public void activateScopeFor(UiElement target) {

        UiWindow next = belongsToVisibleTree(target)
                ? findOwningWindow(target)
                : null;

        if (activeWindow() != next) {
            activeWindow = next;
            focused = null;
        }
    }

    // ------------------------------------------------------------
    // Focus eligibility
    // ------------------------------------------------------------

    public boolean canFocus(UiElement element) {

        return element instanceof UiFocusable focusable
                && focusable.canReceiveFocus()
                && belongsToVisibleTree(element);
    }

    /**
     * Verifies that an element belongs to this root
     * and that it and all its ancestors are visible.
     */
    private boolean belongsToVisibleTree(
            UiElement element) {

        for (UiElement current = element;
                current != null;
                current = current.parent()) {

            if (!current.visible()) {
                return false;
            }

            if (current == root.content()) {
                return true;
            }
        }

        return false;
    }

    // ------------------------------------------------------------
    // Focus changes
    // ------------------------------------------------------------

    /**
     * Requests focus for an eligible control.
     *
     * Programmatic focus requests also activate
     * the control's owning window.
     */
    public boolean requestFocus(UiElement element) {

        if (!canFocus(element)) {
            return false;
        }

        if (focusedElement() == element) {
            return false;
        }

        activeWindow = findOwningWindow(element);
        focused = element;

        return true;
    }

    /**
     * Clears focus without changing the active window.
     *
     * This allows Tab to start at the first control
     * after clicking an empty area of a window.
     */
    public boolean clearFocus() {

        boolean changed = focused != null;

        focused = null;

        return changed;
    }

    // ------------------------------------------------------------
    // Keyboard traversal
    // ------------------------------------------------------------

    public boolean focusNext() {
        return moveFocus(false);
    }

    public boolean focusPrevious() {
        return moveFocus(true);
    }

    private boolean moveFocus(boolean backwards) {

        UiWindow scope = activeWindow();

        List<UiElement> candidates =
                new ArrayList<>();

        // An active window restricts traversal to its
        // own controls.
        //
        // Without one, root-level controls remain
        // accessible, but other windows are skipped.
        collect(
                scope == null ? root.content() : scope,
                candidates,
                scope == null);

        if (candidates.isEmpty()) {
            return clearFocus();
        }

        int current = candidates.indexOf(
                focusedElement());

        int next;

        if (current < 0) {

            next = backwards
                    ? candidates.size() - 1
                    : 0;

        } else {

            next = Math.floorMod(
                    current + (backwards ? -1 : 1),
                    candidates.size());
        }

        return requestFocus(candidates.get(next));
    }

    /**
     * Collects controls in their natural tree order.
     *
     * When traversing the root scope, nested windows
     * are excluded.
     */
    private void collect(
            UiElement element,
            List<UiElement> candidates,
            boolean skipWindows) {

        if (!element.visible()) {
            return;
        }

        if (skipWindows
                && element instanceof UiWindow
                && element != root.content()) {

            return;
        }

        if (element instanceof UiFocusable focusable
                && focusable.canReceiveFocus()) {

            candidates.add(element);
        }

        if (element instanceof UiContainer container) {

            for (UiElement child : container.children()) {
                collect(child, candidates, skipWindows);
            }
        }
    }

    // ------------------------------------------------------------
    // Window lookup
    // ------------------------------------------------------------

    /**
     * Finds the nearest window containing an element.
     *
     * Returns null for controls outside windows.
     */
    private static UiWindow findOwningWindow(
            UiElement element) {

        for (UiElement current = element;
                current != null;
                current = current.parent()) {

            if (current instanceof UiWindow window) {
                return window;
            }
        }

        return null;
    }
}
