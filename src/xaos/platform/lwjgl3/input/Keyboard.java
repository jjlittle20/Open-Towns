package xaos.platform.lwjgl3.input;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import static org.lwjgl.glfw.GLFW.*;

public final class Keyboard {
    public static final int KEY_NONE = 0;
    public static final int KEY_ESCAPE = GLFW_KEY_ESCAPE;
    public static final int KEY_1 = GLFW_KEY_1;
    public static final int KEY_2 = GLFW_KEY_2;
    public static final int KEY_3 = GLFW_KEY_3;
    public static final int KEY_4 = GLFW_KEY_4;
    public static final int KEY_5 = GLFW_KEY_5;
    public static final int KEY_6 = GLFW_KEY_6;
    public static final int KEY_7 = GLFW_KEY_7;
    public static final int KEY_8 = GLFW_KEY_8;
    public static final int KEY_9 = GLFW_KEY_9;
    public static final int KEY_0 = GLFW_KEY_0;
    public static final int KEY_A = GLFW_KEY_A;
    public static final int KEY_B = GLFW_KEY_B;
    public static final int KEY_C = GLFW_KEY_C;
    public static final int KEY_D = GLFW_KEY_D;
    public static final int KEY_E = GLFW_KEY_E;
    public static final int KEY_F = GLFW_KEY_F;
    public static final int KEY_G = GLFW_KEY_G;
    public static final int KEY_H = GLFW_KEY_H;
    public static final int KEY_I = GLFW_KEY_I;
    public static final int KEY_J = GLFW_KEY_J;
    public static final int KEY_K = GLFW_KEY_K;
    public static final int KEY_L = GLFW_KEY_L;
    public static final int KEY_M = GLFW_KEY_M;
    public static final int KEY_N = GLFW_KEY_N;
    public static final int KEY_O = GLFW_KEY_O;
    public static final int KEY_P = GLFW_KEY_P;
    public static final int KEY_Q = GLFW_KEY_Q;
    public static final int KEY_R = GLFW_KEY_R;
    public static final int KEY_S = GLFW_KEY_S;
    public static final int KEY_T = GLFW_KEY_T;
    public static final int KEY_U = GLFW_KEY_U;
    public static final int KEY_V = GLFW_KEY_V;
    public static final int KEY_W = GLFW_KEY_W;
    public static final int KEY_X = GLFW_KEY_X;
    public static final int KEY_Y = GLFW_KEY_Y;
    public static final int KEY_Z = GLFW_KEY_Z;
    public static final int KEY_BACK = GLFW_KEY_BACKSPACE;
    public static final int KEY_DELETE = GLFW_KEY_DELETE;
    public static final int KEY_DIVIDE = GLFW_KEY_KP_DIVIDE;
    public static final int KEY_DOWN = GLFW_KEY_DOWN;
    public static final int KEY_LCONTROL = GLFW_KEY_LEFT_CONTROL;
    public static final int KEY_LEFT = GLFW_KEY_LEFT;
    public static final int KEY_LSHIFT = GLFW_KEY_LEFT_SHIFT;
    public static final int KEY_PERIOD = GLFW_KEY_PERIOD;
    public static final int KEY_RCONTROL = GLFW_KEY_RIGHT_CONTROL;
    public static final int KEY_RETURN = GLFW_KEY_ENTER;
    public static final int KEY_RIGHT = GLFW_KEY_RIGHT;
    public static final int KEY_RSHIFT = GLFW_KEY_RIGHT_SHIFT;
    public static final int KEY_SLASH = GLFW_KEY_SLASH;
    public static final int KEY_SPACE = GLFW_KEY_SPACE;
    public static final int KEY_UP = GLFW_KEY_UP;
    public static final int KEY_MINUS = GLFW_KEY_MINUS;
    public static final int KEY_EQUALS = GLFW_KEY_EQUAL;
    public static final int KEY_ADD = GLFW_KEY_KP_ADD;
    public static final int KEY_SUBTRACT = GLFW_KEY_KP_SUBTRACT;

    public static final int KEY_TAB = GLFW_KEY_TAB;
    public static final int KEY_NUMPADENTER = GLFW_KEY_KP_ENTER;
    public static final int KEY_F8 = GLFW_KEY_F8;

    public static final int KEY_HOME = GLFW_KEY_HOME;
    public static final int KEY_END = GLFW_KEY_END;

    private static final boolean[] down = new boolean[GLFW_KEY_LAST + 1];
    private static final Queue<Event> events = new ArrayDeque<Event>();
    private static final Map<String, Integer> namesToKeys = new HashMap<String, Integer>();
    private static final Map<Integer, String> keysToNames = new HashMap<Integer, String>();
    private static Event currentEvent;

    static {
        register("NONE", KEY_NONE);
        register("ESCAPE", KEY_ESCAPE);
        register("BACK", KEY_BACK);
        register("DELETE", KEY_DELETE);
        register("DIVIDE", KEY_DIVIDE);
        register("DOWN", KEY_DOWN);
        register("LCONTROL", KEY_LCONTROL);
        register("LEFT", KEY_LEFT);
        register("LSHIFT", KEY_LSHIFT);
        register("PERIOD", KEY_PERIOD);
        register("RCONTROL", KEY_RCONTROL);
        register("RETURN", KEY_RETURN);
        register("RIGHT", KEY_RIGHT);
        register("RSHIFT", KEY_RSHIFT);
        register("SLASH", KEY_SLASH);
        register("SPACE", KEY_SPACE);
        register("UP", KEY_UP);
        register("TAB", KEY_TAB);
        register("NUMPADENTER", KEY_NUMPADENTER);
        register("HOME", KEY_HOME);
        register("END", KEY_END);
        final int functionKeyCount = 12;

        for (int functionKeyNumber = 1; functionKeyNumber <= functionKeyCount; functionKeyNumber++) {
            register("F" + functionKeyNumber, GLFW_KEY_F1 + functionKeyNumber - 1);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            register(String.valueOf(c), GLFW_KEY_A + (c - 'A'));
        }
        for (char c = '0'; c <= '9'; c++) {
            register(String.valueOf(c), GLFW_KEY_0 + (c - '0'));
        }
    }

