
package xaos.ui.core;

/**
 * Implemented by UI controls that can receive
 * keyboard focus.
 *
 * A control may temporarily reject focus,
 * for example while disabled.
 */
public interface UiFocusable {

    /**
     * Returns whether this control currently
     * accepts keyboard focus.
     */
    boolean canReceiveFocus();
}
