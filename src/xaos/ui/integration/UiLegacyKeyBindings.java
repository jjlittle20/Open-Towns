
package xaos.ui.integration;

import java.util.Objects;

import xaos.panels.menus.MenuItemDefinition;
import xaos.platform.lwjgl3.input.Keyboard;
import xaos.ui.components.UiKeyBindingRow;
import xaos.ui.input.UiInputRouter;
import xaos.ui.rendering.UiTextMetrics;
import xaos.utils.UtilsKeyboard;

/**
 * Connects original Towns keyboard definitions
 * to the reusable UI key-binding controls.
 *
 * This is a compatibility adapter, not part
 * of the general-purpose UI component system.
 */
public final class UiLegacyKeyBindings {

        private final UiInputRouter input;
        private final UiTextMetrics metrics;
        private final Runnable onBindingsChanged;

        public UiLegacyKeyBindings(
                        UiInputRouter input,
                        UiTextMetrics metrics,
                        Runnable onBindingsChanged) {

                this.input = Objects.requireNonNull(
                                input, "input");

                this.metrics = Objects.requireNonNull(
                                metrics, "metrics");

                this.onBindingsChanged = Objects.requireNonNull(
                                onBindingsChanged, "onBindingsChanged");
        }

        /**
         * Creates a live keyboard-binding row
         * from an original XML item.
         */
        public UiKeyBindingRow forItem(
                        MenuItemDefinition item) {

                Objects.requireNonNull(item, "item");

                if (!"keyboard".equals(item.getType())) {
                        throw new IllegalArgumentException(
                                        "Not a keyboard item: "
                                                        + item.getId());
                }

                int functionId = resolveFunctionId(
                                item.getKey());

                UiKeyBindingRow row = new UiKeyBindingRow(
                                UtilsKeyboard.getFNHumanString(functionId),
                                metrics,

                                // Read current assignments, not copies.
                                () -> UtilsKeyboard.getKey(functionId, 0),

                                () -> UtilsKeyboard.getKey(functionId, 1),

                                keyCode -> keyCode == Keyboard.KEY_NONE
                                                ? "Unbound"
                                                : Keyboard.getKeyName(keyCode),

                                // Replaced by the capture binder below.
                                slot -> {
                                });

                UiKeyBindingCaptureBinder.bind(
                                row,
                                input,
                                Keyboard.KEY_ESCAPE,

                                // Preserve the original game's
                                // duplicate handling and persistence.
                                (slot, keyCode) -> UtilsKeyboard.redefineKey(
                                                slot, functionId, keyCode),

                                // Other rows may also have changed.
                                onBindingsChanged);

                // --------------------------------------------------------
                // Clear an individual key assignment
                // --------------------------------------------------------

                row.setOnClearRequested(slot -> {

                        // Nothing to clear.
                        // Avoid unnecessary settings writes.
                        if (UtilsKeyboard.getKey(
                                        functionId, slot) == Keyboard.KEY_NONE) {

                                return;
                        }

                        // The existing keyboard system handles
                        // clearing and saving the new assignment.
                        UtilsKeyboard.redefineKey(
                                        slot,
                                        functionId,
                                        Keyboard.KEY_NONE);

                        // Refresh this row immediately.
                        row.refresh();

                        // Other rows might need to update too.
                        onBindingsChanged.run();
                });

                return row;
        }

        /**
         * Converts an XML symbolic shortcut name
         * into the original Towns function ID.
         *
         * Uses the game's existing function table
         * rather than maintaining a second hardcoded map.
         */
        public static int resolveFunctionId(
                        String symbol) {

                if (symbol == null || symbol.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Missing keyboard function name");
                }

                // The original XML uses this constant name,
                // while UtilsKeyboard's persisted name is
                // FN_ITEM_BUILD_FACE.
                if ("FN_TOGGLE_ITEM_BUILD_FACE".equals(symbol)) {
                        return UtilsKeyboard.FN_TOGGLE_ITEM_BUILD_FACE;
                }

                for (int functionId = 0;; functionId++) {

                        String name = UtilsKeyboard.getFNString(functionId);

                        if (name == null) {
                                break;
                        }

                        if (name.equals(symbol)) {
                                return functionId;
                        }
                }

                throw new IllegalArgumentException(
                                "Unknown keyboard function: " + symbol);
        }
}
