
package xaos.ui.rendering;

import java.util.Objects;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiCycleButton;
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiScrollBar;
import xaos.ui.components.UiSlider;
import xaos.ui.components.UiTextField;
import xaos.ui.components.UiToggleButton;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.theme.UiTheme;

/**
 * Default component painter for the Open Towns UI.
 *
 * Supports the standard component types and uses
 * the active theme for their appearance.
 *
 * Does not depend on OpenGL or game logic.
 */
public final class UiStandardPainter
        implements UiElementPainter {

    private final UiSystem system;

    public UiStandardPainter(UiSystem system) {
        this.system = Objects.requireNonNull(
                system, "system");
    }

    @Override
    public void paint(
            UiElement element,
            UiRenderer renderer) {

        UiTheme theme = system.themes().activeTheme();

        // --------------------------------------------------------
        // Windows
        // --------------------------------------------------------

        if (element instanceof UiWindow window) {

            theme.paintWindow(window, renderer);

            UiResizeGripPainter.paint(
                    window,
                    renderer,
                    theme.textColor());

            // --------------------------------------------------------
            // Title bars
            // --------------------------------------------------------

        } else if (element.parent() instanceof UiWindow window
                && element == window.titleBar()) {

            theme.paintTitleBar(window, renderer);

            // --------------------------------------------------------
            // Scrollbars
            // --------------------------------------------------------

        } else if (element instanceof UiScrollBar bar) {

            UiScrollBarPainter.paint(
                    bar,
                    renderer,
                    theme.textColor());

            // --------------------------------------------------------
            // Labels
            // --------------------------------------------------------

        } else if (element instanceof UiLabel label) {

            UiRect bounds = label.bounds();

            renderer.drawText(
                    label.text(),
                    new UiPoint(bounds.x(), bounds.y()),
                    theme.textColor());

        } else if (element instanceof UiSlider slider) {

            // Read the current setting in case it changed
            // outside the UI.
            slider.refresh();

            UiSliderPainter.paint(
                    slider,
                    renderer,
                    theme.textColor());

            // Reuse the framework's focus indicator.
            if (system.input()
                    .focusManager()
                    .isFocused(slider)) {

                UiFocusPainter.paint(
                        slider,
                        renderer,
                        theme.focusColor());
            }

            // --------------------------------------------------------
            // Buttons
            // --------------------------------------------------------

        } else if (element instanceof UiButton button) {

            // Toggle state may change outside the UI,
            // for example through a fullscreen hotkey.
            //
            // Refresh before painting so the displayed
            // state always reflects the actual setting.
            if (button instanceof UiToggleButton toggle) {
                toggle.refresh();
            }

            // Cycle values can also change outside this UI.
            if (button instanceof UiCycleButton cycle) {
                cycle.refresh();
            }

            theme.paintButton(button, renderer);

            // --------------------------------------------------------
            // Text fields
            // --------------------------------------------------------

        } else if (element instanceof UiTextField field) {

            UiTextFieldPainter.paint(
                    field,
                    renderer,
                    system.input()
                            .focusManager()
                            .isFocused(field),
                    theme.textColor(),
                    theme.focusColor());
        }
    }
}
