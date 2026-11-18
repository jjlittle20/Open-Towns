
package xaos.ui.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiFocusable;
import xaos.ui.core.UiRoot;

/**
 * Manages keyboard focus within a single UI root.
 *
 * Only one control can be focused at a time.
 *
 * Focus is automatically invalidated when a control
 * becomes disabled, hidden or detached from the tree.
 */
public final class UiFocusManager {

    private final UiRoot root;

    private UiElement focused;

    public UiFocusManager(UiRoot root) {
        this.root = Objects.requireNonNull(
                root, "root");
    }

    // ------------------------------------------------------------
    // Current focus
    // ------------------------------------------------------------

    /**
     * Returns the currently focused element, or null.
     *
     * Invalid focus is cleared automatically.
     */
    public UiElement focusedElement() {

        if (!canFocus(focused)) {
            focused = null;
        }

        return focused;
    }

    /**
     * Returns whether an element currently owns focus.
     */
    public boolean isFocused(UiElement element) {
        return element != null
                && focusedElement() == element;
    }

    // ------------------------------------------------------------
    // Focus eligibility
    // ------------------------------------------------------------

    /**
     * Checks whether a component can receive focus.
     *
     * It must:
     * - implement UiFocusable
     * - accept focus
     * - be visible, including all ancestors
     * - belong to this manager's UI root
     */
    public boolean canFocus(UiElement element) {

        if (!(element instanceof UiFocusable focusable)
                || !focusable.canReceiveFocus()) {

            return false;
        }

        // Walk upwards to verify visibility and
        // membership of this UI tree.
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
     * Requests focus for an element.
     *
     * @return true if focus changed
     */
    public boolean requestFocus(UiElement element) {

        if (!canFocus(element)) {
            return false;
        }

        if (focusedElement() == element) {
            return false;
        }

        focused = element;
        return true;
    }

    /**
     * Clears keyboard focus.
     *
     * @return true if a focus owner was cleared
     */
    public boolean clearFocus() {

        boolean changed = focused != null;

        focused = null;

        return changed;
    }

    // ------------------------------------------------------------
    // Focus traversal
    // ------------------------------------------------------------

    /**
     * Moves focus to the next eligible control.
     */
    public boolean focusNext() {
        return moveFocus(false);
    }

    /**
     * Moves focus to the previous eligible control.
     */
    public boolean focusPrevious() {
        return moveFocus(true);
    }

    private boolean moveFocus(boolean backwards) {

        List<UiElement> candidates =
                new ArrayList<>();

        collect(root.content(), candidates);

        if (candidates.isEmpty()) {
            return clearFocus();
        }

        int current = candidates.indexOf(
                focusedElement());

        int next;

        if (current < 0) {

            // Nothing focused: begin at the first
            // or last eligible component.
            next = backwards
                    ? candidates.size() - 1
                    : 0;

        } else {

            // Wrap around at either end.
            next = Math.floorMod(
                    current + (backwards ? -1 : 1),
                    candidates.size());
        }

        return requestFocus(candidates.get(next));
    }

    /**
     * Collects focusable controls in UI tree order.
     */
    private void collect(
            UiElement element,
            List<UiElement> candidates) {

        if (!element.visible()) {
            return;
        }

        if (element instanceof UiFocusable focusable
                && focusable.canReceiveFocus()) {

            candidates.add(element);
        }

        if (element instanceof UiContainer container) {

            for (UiElement child : container.children()) {
                collect(child, candidates);
            }
        }
    }
}
