
package xaos.ui.integration;

import java.util.Objects;

import xaos.internal.api.ui.UiActionRegistry;
import xaos.panels.menus.MenuItemDefinition;

/**
 * Connects registered Java mod actions to
 * XML-defined gameplay menu entries.
 *
 * Existing Towns commands are delegated to
 * the original gameplay action binding.
 */
public final class UiRegisteredGameplayActionBinding
        implements UiGameplayActionBinding {

    /**
     * XML command used for registered UI actions.
     *
     * Example:
     * command="MOD_ACTION"
     * parameter="example:hello"
     */
    public static final String COMMAND = "MOD_ACTION";

    private final UiGameplayActionBinding legacy;

    public UiRegisteredGameplayActionBinding(
            UiGameplayActionBinding legacy) {

        this.legacy = Objects.requireNonNull(
                legacy, "legacy");
    }

    @Override
    public boolean isAvailable(MenuItemDefinition item) {

        return availability(item).canExecute();
    }

    @Override
    public UiActionAvailability availability(
            MenuItemDefinition item) {

        // Keep original gameplay commands working.
        if (!isModAction(item)) {
            return legacy.availability(item);
        }

        if (!"action".equals(item.getType())) {
            return UiActionAvailability.blocked(
                    "Menu entry is not an action.");
        }

        String id = item.getParameter();

        if (id == null || id.isBlank()) {
            return UiActionAvailability.blocked(
                    "Missing registered UI action ID.");
        }

        if (!id.contains(":")) {
            return UiActionAvailability.blocked(
                    "Mod action ID must be namespaced: "
                            + id);
        }

        if (!UiActionRegistry.isRegistered(id)) {
            return UiActionAvailability.blocked(
                    "Mod action not registered: " + id);
        }

        return UiActionAvailability.allowed();
    }

    @Override
    public void execute(MenuItemDefinition item) {

        // Original Towns commands retain their
        // existing execution behaviour.
        if (!isModAction(item)) {
            legacy.execute(item);
            return;
        }

        UiActionAvailability state = availability(item);

        if (!state.canExecute()) {
            throw new IllegalStateException(
                    state.reason());
        }

        String id = item.getParameter();

        // Recheck registration during execution.
        // A mod may have been unloaded since the
        // button's availability was last refreshed.
        if (!UiActionRegistry.execute(id)) {
            throw new IllegalStateException(
                    "Mod action no longer registered: "
                            + id);
        }
    }

    private static boolean isModAction(
            MenuItemDefinition item) {

        return item != null
                && COMMAND.equalsIgnoreCase(
                        item.getCommand());
    }
}
