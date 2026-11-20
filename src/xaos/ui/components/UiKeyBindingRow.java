
package xaos.ui.components;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

import xaos.ui.core.UiContainer;
import xaos.ui.layout.UiLinearLayout;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiSizePolicy;
import xaos.ui.rendering.UiTextMetrics;

/**
 * Displays an action and two assignable key slots.
 *
 * This component does not own keyboard assignments.
 * The game or mod provides the current values,
 * key names and capture callback.
 */
public final class UiKeyBindingRow extends UiContainer {

        private final UiLabel actionLabel;

        private final UiButton primaryButton;
        private final UiButton secondaryButton;
        private final UiButton clearPrimaryButton;
        private final UiButton clearSecondaryButton;

        // Clearing is optional until a caller supplies
        // an actual binding implementation.
        private IntConsumer onClearRequested = slot -> {
        };

        private final IntSupplier primaryKey;
        private final IntSupplier secondaryKey;
        private final IntFunction<String> keyName;
        private IntConsumer onCaptureRequested;

        // -1 means neither key slot is capturing.
        private int capturingSlot = -1;

        /**
         * onCaptureRequested receives:
         * 0 = primary key
         * 1 = secondary key
         */
        public UiKeyBindingRow(
                        String actionName,
                        UiTextMetrics metrics,
                        IntSupplier primaryKey,
                        IntSupplier secondaryKey,
                        IntFunction<String> keyName,
                        IntConsumer onCaptureRequested) {

                Objects.requireNonNull(actionName, "actionName");
                Objects.requireNonNull(metrics, "metrics");

                this.primaryKey = Objects.requireNonNull(
                                primaryKey, "primaryKey");

                this.secondaryKey = Objects.requireNonNull(
                                secondaryKey, "secondaryKey");

                this.keyName = Objects.requireNonNull(
                                keyName, "keyName");
                this.onCaptureRequested = Objects.requireNonNull(
                                onCaptureRequested, "onCaptureRequested");

                // Each binding uses a label above two key buttons.
                setLayoutStrategy(UiLinearLayout.VERTICAL);
                setHorizontalSizePolicy(UiSizePolicy.FILL);
                setSpacing(3);

                actionLabel = new UiLabel(actionName, metrics);
                add(actionLabel);

                // --------------------------------------------------------
                // Key buttons
                // --------------------------------------------------------

                UiContainer slots = new UiContainer();

                slots.setLayoutStrategy(UiLinearLayout.HORIZONTAL);
                slots.setHorizontalSizePolicy(UiSizePolicy.FILL);
                slots.setSpacing(6);

                primaryButton = new UiButton("Primary", metrics);
                secondaryButton = new UiButton("Secondary", metrics);

                primaryButton.setHorizontalSizePolicy(
                                UiSizePolicy.FILL);

                secondaryButton.setHorizontalSizePolicy(
                                UiSizePolicy.FILL);

                primaryButton.setPreferredSize(
                                new UiSize(140, 28));

                secondaryButton.setPreferredSize(
                                new UiSize(140, 28));

                primaryButton.setOnClick(
                                () -> this.onCaptureRequested.accept(0));

                secondaryButton.setOnClick(
                                () -> this.onCaptureRequested.accept(1));

                // --------------------------------------------------------
                // Clear buttons
                // --------------------------------------------------------

                clearPrimaryButton = new UiButton(
                                "Clear", metrics);

                clearSecondaryButton = new UiButton(
                                "Clear", metrics);

                clearPrimaryButton.setPreferredSize(
                                new UiSize(64, 28));

                clearSecondaryButton.setPreferredSize(
                                new UiSize(64, 28));

                clearPrimaryButton.setOnClick(
                                () -> onClearRequested.accept(0));

                clearSecondaryButton.setOnClick(
                                () -> onClearRequested.accept(1));

                // Keep each Clear button immediately
                // beside the key slot it belongs to.
                slots.add(primaryButton);
                slots.add(clearPrimaryButton);

                slots.add(secondaryButton);
                slots.add(clearSecondaryButton);

                add(slots);

                refresh();
        }

        // ------------------------------------------------------------
        // Component access
        // ------------------------------------------------------------

        public UiLabel actionLabel() {
                return actionLabel;
        }

        public UiButton primaryButton() {
                return primaryButton;
        }

        public UiButton secondaryButton() {
                return secondaryButton;
        }

        public UiButton clearPrimaryButton() {
                return clearPrimaryButton;
        }

        public UiButton clearSecondaryButton() {
                return clearSecondaryButton;
        }

        /**
         * Registers an action that clears an assigned key.
         *
         * 0 = primary binding
         * 1 = secondary binding
         *
         * This UI component does not own persistence.
         */
        public void setOnClearRequested(
                        IntConsumer handler) {

                onClearRequested = Objects.requireNonNull(
                                handler, "handler");
        }

        /**
         * Replaces the handler called when the player
         * selects a primary or secondary key slot.
         */
        public void setOnCaptureRequested(
                        IntConsumer handler) {

                this.onCaptureRequested = Objects.requireNonNull(
                                handler, "handler");
        }

        /**
         * Shows the capture prompt for one slot.
         *
         * 0 = primary
         * 1 = secondary
         */
        public void beginCapture(int slot) {

                if (slot != 0 && slot != 1) {
                        throw new IllegalArgumentException(
                                        "Invalid key slot: " + slot);
                }

                capturingSlot = slot;
                refresh();
        }

        /**
         * Ends the visual capture state and restores
         * the current assigned key names.
         */
        public void endCapture() {

                capturingSlot = -1;
                refresh();
        }

        public int capturingSlot() {
                return capturingSlot;
        }

        // ------------------------------------------------------------
        // Display
        // ------------------------------------------------------------

        public void refresh() {

                primaryButton.setText(
                                capturingSlot == 0
                                                ? "Press a key..."
                                                : "Primary: " + displayKey(
                                                                primaryKey.getAsInt()));

                secondaryButton.setText(
                                capturingSlot == 1
                                                ? "Press a key..."
                                                : "Secondary: " + displayKey(
                                                                secondaryKey.getAsInt()));
        }

        private String displayKey(int code) {

                return Objects.requireNonNull(
                                keyName.apply(code),
                                "Key name cannot be null");
        }
}
