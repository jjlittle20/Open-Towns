
package xaos.ui.layout;

/**
 * Controls whether an anchored window stretches
 * along the edge of the game viewport.
 *
 * NONE - Keep normal window dimensions.
 * FILL_HEIGHT - Fill height for LEFT/RIGHT anchors.
 * FILL_WIDTH - Fill width for TOP/BOTTOM anchors.
 */
public enum UiWindowDockMode {

    NONE,
    FILL_HEIGHT,
    FILL_WIDTH
}
