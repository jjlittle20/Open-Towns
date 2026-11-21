
package xaos.ui.layout;

/**
 * Defines how a UI window is positioned
 * relative to the logical game viewport.
 *
 * NONE retains normal floating-window behaviour.
 */
public enum UiWindowAnchor {

    NONE,

    TOP,
    BOTTOM,
    LEFT,
    RIGHT,

    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}
