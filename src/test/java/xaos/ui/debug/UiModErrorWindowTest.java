
package xaos.ui.debug;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xaos.internal.api.mods.ModUiErrors;
import xaos.ui.core.UiSystem;

class UiModErrorWindowTest {

    @Test
    void newErrorsOpenWindowAndDismissalIsPreserved() {

        ModUiErrors.clear();

        UiSystem ui = new UiSystem();

        UiModErrorWindow errors = new UiModErrorWindow(ui);

        assertFalse(errors.window().isOpen());

        ModUiErrors.report(
                "examplemod",
                "Menu loading",
                "Invalid menu XML");

        errors.update();

        assertTrue(errors.window().isOpen());

        // Dismiss the popup.
        errors.toggle();

        assertFalse(errors.window().isOpen());

        // No new error: stay dismissed.
        errors.update();

        assertFalse(errors.window().isOpen());

        // Another failure should reopen it.
        ModUiErrors.report(
                "othermod",
                "Mod callback",
                "Something failed");

        errors.update();

        assertTrue(errors.window().isOpen());
        assertTrue(
                errors.window().title().contains("(2)"));
    }
}
