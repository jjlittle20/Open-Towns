
package xaos.platform.lwjgl3.input;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class KeyboardTest {

    @AfterEach
    void cleanup() {
        Keyboard.destroy();
    }

    @Test
    void preservesFullUnicodeCodePoint() {

        Keyboard.destroy();

        Keyboard.enqueueCharacter(0x1F48E);

        // Legacy compatibility event.
        assertTrue(Keyboard.next());

        assertFalse(Keyboard.isTextInputEvent());
        assertEquals(
                Keyboard.KEY_NONE,
                Keyboard.getEventKey());

        // New Unicode character event.
        assertTrue(Keyboard.next());

        assertTrue(Keyboard.isTextInputEvent());
        assertEquals(
                0x1F48E,
                Keyboard.getEventCodePoint());

        assertFalse(Keyboard.next());
    }

    @Test
    void consecutiveCharactersRemainSeparateEvents() {

        Keyboard.destroy();

        Keyboard.enqueueCharacter('A');
        Keyboard.enqueueCharacter('B');

        StringBuilder received = new StringBuilder();

        while (Keyboard.next()) {

            if (Keyboard.isTextInputEvent()) {
                received.appendCodePoint(
                        Keyboard.getEventCodePoint());
            }
        }

        assertEquals("AB", received.toString());
    }
}
