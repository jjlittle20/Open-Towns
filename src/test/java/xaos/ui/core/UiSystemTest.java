
package xaos.ui.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import xaos.ui.components.UiTextField;
import xaos.ui.components.UiWindow;
import xaos.ui.input.UiInputRouter;
import xaos.ui.input.UiKey;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.theme.BasicUiTheme;
import xaos.ui.theme.UiThemeRegistry;

class UiSystemTest {

    @Test
    void adoptsExistingWindowManagerAndThemes() {

        UiWindowManager windows = new UiWindowManager();

        UiThemeRegistry themes = new UiThemeRegistry();

        themes.register("basic", new BasicUiTheme());

        UiWindow window = new UiWindow("Inventory");
        windows.register(window);

        UiSystem system = new UiSystem(
                windows, themes);

        assertSame(windows, system.windows());
        assertSame(themes, system.themes());

        assertSame(
                windows.root(),
                system.root());

        assertTrue(
                system.windows().isManaged(window));

        assertEquals(
                "basic",
                system.themes().activeId());
    }

    @Test
    void layoutUpdatesExistingWindows() {

        UiSystem system = new UiSystem();

        UiWindow window = new UiWindow("Inventory");

        window.setPosition(new UiPoint(750, 550));
        window.setPreferredSize(new UiSize(300, 200));

        system.windows().register(window);

        assertTrue(
                system.updateLayout(new UiSize(800, 600)));

        // Existing viewport constraints still apply.
        assertEquals(
                new UiRect(500, 400, 300, 200),
                window.bounds());

        // Unchanged layout should not recalculate.
        assertFalse(
                system.updateLayout(new UiSize(800, 600)));
    }

    @Test
    void cancellationClearsInputButPreservesText() {

        UiSystem system = new UiSystem();

        UiWindow window = new UiWindow("Inventory");

        UiTextField field = new UiTextField(
                "",
                text -> new UiSize(text.length() * 7, 16));

        window.content().add(field);
        system.windows().register(window);

        system.updateLayout(new UiSize(800, 600));

        UiInputRouter input = system.input();

        int mouseX = field.bounds().x() + 10;
        int mouseY = field.bounds().y() + 10;

        input.mousePressed(mouseX, mouseY);
        input.mouseReleased(mouseX, mouseY);

        assertTrue(input.textInputActive());

        input.characterTyped('A');

        assertEquals("A", field.text());

        system.cancelInput();

        assertFalse(input.textInputActive());

        assertNull(
                input.focusManager().focusedElement());

        // Cancellation does not destroy UI state.
        assertEquals("A", field.text());
        assertTrue(window.isOpen());

        assertTrue(
                system.windows().isManaged(window));
    }

    @Test
    void defaultSystemHasIndependentState() {

        UiSystem first = new UiSystem();
        UiSystem second = new UiSystem();

        assertNotSame(first.root(), second.root());
        assertNotSame(first.input(), second.input());
        assertNotSame(first.themes(), second.themes());
        assertNotSame(first.windows(), second.windows());
    }

    @Test
    void gameOwnedWindowAcceptsInputWithoutDebugOverlay() {

        UiSystem system = new UiSystem();

        UiWindow window = new UiWindow("Inventory");

        window.setPosition(new UiPoint(50, 50));
        window.setPreferredSize(new UiSize(300, 180));

        UiTextField field = new UiTextField(
                "",
                text -> new UiSize(text.length() * 7, 16));

        window.content().add(field);

        // Register directly with the permanent runtime.
        system.windows().register(window);

        system.updateLayout(new UiSize(800, 600));

        UiInputRouter input = system.input();

        int mouseX = field.bounds().x() + 10;
        int mouseY = field.bounds().y() + 10;

        // Focus the game-owned text field.
        assertTrue(input.mousePressed(mouseX, mouseY));
        assertTrue(input.mouseReleased(mouseX, mouseY));

        assertTrue(input.textInputActive());

        // Unicode text input reaches the field.
        assertTrue(input.characterTyped('A'));
        assertTrue(input.characterTyped(0x1F48E));

        assertEquals("A💎", field.text());

        // Keyboard editing uses the same input router.
        assertTrue(input.keyPressed(
                UiKey.BACKSPACE, false));

        assertEquals("A", field.text());

        input.keyReleased(UiKey.BACKSPACE);

        // Cancelling the permanent UI input releases focus.
        system.cancelInput();

        assertFalse(input.textInputActive());
        assertNull(
                input.focusManager().focusedElement());

        // The window and its text survive cancellation.
        assertTrue(window.isOpen());
        assertEquals("A", field.text());
    }

}
