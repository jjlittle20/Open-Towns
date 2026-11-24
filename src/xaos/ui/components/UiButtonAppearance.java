
package xaos.ui.components;

/**
 * Semantic button appearances.
 *
 * The active UI theme decides how each
 * appearance is drawn.
 */
public enum UiButtonAppearance {

    /**
     * Ordinary text, icon or mixed button.
     */
    STANDARD,

    /**
     * Gameplay menu action.
     *
     * Classic Towns uses bottom_item.
     */
    MENU_ACTION,

    /**
     * Button opening another gameplay menu.
     *
     * Classic Towns uses bottom_item_sm.
     */
    MENU_SUBMENU
}
