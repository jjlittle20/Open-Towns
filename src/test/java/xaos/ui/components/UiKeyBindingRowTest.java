
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiKeyBindingRowTest {

        private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

        private static final IntFunction<String> KEY_NAMES = key -> switch (key) {
                case 0 -> "Unbound";
                case 17 -> "W";
                case 31 -> "S";
                case 200 -> "UP";
                default -> "Key " + key;
        };

        @Test
        void displaysBothAssignedKeys() {

                UiKeyBindingRow row = new UiKeyBindingRow(
                                "Move Up",
                                FONT,
                                () -> 17,
                                () -> 200,
                                KEY_NAMES,
                                slot -> {
                                });

                assertEquals(
                                "Move Up",
                                row.actionLabel().text());

                assertEquals(
                                "Primary: W",
                                row.primaryButton().text());

                assertEquals(
                                "Secondary: UP",
                                row.secondaryButton().text());
        }

        @Test
        void displaysUnboundKeys() {

                UiKeyBindingRow row = new UiKeyBindingRow(
                                "Move Up",
                                FONT,
                                () -> 0,
                                () -> 0,
                                KEY_NAMES,
                                slot -> {
                                });

                assertEquals(
                                "Primary: Unbound",
                                row.primaryButton().text());

                assertEquals(
                                "Secondary: Unbound",
                                row.secondaryButton().text());
        }

        @Test
        void refreshReadsCurrentKeyAssignments() {

                AtomicInteger primary = new AtomicInteger(17);
                AtomicInteger secondary = new AtomicInteger(200);

                UiKeyBindingRow row = new UiKeyBindingRow(
                                "Move Up",
                                FONT,
                                primary::get,
                                secondary::get,
                                KEY_NAMES,
                                slot -> {
                                });

                assertEquals(
                                "Primary: W",
                                row.primaryButton().text());

                // Simulate an external key reassignment.
                primary.set(31);
                secondary.set(0);

                row.refresh();

                assertEquals(
                                "Primary: S",
                                row.primaryButton().text());

                assertEquals(
                                "Secondary: Unbound",
                                row.secondaryButton().text());
        }

        @Test
        void eachButtonRequestsCorrectCaptureSlot() {

                AtomicInteger requestedSlot = new AtomicInteger(-1);

                UiKeyBindingRow row = new UiKeyBindingRow(
                                "Move Up",
                                FONT,
                                () -> 17,
                                () -> 200,
                                KEY_NAMES,
                                requestedSlot::set);

                row.primaryButton().activate();

                assertEquals(0, requestedSlot.get());

                row.secondaryButton().activate();

                assertEquals(1, requestedSlot.get());
        }

        @Test
        void requestingCaptureDoesNotModifyBindings() {

                AtomicInteger primary = new AtomicInteger(17);
                AtomicInteger secondary = new AtomicInteger(200);

                UiKeyBindingRow row = new UiKeyBindingRow(
                                "Move Up",
                                FONT,
                                primary::get,
                                secondary::get,
                                KEY_NAMES,
                                slot -> {
                                });

                row.primaryButton().activate();
                row.secondaryButton().activate();

                assertEquals(17, primary.get());
                assertEquals(200, secondary.get());
        }

        @Test
        void bothKeySlotsCanReceiveKeyboardFocus() {

                UiKeyBindingRow row = new UiKeyBindingRow(
                                "Move Up",
                                FONT,
                                () -> 17,
                                () -> 200,
                                KEY_NAMES,
                                slot -> {
                                });

                assertTrue(
                                row.primaryButton().canReceiveFocus());

                assertTrue(
                                row.secondaryButton().canReceiveFocus());
        }

        @Test
        void clearButtonsRequestCorrectBindingSlot() {

                AtomicInteger requestedSlot = new AtomicInteger(-1);

                UiKeyBindingRow row = new UiKeyBindingRow(
                                "Move Up",
                                FONT,
                                () -> 17,
                                () -> 200,
                                KEY_NAMES,
                                slot -> {
                                });

                row.setOnClearRequested(
                                requestedSlot::set);

                row.clearPrimaryButton().activate();

                assertEquals(0, requestedSlot.get());

                row.clearSecondaryButton().activate();

                assertEquals(1, requestedSlot.get());
        }

        @Test
        void clearCallbackOnlyChangesSelectedKey() {

                AtomicInteger primary = new AtomicInteger(17);
                AtomicInteger secondary = new AtomicInteger(200);

                AtomicInteger saveCount = new AtomicInteger();

                UiKeyBindingRow row = new UiKeyBindingRow(
                                "Move Up",
                                FONT,
                                primary::get,
                                secondary::get,
                                KEY_NAMES,
                                slot -> {
                                });

                // Test-only assignment implementation.
                // Does not touch the player's settings.
                row.setOnClearRequested(slot -> {

                        if (slot == 0) {
                                primary.set(0);
                        } else {
                                secondary.set(0);
                        }

                        saveCount.incrementAndGet();
                        row.refresh();
                });

                // Clear primary.
                row.clearPrimaryButton().activate();

                assertEquals(0, primary.get());
                assertEquals(200, secondary.get());

                assertEquals(
                                "Primary: Unbound",
                                row.primaryButton().text());

                assertEquals(
                                "Secondary: UP",
                                row.secondaryButton().text());

                assertEquals(1, saveCount.get());

                // Clear secondary.
                row.clearSecondaryButton().activate();

                assertEquals(0, primary.get());
                assertEquals(0, secondary.get());

                assertEquals(
                                "Secondary: Unbound",
                                row.secondaryButton().text());

                assertEquals(2, saveCount.get());
        }

        @Test
        void clearButtonsSupportKeyboardFocus() {

                UiKeyBindingRow row = new UiKeyBindingRow(
                                "Move Up",
                                FONT,
                                () -> 17,
                                () -> 200,
                                KEY_NAMES,
                                slot -> {
                                });

                assertTrue(
                                row.clearPrimaryButton()
                                                .canReceiveFocus());

                assertTrue(
                                row.clearSecondaryButton()
                                                .canReceiveFocus());
        }

}
