
package xaos.ui.integration;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;
import xaos.utils.Utils;

/**
 * Connects original Towns XML cycle items
 * to their existing command handlers and
 * translated display values.
 *
 * Internal compatibility adapter.
 */
public final class UiLegacyCycleBindings {

    private static final Map<String, String> CYCLE_COMMANDS = Map.of(
            "COMMAND_MM_SWITCH_AUTOSAVE_DAYS",
            CommandPanel.COMMAND_MM_SWITCH_AUTOSAVE_DAYS,

            "COMMAND_MM_SWITCH_SIEGES",
            CommandPanel.COMMAND_MM_SWITCH_SIEGES);

    private final Function<String, String> valueReader;
    private final Consumer<String> commandExecutor;

    /**
     * Injectable constructor for unit tests
     * and alternative setting providers.
     */
    public UiLegacyCycleBindings(
            Function<String, String> valueReader,
            Consumer<String> commandExecutor) {

        this.valueReader = Objects.requireNonNull(
                valueReader, "valueReader");

        this.commandExecutor = Objects.requireNonNull(
                commandExecutor, "commandExecutor");
    }

    /**
     * Connects cycle controls to original Towns
     * state formatting and command execution.
     */
    public static UiLegacyCycleBindings forGame() {

        return new UiLegacyCycleBindings(

                command -> {

                    if (CommandPanel.COMMAND_MM_SWITCH_AUTOSAVE_DAYS
                            .equals(command)) {

                        // Reuse original localisation,
                        // including singular/plural days.
                        return Utils.getDynamicString(
                                "__SAVE_DAYS__");
                    }

                    if (CommandPanel.COMMAND_MM_SWITCH_SIEGES
                            .equals(command)) {

                        // Reuse original translated
                        // difficulty names.
                        return Utils.getDynamicString(
                                "__SIEGES__");
                    }

                    throw new IllegalArgumentException(
                            "Unsupported cycle state: "
                                    + command);
                },

                command -> CommandPanel.executeCommand(
                        command,
                        null,
                        null,
                        null,
                        null,
                        0));
    }

    /**
     * Creates a binding from an XML cycle item.
     *
     * No command is executed until the user
     * activates the component.
     */
    public UiCycleBinding forItem(
            MenuItemDefinition item) {

        Objects.requireNonNull(item, "item");

        if (!"cycle".equals(item.getType())) {
            throw new IllegalArgumentException(
                    "Not a cycle item: "
                            + item.getId());
        }

        String command = CYCLE_COMMANDS.get(item.getCommand());

        if (command == null) {
            throw new IllegalArgumentException(
                    "Unsupported XML cycle command '"
                            + item.getCommand()
                            + "' for item "
                            + item.getId());
        }

        return new UiCycleBinding(

                // Always read the live setting.
                () -> valueReader.apply(command),

                // Delegate wraparound, saving and
                // other behaviour to the original
                // game command.
                () -> commandExecutor.accept(command));
    }
}
