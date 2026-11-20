
package xaos.ui.integration;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import xaos.panels.CommandPanel;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuStateResolver;

/**
 * Connects original Towns XML toggle items
 * to their existing command and state systems.
 *
 * Converts XML command field names into the
 * command values expected by CommandPanel.
 *
 * This is an internal compatibility adapter,
 * not the public modding API.
 */
public final class UiLegacyToggleBindings {

    /**
     * Explicitly supported boolean settings.
     *
     * Keys are symbolic names from XML.
     * Values are actual command identifiers.
     *
     * Unknown commands are rejected rather than
     * silently displaying an incorrect state.
     */
    private static final Map<String, String> BOOLEAN_COMMANDS = Map.ofEntries(

            // Graphics
            Map.entry(
                    "COMMAND_MM_TOGGLE_FULL_SCREEN",
                    CommandPanel.COMMAND_MM_TOGGLE_FULL_SCREEN),

            // Game
            Map.entry(
                    "COMMAND_MM_SWITCH_MOUSE_SCROLL",
                    CommandPanel.COMMAND_MM_SWITCH_MOUSE_SCROLL),

            Map.entry(
                    "COMMAND_MM_SWITCH_MOUSE_SCROLL_EARS",
                    CommandPanel.COMMAND_MM_SWITCH_MOUSE_SCROLL_EARS),

            Map.entry(
                    "COMMAND_MM_SWITCH_MOUSE_2D_CUBES",
                    CommandPanel.COMMAND_MM_SWITCH_MOUSE_2D_CUBES),

            Map.entry(
                    "COMMAND_MM_SWITCH_DISABLE_ITEMS",
                    CommandPanel.COMMAND_MM_SWITCH_DISABLE_ITEMS),

            Map.entry(
                    "COMMAND_MM_SWITCH_PAUSE",
                    CommandPanel.COMMAND_MM_SWITCH_PAUSE),

            Map.entry(
                    "COMMAND_MM_SWITCH_SIEGE_PAUSE",
                    CommandPanel.COMMAND_MM_SWITCH_SIEGE_PAUSE),

            Map.entry(
                    "COMMAND_MM_SWITCH_CARAVAN_PAUSE",
                    CommandPanel.COMMAND_MM_SWITCH_CARAVAN_PAUSE),

            // Audio
            Map.entry(
                    "COMMAND_MM_SWITCH_MUSIC",
                    CommandPanel.COMMAND_MM_SWITCH_MUSIC),

            Map.entry(
                    "COMMAND_MM_SWITCH_FX",
                    CommandPanel.COMMAND_MM_SWITCH_FX));

    private final Predicate<String> stateReader;
    private final Consumer<String> commandExecutor;

    /**
     * Constructor with injectable dependencies.
     *
     * Allows bindings to be tested without
     * executing actual game commands.
     */
    public UiLegacyToggleBindings(
            Predicate<String> stateReader,
            Consumer<String> commandExecutor) {

        this.stateReader = Objects.requireNonNull(
                stateReader, "stateReader");

        this.commandExecutor = Objects.requireNonNull(
                commandExecutor, "commandExecutor");
    }

    /**
     * Creates bindings connected to the original
     * Towns settings and command dispatcher.
     */
    public static UiLegacyToggleBindings forGame() {

        return new UiLegacyToggleBindings(
                MenuStateResolver::getBoolean,
                command -> CommandPanel.executeCommand(
                        command,
                        null,
                        null,
                        null,
                        null,
                        0));
    }

    /**
     * Returns the state and action for an XML
     * toggle item.
     *
     * State is read when requested, not cached.
     * The command executes only when activated.
     */
    public UiToggleBinding forItem(
            MenuItemDefinition item) {

        Objects.requireNonNull(item, "item");

        if (!"toggle".equals(item.getType())) {
            throw new IllegalArgumentException(
                    "Not a toggle item: "
                            + item.getId());
        }

        String command = BOOLEAN_COMMANDS.get(
                item.getCommand());

        if (command == null) {
            throw new IllegalArgumentException(
                    "Unsupported XML toggle command '"
                            + item.getCommand()
                            + "' for item "
                            + item.getId());
        }

        return new UiToggleBinding(

                // Always read current game state.
                () -> stateReader.test(command),

                // Execute the existing game command.
                () -> commandExecutor.accept(command));
    }
}
