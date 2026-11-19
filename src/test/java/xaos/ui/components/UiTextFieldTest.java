
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiRoot;
import xaos.ui.input.UiFocusManager;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiTextFieldTest {

    private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

    @Test
    void textReplacementMovesCursorToEnd() {

        UiTextField field = new UiTextField(
                "Iron Ore", FONT);

        assertEquals("Iron Ore", field.text());
        assertEquals(8, field.cursorPosition());

        field.setText("Wood");

        assertEquals("Wood", field.text());
        assertEquals(4, field.cursorPosition());

        field.setCursorPosition(1);

        // Setting the same text should not reset
        // the player's current cursor position.
        field.setText("Wood");

        assertEquals(1, field.cursorPosition());
    }

    @Test
    void cursorMovesAndClampsToTextBounds() {

        UiTextField field = new UiTextField(
                "Stone", FONT);

        field.moveCursorToStart();

        assertEquals(0, field.cursorPosition());

        field.moveCursor(2);

        assertEquals(2, field.cursorPosition());

        field.moveCursor(-1);

        assertEquals(1, field.cursorPosition());

        field.moveCursor(1000);

        assertEquals(5, field.cursorPosition());

        field.moveCursor(-1000);

        assertEquals(0, field.cursorPosition());
    }

    @Test
    void insertionAndDeletionUpdateTextAndCursor() {

        UiTextField field = new UiTextField(
                "Stone", FONT);

        field.setCursorPosition(1);

        assertTrue(field.insertText("X"));

        assertEquals("SXtone", field.text());
        assertEquals(2, field.cursorPosition());

        // Backspace removes the inserted X.
        assertTrue(field.deleteBackward());

        assertEquals("Stone", field.text());
        assertEquals(1, field.cursorPosition());

        // Delete removes the t after the cursor.
        assertTrue(field.deleteForward());

        assertEquals("Sone", field.text());
        assertEquals(1, field.cursorPosition());

        field.moveCursorToStart();

        assertFalse(field.deleteBackward());

        field.moveCursorToEnd();

        assertFalse(field.deleteForward());
    }

    @Test
    void cursorHandlesUnicodeCodePoints() {

        UiTextField field = new UiTextField(
                "A💎B", FONT);

        // Three code points, despite four UTF-16 chars.
        assertEquals(3, field.cursorPosition());

        field.setCursorPosition(2);

        assertTrue(field.deleteBackward());

        assertEquals("AB", field.text());
        assertEquals(1, field.cursorPosition());

        field.insertText("💎");

        assertEquals("A💎B", field.text());
        assertEquals(2, field.cursorPosition());
    }

    @Test
    void fieldRejectsMultilineText() {

        UiTextField field = new UiTextField(
                "", FONT);

        assertThrows(
                IllegalArgumentException.class,
                () -> field.setText("Line 1\nLine 2"));

        assertThrows(
                IllegalArgumentException.class,
                () -> field.insertText("Hello\rWorld"));

        assertThrows(
                IllegalArgumentException.class,
                () -> field.setPlaceholder("One\nTwo"));

        assertEquals("", field.text());
    }

    @Test
    void textFieldKeepsPreferredWidth() {

        UiTextField field = new UiTextField(
                "Short", FONT);

        assertEquals(
                new UiSize(180, 30),
                field.measure(new UiSize(500, 100)));

        field.setText("A very long piece of text "
                + "that exceeds the field width");

        // Text does not force the field to grow.
        assertEquals(
                new UiSize(180, 30),
                field.measure(new UiSize(500, 100)));

        // A narrower parent can constrain the field.
        assertEquals(
                new UiSize(140, 30),
                field.measure(new UiSize(140, 100)));
    }

    @Test
    void editingInvalidatesParentLayout() {

        UiContainer desktop = new UiContainer();

        UiTextField field = new UiTextField(
                "Iron", FONT);

        desktop.add(field);

        UiRoot root = new UiRoot(desktop);
        root.updateLayout(new UiSize(400, 200));

        assertFalse(desktop.isLayoutDirty());

        field.insertText(" Ore");

        assertTrue(field.isLayoutDirty());
        assertTrue(desktop.isLayoutDirty());

        assertTrue(
                root.updateLayout(new UiSize(400, 200)));

        assertEquals(
                new UiRect(0, 0, 180, 30),
                field.bounds());
    }

    @Test
    void disabledTextFieldCannotReceiveFocus() {

        UiContainer desktop = new UiContainer();

        UiTextField field = new UiTextField(
                "", FONT);

        desktop.add(field);

        UiFocusManager focus = new UiFocusManager(new UiRoot(desktop));

        assertTrue(focus.requestFocus(field));

        field.setEnabled(false);

        assertFalse(field.canReceiveFocus());
        assertNull(focus.focusedElement());

        field.setEnabled(true);

        assertTrue(focus.requestFocus(field));
    }

    @Test
    void caretPositionUsesMeasuredTextWidth() {

        UiTextField field = new UiTextField(
                "Stone", FONT);

        field.layout(new UiRect(20, 30, 180, 30));

        // Text begins after 8 units of left padding.
        assertEquals(28, field.textDrawX());

        // Five characters at 7 units each.
        assertEquals(63, field.caretDrawX());

        field.setCursorPosition(2);

        // Cursor after "St".
        assertEquals(42, field.caretDrawX());
    }

    @Test
    void mousePositionsCursorAtNearestBoundary() {

        UiTextField field = new UiTextField(
                "Stone", FONT);

        field.layout(new UiRect(20, 30, 180, 30));

        field.placeCursorAt(42);

        assertEquals(2, field.cursorPosition());

        // Clicking before the text moves to the start.
        field.placeCursorAt(0);

        assertEquals(0, field.cursorPosition());

        // Clicking beyond the text moves to the end.
        field.placeCursorAt(500);

        assertEquals(5, field.cursorPosition());
    }

    @Test
    void longTextKeepsCaretInsideViewport() {

        UiTextField field = new UiTextField(
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ", FONT);

        field.layout(new UiRect(10, 10, 100, 30));

        int left = field.bounds().x()
                + field.padding().left();

        int right = field.bounds().x()
                + field.bounds().width()
                - field.padding().right();

        // Long text shifts left to keep the caret visible.
        assertTrue(field.textDrawX() < left);

        assertTrue(field.caretDrawX() >= left);
        assertTrue(field.caretDrawX() < right);

        // Returning to the beginning reveals the text start.
        field.moveCursorToStart();

        assertEquals(left, field.textDrawX());
        assertEquals(left, field.caretDrawX());
    }

}