    private Keyboard() {
    }

    public static void install(long window) {
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key >= 0 && key <= GLFW_KEY_LAST) {
                down[key] = action != GLFW_RELEASE;
            }
            if (action == GLFW_PRESS || action == GLFW_REPEAT || action == GLFW_RELEASE) {
                events.add(new Event(key, action != GLFW_RELEASE, printableChar(key, mods)));
            }
        });

        glfwSetCharCallback(
                window,
                (win, codepoint) -> enqueueCharacter(codepoint));

    }

    public static void destroy() {
        events.clear();
        currentEvent = null;
        for (int i = 0; i < down.length; i++) {
            down[i] = false;
        }
    }

    /**
     * Clears pending keyboard events and held-key state.
     *
     * Used when the application loses input focus.
     * Does not uninstall GLFW callbacks.
     */
    public static void clearInputState() {

        events.clear();
        currentEvent = null;

        for (int i = 0; i < down.length; i++) {
            down[i] = false;
        }
    }

    public static boolean next() {
        currentEvent = events.poll();
        return currentEvent != null;
    }

    public static int getEventKey() {
        return currentEvent == null ? KEY_NONE : currentEvent.key;
    }

    public static boolean getEventKeyState() {
        return currentEvent != null && currentEvent.state;
    }

    public static char getEventCharacter() {
        return currentEvent == null ? 0 : currentEvent.character;
    }

    /**
     * Returns true when the current event is a
     * Unicode character event rather than a
     * physical key press or release.
     */
    public static boolean isTextInputEvent() {
        return currentEvent != null
                && currentEvent.textInputEvent;
    }

    /**
     * Returns the Unicode code point of the
     * current text-input event.
     */
    public static int getEventCodePoint() {
        return isTextInputEvent()
                ? currentEvent.codePoint
                : 0;
    }

    public static boolean isKeyDown(int key) {
        return key > KEY_NONE && key < down.length && down[key];
    }

    public static int getKeyIndex(String name) {
        if (name == null) {
            return KEY_NONE;
        }
        Integer key = namesToKeys.get(name.toUpperCase());
        if (key == null) {
            key = parseUnknownKey(name);
            if (key != KEY_NONE)
                register(name.toUpperCase(), key);
        }
        return key;
    }

    public static String getKeyName(int key) {
        String name = keysToNames.get(key);
        if (name == null) {
            name = "#" + key;
            register(name, key);
        }
        return name;
    }

    private static Integer parseUnknownKey(String name) {
        if (name.startsWith("#")) {
            try {
                return Integer.parseInt(name.substring(1));
            } catch (NumberFormatException _) {

            }
        }
        return KEY_NONE;
    }

    private static void register(String name, int key) {
        namesToKeys.put(name, key);
        keysToNames.put(key, name);
    }

    private static char printableChar(int key, int mods) {
        boolean shift = (mods & GLFW_MOD_SHIFT) != 0;
        if (key >= GLFW_KEY_A && key <= GLFW_KEY_Z) {
            char c = (char) ('a' + (key - GLFW_KEY_A));
            return shift ? Character.toUpperCase(c) : c;
        }
        if (key >= GLFW_KEY_0 && key <= GLFW_KEY_9) {
            return (char) ('0' + (key - GLFW_KEY_0));
        }
        if (key == GLFW_KEY_SPACE) {
            return ' ';
        }
        if (key == GLFW_KEY_PERIOD) {
            return '.';
        }
        if (key == GLFW_KEY_SLASH) {
            return '/';
        }
        return 0;
    }

    /**
     * Queues a Unicode character while preserving
     * the legacy getEventCharacter() behaviour.
     *
     * Package-private so it can be unit tested
     * without creating a GLFW window.
     */
    static void enqueueCharacter(int codePoint) {

        if (!Character.isValidCodePoint(codePoint)
                || (codePoint >= Character.MIN_SURROGATE
                        && codePoint <= Character.MAX_SURROGATE)) {
            return;
        }

        // Preserve the previous legacy behaviour:
        // associate a printable character with the
        // most recent key event when possible.
        Event last = ((ArrayDeque<Event>) events).peekLast();

        if (last != null
                && last.state
                && !last.textInputEvent) {

            last.character = (char) codePoint;

        } else {

            // Original fallback for character input
            // without an associated physical key.
            events.add(new Event(
                    KEY_NONE,
                    true,
                    (char) codePoint));
        }

        // Always preserve the complete Unicode value
        // as a separate text-input event.
        events.add(Event.textInput(codePoint));
    }

    private static final class Event {

        final int key;
        final boolean state;

        char character;

        final boolean textInputEvent;
        final int codePoint;

        // Existing physical key event.
        Event(int key, boolean state, char character) {
            this(key, state, character, false, 0);
        }

        private Event(
                int key,
                boolean state,
                char character,
                boolean textInputEvent,
                int codePoint) {

            this.key = key;
            this.state = state;
            this.character = character;
            this.textInputEvent = textInputEvent;
            this.codePoint = codePoint;
        }

        // Unicode character event.
        static Event textInput(int codePoint) {

            return new Event(
                    KEY_NONE,
                    true,
                    (char) codePoint,
                    true,
                    codePoint);
        }
    }

}
