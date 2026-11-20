
package xaos.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.StringTokenizer;

import org.junit.jupiter.api.Test;

import xaos.platform.lwjgl3.input.Keyboard;

class UtilsKeyboardTest {

    @Test
    void savesBothAssignedKeys() {

        assertEquals(
                "W, UP",
                UtilsKeyboard.shortcutPropertyValue(
                        Keyboard.KEY_W,
                        Keyboard.KEY_UP));
    }

    @Test
    void savesPrimaryOnly() {

        assertEquals(
                "W",
                UtilsKeyboard.shortcutPropertyValue(
                        Keyboard.KEY_W,
                        Keyboard.KEY_NONE));
    }

    @Test
    void savesSecondaryWithoutPrimary() {

        assertEquals(
                "NONE, UP",
                UtilsKeyboard.shortcutPropertyValue(
                        Keyboard.KEY_NONE,
                        Keyboard.KEY_UP));
    }

    @Test
    void savesBothUnboundAsEmpty() {

        assertEquals(
                "",
                UtilsKeyboard.shortcutPropertyValue(
                        Keyboard.KEY_NONE,
                        Keyboard.KEY_NONE));
    }

    @Test
    void secondaryOnlyValueUsesExistingKeyNames() {

        String saved = UtilsKeyboard.shortcutPropertyValue(
                Keyboard.KEY_NONE,
                Keyboard.KEY_UP);

        // Verify that the existing comma-separated
        // key-name format can represent both slots.
        StringTokenizer tokens = new StringTokenizer(saved, ",");

        int primary = Keyboard.getKeyIndex(
                tokens.nextToken().trim());

        int secondary = Keyboard.getKeyIndex(
                tokens.nextToken().trim());

        assertEquals(Keyboard.KEY_NONE, primary);
        assertEquals(Keyboard.KEY_UP, secondary);
    }
}
