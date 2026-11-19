
package xaos.ui.input;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiRoot;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiFocusManagerTest {

    private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

    private static UiButton button(String text) {
        return new UiButton(text, FONT);
    }

    @Test
    void onlyOneElementCanOwnFocus() {

        UiContainer desktop = new UiContainer();

        UiButton first = button("First");
        UiButton second = button("Second");

        desktop.add(first);
        desktop.add(second);

        UiFocusManager focus = new UiFocusManager(
                new UiRoot(desktop));

        assertNull(focus.focusedElement());

        assertTrue(focus.requestFocus(first));
        assertTrue(focus.isFocused(first));

        assertTrue(focus.requestFocus(second));

        assertFalse(focus.isFocused(first));
        assertTrue(focus.isFocused(second));

        // Requesting the same focus does not
        // count as another focus change.
        assertFalse(focus.requestFocus(second));

        assertTrue(focus.clearFocus());
        assertNull(focus.focusedElement());

        assertFalse(focus.clearFocus());
    }

    @Test
    void focusTraversalSkipsDisabledControls() {

        UiContainer desktop = new UiContainer();

        UiButton first = button("First");
        UiButton disabled = button("Disabled");
        UiButton last = button("Last");

        disabled.setEnabled(false);

        desktop.add(first);
        desktop.add(disabled);
        desktop.add(last);

        UiFocusManager focus = new UiFocusManager(
                new UiRoot(desktop));

        assertTrue(focus.focusNext());
        assertSame(first, focus.focusedElement());

        // Disabled control is skipped.
        assertTrue(focus.focusNext());
        assertSame(last, focus.focusedElement());

        // Wrap to the beginning.
        assertTrue(focus.focusNext());
        assertSame(first, focus.focusedElement());

        // Reverse traversal wraps to the end.
        assertTrue(focus.focusPrevious());
        assertSame(last, focus.focusedElement());
    }

    @Test
    void closingWindowInvalidatesFocus() {

        UiContainer desktop = new UiContainer();

        UiWindow window = new UiWindow("Inventory");
        UiButton button = button("Search");

        window.content().add(button);
        desktop.add(window);

        UiFocusManager focus = new UiFocusManager(
                new UiRoot(desktop));

        assertTrue(focus.requestFocus(button));
        assertTrue(focus.isFocused(button));

        window.close();

        // Hidden ancestors invalidate keyboard focus.
        assertNull(focus.focusedElement());
        assertFalse(focus.requestFocus(button));

        window.open();

        // Reopening does not unexpectedly restore focus.
        assertNull(focus.focusedElement());

        assertTrue(focus.requestFocus(button));
    }

    @Test
    void detachedAndDisabledControlsLoseFocus() {

        UiContainer desktop = new UiContainer();

        UiButton button = button("Action");
        desktop.add(button);

        UiFocusManager focus = new UiFocusManager(
                new UiRoot(desktop));

        assertTrue(focus.requestFocus(button));

        button.setEnabled(false);

        assertNull(focus.focusedElement());
        assertFalse(focus.requestFocus(button));

        button.setEnabled(true);

        assertTrue(focus.requestFocus(button));

        desktop.remove(button);

        assertNull(focus.focusedElement());
        assertFalse(focus.requestFocus(button));
    }

    @Test
    void focusCannotCrossUiRoots() {

        UiContainer firstDesktop = new UiContainer();
        UiContainer secondDesktop = new UiContainer();

        UiButton firstButton = button("First");
        UiButton secondButton = button("Second");

        firstDesktop.add(firstButton);
        secondDesktop.add(secondButton);

        UiFocusManager focus = new UiFocusManager(
                new UiRoot(firstDesktop));

        assertTrue(focus.requestFocus(firstButton));

        // The other UI root does not belong
        // to this focus manager.
        assertFalse(focus.requestFocus(secondButton));

        assertSame(firstButton, focus.focusedElement());
    }

    @Test
    void tabTraversalStaysInsideActiveWindow() {

        UiContainer desktop = new UiContainer();

        UiWindow firstWindow = new UiWindow("Inventory");
        UiWindow secondWindow = new UiWindow("Building");

        UiButton first = button("Search");
        UiButton second = button("Sort");
        UiButton other = button("Build");

        firstWindow.content().add(first);
        firstWindow.content().add(second);

        secondWindow.content().add(other);

        desktop.add(firstWindow);
        desktop.add(secondWindow);

        UiFocusManager focus = new UiFocusManager(
                new UiRoot(desktop));

        assertTrue(focus.requestFocus(first));

        assertSame(firstWindow, focus.activeWindow());

        // Navigate to the second control.
        assertTrue(focus.focusNext());
        assertSame(second, focus.focusedElement());

        // Wrap to the first control, not the other window.
        assertTrue(focus.focusNext());
        assertSame(first, focus.focusedElement());

        // Reverse traversal also stays in this window.
        assertTrue(focus.focusPrevious());
        assertSame(second, focus.focusedElement());

        assertFalse(focus.isFocused(other));
    }

    @Test
    void switchingWindowsChangesFocusScope() {

        UiContainer desktop = new UiContainer();

        UiWindow inventory = new UiWindow("Inventory");
        UiWindow building = new UiWindow("Building");

        UiButton inventoryButton = button("Search");
        UiButton buildingButton = button("Build");

        inventory.content().add(inventoryButton);
        building.content().add(buildingButton);

        desktop.add(inventory);
        desktop.add(building);

        UiFocusManager focus = new UiFocusManager(
                new UiRoot(desktop));

        // Activate inventory.
        focus.activateScopeFor(inventory);

        assertSame(inventory, focus.activeWindow());

        assertTrue(focus.focusNext());
        assertSame(inventoryButton, focus.focusedElement());

        // Activate building by clicking its non-button area.
        focus.activateScopeFor(building);

        assertSame(building, focus.activeWindow());
        assertNull(focus.focusedElement());

        // Tab now starts with the building controls.
        assertTrue(focus.focusNext());
        assertSame(buildingButton, focus.focusedElement());

        // Switch back to inventory.
        focus.activateScopeFor(inventory);

        assertTrue(focus.focusNext());
        assertSame(inventoryButton, focus.focusedElement());
    }

    @Test
    void closingActiveWindowInvalidatesScope() {

        UiContainer desktop = new UiContainer();

        UiWindow window = new UiWindow("Inventory");
        UiButton button = button("Search");

        window.content().add(button);
        desktop.add(window);

        UiFocusManager focus = new UiFocusManager(
                new UiRoot(desktop));

        assertTrue(focus.requestFocus(button));
        assertSame(window, focus.activeWindow());

        window.close();

        assertNull(focus.activeWindow());
        assertNull(focus.focusedElement());

        // Closing must not allow Tab to focus
        // a hidden control.
        assertFalse(focus.focusNext());

        window.open();

        // Clicking the reopened window restores its scope.
        focus.activateScopeFor(window);

        assertTrue(focus.focusNext());
        assertSame(button, focus.focusedElement());
    }

}
