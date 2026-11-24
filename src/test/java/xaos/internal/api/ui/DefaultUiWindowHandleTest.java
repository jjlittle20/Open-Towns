
package xaos.internal.api.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import xaos.api.ui.UiWindowHandle;
import xaos.panels.UI.UiDispatcher;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class DefaultUiWindowHandleTest {

    private final List<UiWindowHandle> handles = new ArrayList<>();

    private final UiTextMetrics metrics = text -> new UiSize(
            text.length() * 8, 16);

    private UiWindowHandle create(
            UiSystem ui,
            String id) {

        UiWindowHandle handle = DefaultUiWindowHandle.create(
                ui, metrics, id, "Test Window");

        handles.add(handle);
        return handle;
    }

    @AfterEach
    void cleanup() {

        for (UiWindowHandle handle : handles) {
            handle.close();
        }

        UiDispatcher.processPending();
    }

    @Test
    void createsWindowHidden() {

        UiSystem ui = new UiSystem();

        UiWindowHandle handle = create(ui, "testmod:hidden");

        UiDispatcher.processPending();

        assertTrue(handle.isActive());
        assertFalse(handle.isOpen());

        assertEquals(
                1,
                ui.windows().windows().size());
    }

    @Test
    void windowCanBeShownAndHidden() {

        UiSystem ui = new UiSystem();

        UiWindowHandle handle = create(ui, "testmod:visibility");

        handle.show();
        UiDispatcher.processPending();

        assertTrue(handle.isOpen());

        handle.hide();
        UiDispatcher.processPending();

        assertFalse(handle.isOpen());

        // Hiding does not unregister the window.
        assertEquals(
                1,
                ui.windows().windows().size());

        handle.show();
        UiDispatcher.processPending();

        assertTrue(handle.isOpen());
    }

    @Test
    void buttonsExecuteTheirCallbacks() {

        UiSystem ui = new UiSystem();
        AtomicInteger clicks = new AtomicInteger();

        UiWindowHandle handle = create(ui, "testmod:buttons");

        handle.addLabel("Hello");
        handle.addButton(
                "Click Me",
                clicks::incrementAndGet);

        handle.show();

        UiDispatcher.processPending();

        UiWindow window = ui.windows().windows().get(0);

        assertEquals(
                2,
                window.content().children().size());

        UiButton button = (UiButton) window.content()
                .children().get(1);

        button.activate();

        assertEquals(1, clicks.get());
    }

    @Test
    void disposalUnregistersWindow() {

        UiSystem ui = new UiSystem();

        UiWindowHandle handle = create(ui, "testmod:dispose");

        handle.show();
        UiDispatcher.processPending();

        assertEquals(
                1,
                ui.windows().windows().size());

        handle.close();
        UiDispatcher.processPending();

        assertFalse(handle.isActive());
        assertFalse(handle.isOpen());

        assertTrue(
                ui.windows().windows().isEmpty());

        // Repeated disposal is safe.
        assertDoesNotThrow(handle::close);

        assertThrows(
                IllegalStateException.class,
                handle::show);
    }

    @Test
    void disposingBeforeRegistrationIsSafe() {

        UiSystem ui = new UiSystem();

        UiWindowHandle handle = create(ui, "testmod:early-close");

        // Window creation is still queued.
        handle.close();

        UiDispatcher.processPending();

        assertTrue(
                ui.windows().windows().isEmpty());
    }

    @Test
    void duplicateWindowIdsAreRejected() {

        UiSystem ui = new UiSystem();

        UiWindowHandle first = create(ui, "testmod:unique");

        assertThrows(
                IllegalArgumentException.class,
                () -> DefaultUiWindowHandle.create(
                        ui,
                        metrics,
                        "testmod:unique",
                        "Duplicate"));

        first.close();
        UiDispatcher.processPending();

        // The namespace can be reused after cleanup.
        UiWindowHandle second = create(ui, "testmod:unique");

        assertTrue(second.isActive());
    }
}
