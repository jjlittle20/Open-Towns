
package xaos.internal.api.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import xaos.api.ui.UiWindowHandle;
import xaos.internal.api.mods.ModUiErrors;
import xaos.panels.UI.UiDispatcher;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

/**
 * Internal implementation of public mod windows.
 *
 * Uses the same UiSystem as the built-in game UI.
 */
public final class DefaultUiWindowHandle
        implements UiWindowHandle {

    private static final Map<String, DefaultUiWindowHandle> ACTIVE_WINDOWS = new HashMap<>();

    private final String id;
    private final String modId;
    private final UiSystem ui;
    private final UiTextMetrics metrics;
    private final UiWindow window;

    private final AtomicBoolean disposed = new AtomicBoolean(false);

    private DefaultUiWindowHandle(
            UiSystem ui,
            UiTextMetrics metrics,
            String id,
            String title) {

        this.ui = Objects.requireNonNull(ui, "ui");

        this.metrics = Objects.requireNonNull(
                metrics, "metrics");

        this.id = id;
        this.modId = id.substring(0, id.indexOf(':'));

        window = new UiWindow(title);

        window.setCentered(true);
        window.setUserSize(new UiSize(340, 180));

        window.setDraggable(true);
        window.setResizable(true);
        window.setModal(false);

        window.setCloseAction(
                metrics,
                this::hide);

        window.content().setSpacing(8);

        // Windows start hidden.
        window.close();
    }

    /**
     * Internal factory.
     *
     * Namespaced IDs must be unique among active
     * mod-owned windows.
     */
    public static UiWindowHandle create(
            UiSystem ui,
            UiTextMetrics metrics,
            String id,
            String title) {

        Objects.requireNonNull(ui, "ui");
        Objects.requireNonNull(metrics, "metrics");

        if (id == null
                || id.indexOf(':') <= 0
                || id.endsWith(":")) {

            throw new IllegalArgumentException(
                    "Window ID must be namespaced, "
                            + "for example example:inventory");
        }

        if (title == null || title.isBlank()) {

            throw new IllegalArgumentException(
                    "Window title cannot be blank");
        }

        synchronized (ACTIVE_WINDOWS) {

            if (ACTIVE_WINDOWS.containsKey(id)) {

                throw new IllegalArgumentException(
                        "UI window ID already registered: "
                                + id);
            }

            DefaultUiWindowHandle handle = new DefaultUiWindowHandle(
                    ui, metrics, id, title);

            ACTIVE_WINDOWS.put(id, handle);

            UiDispatcher.invokeLater(() -> {

                // A mod may unload before the queued
                // registration reaches the game thread.
                if (!handle.disposed.get()) {

                    handle.ui.windows().register(
                            handle.window);
                }
            });

            return handle;
        }
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isActive() {
        return !disposed.get();
    }

    @Override
    public boolean isOpen() {

        return !disposed.get()
                && window.isOpen();
    }

    @Override
    public void setTitle(String title) {

        requireActive();

        String next = Objects.requireNonNull(
                title, "title");

        if (next.isBlank()) {
            throw new IllegalArgumentException(
                    "Window title cannot be blank");
        }

        UiDispatcher.invokeLater(() -> {

            if (!disposed.get()) {
                window.setTitle(next);
            }
        });
    }

    @Override
    public void addLabel(String text) {

        requireActive();

        String label = Objects.requireNonNull(
                text, "text");

        UiDispatcher.invokeLater(() -> {

            if (!disposed.get()) {

                window.content().add(
                        new UiLabel(label, metrics));
            }
        });
    }

    @Override
    public void addButton(
            String text,
            Runnable onClick) {

        requireActive();

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Button text cannot be blank");
        }

        Objects.requireNonNull(
                onClick, "onClick");

        UiDispatcher.invokeLater(() -> {

            if (disposed.get()) {
                return;
            }

            UiButton button = new UiButton(text, metrics);

            button.setOnClick(() -> {

                if (disposed.get()) {
                    return;
                }

                try {

                    onClick.run();

                } catch (RuntimeException exception) {

                    ModUiErrors.report(
                            modId,
                            "Mod window button",
                            "Button failed in window "
                                    + id,
                            exception);
                }
            });

            window.content().add(button);
        });
    }

    @Override
    public void show() {

        requireActive();

        UiDispatcher.invokeLater(() -> {

            if (!disposed.get()
                    && ui.windows().isManaged(window)) {

                ui.windows().open(window);
            }
        });
    }

    @Override
    public void hide() {

        requireActive();

        UiDispatcher.invokeLater(() -> {

            if (!disposed.get()
                    && ui.windows().isManaged(window)) {

                // Release focus or pointer capture
                // before hiding the window.
                ui.cancelInput();

                ui.windows().close(window);
            }
        });
    }

    @Override
    public void close() {

        // Idempotent disposal.
        if (!disposed.compareAndSet(false, true)) {
            return;
        }

        synchronized (ACTIVE_WINDOWS) {

            ACTIVE_WINDOWS.remove(id, this);
        }

        UiDispatcher.invokeLater(() -> {

            if (ui.windows().isManaged(window)) {

                // Important: cancel input before
                // detaching any UI elements.
                ui.cancelInput();

                ui.windows().unregister(window);
            }
        });
    }

    private void requireActive() {

        if (disposed.get()) {

            throw new IllegalStateException(
                    "UI window has been disposed: "
                            + id);
        }
    }
}
