
package xaos.ui.components;

import java.util.Objects;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiFocusable;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

/**
 * Single-line editable text field model.
 *
 * Cursor positions are Unicode code-point indices rather
 * than Java UTF-16 char indices, so a cursor cannot split
 * a surrogate pair.
 *
 * Rendering and keyboard event routing are separate.
 */
public final class UiTextField extends UiElement
        implements UiFocusable {

    private final UiTextMetrics textMetrics;

    private String text = "";
    private String placeholder = "";

    private UiInsets padding =
            new UiInsets(6, 8, 6, 8);

    private int cursorPosition;
    private boolean enabled = true;

    // ------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------

    public UiTextField(
            String text,
            UiTextMetrics textMetrics) {

        this.textMetrics = Objects.requireNonNull(
                textMetrics, "textMetrics");

        setMinimumSize(new UiSize(100, 28));
        setPreferredSize(new UiSize(180, 30));

        setText(text);
    }

    // ------------------------------------------------------------
    // Text
    // ------------------------------------------------------------

    public String text() {
        return text;
    }

    /**
     * Replaces the current text.
     *
     * The cursor moves to the end of the new value.
     * Reassigning the same value leaves the cursor alone.
     */
    public void setText(String text) {

        String value = requireSingleLine(text);

        if (!this.text.equals(value)) {
            this.text = value;
            cursorPosition = codePointLength(value);
            invalidateLayout();
        }
    }

    public String placeholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = requireSingleLine(placeholder);
    }

    // ------------------------------------------------------------
    // Enabled state and focus
    // ------------------------------------------------------------

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean canReceiveFocus() {
        return enabled;
    }

    // ------------------------------------------------------------
    // Appearance
    // ------------------------------------------------------------

    public UiTextMetrics textMetrics() {
        return textMetrics;
    }

    public UiInsets padding() {
        return padding;
    }

    public void setPadding(UiInsets padding) {

        UiInsets next = Objects.requireNonNull(
                padding, "padding");

        if (!this.padding.equals(next)) {
            this.padding = next;
            invalidateLayout();
        }
    }

    // ------------------------------------------------------------
    // Cursor
    // ------------------------------------------------------------

    /**
     * Zero-based Unicode code-point position.
     *
     * Zero is before the first character.
     * The text length is the position after the last.
     */
    public int cursorPosition() {
        return cursorPosition;
    }

    /**
     * Moves the cursor to a position, clamping it
     * to the valid range.
     */
    public void setCursorPosition(int position) {

        cursorPosition = Math.max(
                0,
                Math.min(position, codePointLength(text)));
    }

    public void moveCursor(int delta) {

        long position = (long) cursorPosition + delta;

        setCursorPosition((int) Math.max(
                0L,
                Math.min(codePointLength(text), position)));
    }

    public void moveCursorToStart() {
        setCursorPosition(0);
    }

    public void moveCursorToEnd() {
        setCursorPosition(codePointLength(text));
    }

    // ------------------------------------------------------------
    // Editing
    // ------------------------------------------------------------

    /**
     * Inserts text at the cursor and advances it.
     *
     * @return true when text was inserted
     */
    public boolean insertText(String value) {

        String insertion = requireSingleLine(value);

        if (insertion.isEmpty()) {
            return false;
        }

        int offset = text.offsetByCodePoints(
                0, cursorPosition);

        text = text.substring(0, offset)
                + insertion
                + text.substring(offset);

        cursorPosition += codePointLength(insertion);

        invalidateLayout();
        return true;
    }

    /**
     * Deletes the code point immediately before
     * the cursor, like Backspace.
     */
    public boolean deleteBackward() {

        if (cursorPosition == 0) {
            return false;
        }

        int start = text.offsetByCodePoints(
                0, cursorPosition - 1);

        int end = text.offsetByCodePoints(
                0, cursorPosition);

        text = text.substring(0, start)
                + text.substring(end);

        cursorPosition--;

        invalidateLayout();
        return true;
    }

    /**
     * Deletes the code point immediately after
     * the cursor, like the Delete key.
     */
    public boolean deleteForward() {

        if (cursorPosition == codePointLength(text)) {
            return false;
        }

        int start = text.offsetByCodePoints(
                0, cursorPosition);

        int end = text.offsetByCodePoints(
                0, cursorPosition + 1);

        text = text.substring(0, start)
                + text.substring(end);

        invalidateLayout();
        return true;
    }

    // ------------------------------------------------------------
    // Measurement
    // ------------------------------------------------------------

    @Override
    public UiSize measure(UiSize available) {

        Objects.requireNonNull(available, "available");

        if (!visible()) {
            return UiSize.ZERO;
        }

        UiSize font = Objects.requireNonNull(
                textMetrics.measure("Mg"),
                "textMetrics result");

        int height = Math.max(
                preferredSize().height(),
                Math.addExact(
                        font.height(),
                        padding.vertical()));

        // The field has a preferred width independent of
        // its contents. Long text will eventually scroll
        // horizontally inside the field.
        return new UiSize(
                Math.max(
                        minimumSize().width(),
                        Math.min(
                                preferredSize().width(),
                                available.width())),
                Math.max(
                        minimumSize().height(),
                        Math.min(height, available.height())));
    }

    // ------------------------------------------------------------
    // Text geometry
    // ------------------------------------------------------------

    /**
     * Returns the width of the visible text area.
     */
    public int textViewportWidth() {
        return Math.max(
                0,
                bounds().width() - padding.horizontal());
    }

    /**
     * Measures the text preceding the cursor.
     */
    private int cursorPixelOffset() {

        int utf16Offset = text.offsetByCodePoints(
                0, cursorPosition);

        return textMetrics.measure(
                text.substring(0, utf16Offset)).width();
    }

    /**
     * Returns the X coordinate where the text begins.
     *
     * Long text is shifted left to keep the cursor
     * within the visible area.
     */
    public int textDrawX() {

        int innerWidth = textViewportWidth();

        int visibleWidth = Math.max(0, innerWidth - 2);

        int scrollOffset = Math.max(
                0,
                cursorPixelOffset() - visibleWidth);

        return bounds().x()
                + padding.left()
                - scrollOffset;
    }

    /**
     * Returns the caret's absolute X coordinate.
     */
    public int caretDrawX() {
        return textDrawX() + cursorPixelOffset();
    }

    /**
     * Positions the cursor at the closest character
     * boundary to a mouse X coordinate.
     *
     * Cursor indices remain Unicode code-point based.
     */
    public void placeCursorAt(int mouseX) {

        int startX = textDrawX();
        int count = codePointLength(text);

        int previousWidth = 0;

        for (int index = 1; index <= count; index++) {

            int utf16Offset = text.offsetByCodePoints(
                    0, index);

            int currentWidth = textMetrics.measure(
                    text.substring(0, utf16Offset)).width();

            int midpoint = previousWidth
                    + (currentWidth - previousWidth) / 2;

            if ((long) mouseX < (long) startX + midpoint) {
                setCursorPosition(index - 1);
                return;
            }

            previousWidth = currentWidth;
        }

        setCursorPosition(count);
    }


    // ------------------------------------------------------------
    // Validation and Unicode helpers
    // ------------------------------------------------------------

    private static String requireSingleLine(
            String value) {

        Objects.requireNonNull(value, "text");

        if (value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {

            throw new IllegalArgumentException(
                    "Text field must be single-line");
        }

        return value;
    }

    private static int codePointLength(String value) {
        return value.codePointCount(
                0, value.length());
    }
}
