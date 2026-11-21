
package xaos.ui.debug;

import java.util.List;
import java.util.Objects;

import xaos.internal.api.mods.ModUiErrors;
import xaos.internal.api.mods.ModUiErrors.Entry;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiLinearLayout;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;
import xaos.ui.rendering.opengl.TownsUiTextMetrics;

/**
 * Non-modal, reusable window for mod errors.
 *
 * All methods except construction are intended to be
 * called on the game/UI thread.
 */
public final class UiModErrorWindow {

    private final UiSystem ui;
    private final UiWindow window;
    private final UiScrollContainer scroll;

    private final UiButton previous;
    private final UiButton next;

    private final UiTextMetrics metrics = TownsUiTextMetrics.INSTANCE;

    private long lastShownId;
    private int selectedIndex;

    public UiModErrorWindow(UiSystem ui) {

        this.ui = Objects.requireNonNull(ui, "ui");

        window = new UiWindow("Mod Errors");

        window.setPosition(new UiPoint(100, 80));
        window.setUserSize(new UiSize(560, 370));

        window.setDraggable(true);
        window.setResizable(true);
        window.setModal(false);

        window.setCloseAction(
                metrics,
                () -> ui.windows().close(window));

        window.content().setSpacing(6);
        window.content().setPadding(UiInsets.all(4));

        // --------------------------------------------------------
        // Error navigation
        // --------------------------------------------------------

        UiContainer navigation = new UiContainer();
        navigation.setLayoutStrategy(
                UiLinearLayout.HORIZONTAL);

        navigation.setSpacing(6);

        previous = new UiButton("Previous", metrics);
        next = new UiButton("Next", metrics);

        previous.setOnClick(() -> {
            selectedIndex = Math.max(0, selectedIndex - 1);
            rebuild(ModUiErrors.snapshot());
        });

        next.setOnClick(() -> {
            List<Entry> errors = ModUiErrors.snapshot();

            selectedIndex = Math.min(
                    errors.size() - 1,
                    selectedIndex + 1);

            rebuild(errors);
        });

        navigation.add(previous);
        navigation.add(next);

        window.content().add(navigation);

        // --------------------------------------------------------
        // Scrollable error details
        // --------------------------------------------------------

        scroll = new UiScrollContainer();

        scroll.content().setSpacing(3);
        scroll.content().setPadding(UiInsets.all(4));

        window.content().add(scroll);

        UiButton close = new UiButton("Close", metrics);

        close.setOnClick(
                () -> ui.windows().close(window));

        window.content().add(close);

        // Do not display anything until an error occurs.
        window.close();

        ui.windows().register(window);
    }

    /**
     * Checks for new errors once per game frame.
     *
     * Never call UI mutations from the mod logger itself.
     */
    public void update() {

        List<Entry> errors = ModUiErrors.snapshot();

        if (errors.isEmpty()) {
            return;
        }

        long newestId = errors.get(
                errors.size() - 1).id();

        if (newestId == lastShownId) {
            return;
        }

        lastShownId = newestId;
        selectedIndex = errors.size() - 1;

        rebuild(errors);
        ui.windows().open(window);
    }

    /**
     * Temporary developer shortcut target.
     * Allows the player to reopen a dismissed window.
     */
    public void toggle() {

        if (window.isOpen()) {
            ui.windows().close(window);
            return;
        }

        List<Entry> errors = ModUiErrors.snapshot();

        if (errors.isEmpty()) {
            return;
        }

        selectedIndex = Math.min(
                selectedIndex,
                errors.size() - 1);

        rebuild(errors);

        ui.windows().open(window);
    }

    public UiWindow window() {
        return window;
    }

    private void rebuild(List<Entry> errors) {

        if (errors.isEmpty()) {
            return;
        }

        selectedIndex = Math.max(
                0,
                Math.min(selectedIndex, errors.size() - 1));

        Entry error = errors.get(selectedIndex);

        window.setTitle(
                "Mod Errors (" + errors.size() + ")");

        previous.setEnabled(selectedIndex > 0);
        next.setEnabled(
                selectedIndex < errors.size() - 1);

        scroll.content().clear();
        scroll.scrollTo(0);

        addLine("Error " + (selectedIndex + 1)
                + " of " + errors.size());

        addLine("Mod: " + error.modId());
        addLine("Stage: " + error.stage());

        addLine("");
        addWrapped(error.message(), 64);

        if (!error.details().isBlank()) {

            addLine("");
            addLine("Exception details:");

            int lines = 0;

            for (String sourceLine : error.details().split("\\R")) {

                // Avoid constructing thousands of
                // labels for a single exception.
                if (lines++ >= 60) {
                    addLine("[Further details omitted]");
                    break;
                }

                addWrapped(sourceLine, 64);
            }
        }
    }

    private void addWrapped(String value, int width) {

        if (value == null || value.isEmpty()) {
            addLine("");
            return;
        }

        String line = value.replace('\t', ' ');

        while (line.length() > width) {
            addLine(line.substring(0, width));
            line = line.substring(width);
        }

        addLine(line);
    }

    private void addLine(String text) {

        scroll.content().add(
                new UiLabel(text, metrics));
    }
}
