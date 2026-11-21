
package xaos.ui.components;

import java.util.Objects;

import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

/**
 * Standard Lock/Unlock title-bar button.
 *
 * Prevents the player from moving or resizing
 * its owning window while leaving the window's
 * contents interactive.
 */
public final class UiWindowLockButton extends UiButton {

    public UiWindowLockButton(
            UiWindow window,
            UiTextMetrics metrics) {

        super(
                "Lock",
                Objects.requireNonNull(metrics, "metrics"));

        Objects.requireNonNull(window, "window");

        setMinimumSize(new UiSize(68, 20));
        setPreferredSize(new UiSize(68, 20));
        setPadding(UiInsets.NONE);

        setOnClick(() -> window.setLocked(!window.locked()));

        refresh(window);
    }

    /**
     * Updates the button to reflect its window's
     * current locked state.
     */
    public void refresh(UiWindow window) {

        boolean locked = window.locked();

        setText(locked ? "Unlock" : "Lock");
        setSelected(locked);
    }
}
