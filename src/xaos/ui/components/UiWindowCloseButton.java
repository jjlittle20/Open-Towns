
package xaos.ui.components;

import java.util.Objects;

import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

/**
 * Standard close button for a UI window.
 *
 * Uses the existing button rendering and
 * input handling.
 *
 * The caller supplies the close action, allowing
 * ordinary windows and modal windows to use their
 * appropriate lifecycle methods.
 */
public final class UiWindowCloseButton extends UiButton {

    public static final int SIZE = 20;

    public UiWindowCloseButton(
            UiTextMetrics textMetrics,
            Runnable onClose) {

        super(
                "X",
                Objects.requireNonNull(
                        textMetrics, "textMetrics"));

        setMinimumSize(
                new UiSize(SIZE, SIZE));

        setPreferredSize(
                new UiSize(SIZE, SIZE));

        setPadding(UiInsets.NONE);

        setOnClick(
                Objects.requireNonNull(
                        onClose, "onClose"));
    }
}
