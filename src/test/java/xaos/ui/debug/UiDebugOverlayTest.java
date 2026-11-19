
package xaos.ui.debug;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiSystem;

class UiDebugOverlayTest {

    @Test
    void installingDebugWindowsPreservesExistingGameWindows() {

        UiSystem system = new UiSystem();

        // Simulate a real game-owned window.
        UiWindow gameWindow =
                new UiWindow("Game Window");

        system.windows().register(gameWindow);

        // Install the development windows.
        UiDebugOverlay.install(system);

        assertTrue(
                system.windows().isManaged(gameWindow));

        int count = system.windows().windows().size();

        // Reinstalling must not duplicate windows.
        UiDebugOverlay.install(system);

        assertEquals(
                count,
                system.windows().windows().size());

        // Every window shares the same root.
        for (UiWindow window : system.windows().windows()) {
            assertSame(
                    system.root().content(),
                    window.parent());
        }
    }
}
