
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import xaos.api.ui.UiActionRegistration;
import xaos.internal.api.ui.UiActionRegistry;
import xaos.panels.menus.MenuItemDefinition;

class UiRegisteredGameplayActionBindingTest {

    private UiActionRegistration registration;

    @AfterEach
    void cleanup() {

        if (registration != null) {
            registration.unregister();
        }
    }

    private static MenuItemDefinition action(
            String command,
            String parameter) {

        return new MenuItemDefinition(
                "example:test-button",
                "action",
                null,
                null,
                command,
                false,
                true,
                null,
                null,
                null,
                null,
                true,
                "Test Action",
                null,
                parameter,
                null);
    }

    private static UiRegisteredGameplayActionBinding binding() {

        return new UiRegisteredGameplayActionBinding(
                UiGameplayActionBinding.disabled());
    }

    @Test
    void registeredModActionIsAvailable() {

        registration = UiActionRegistry.register(
                "example:hello",
                "Say Hello",
                () -> {
                });

        var result = binding().availability(
                action("MOD_ACTION", "example:hello"));

        assertTrue(result.canExecute());
    }

    @Test
    void missingModActionIsDisabledWithReason() {

        var result = binding().availability(
                action("MOD_ACTION", "example:missing"));

        assertFalse(result.canExecute());

        assertTrue(result.reason().contains(
                "example:missing"));
    }

    @Test
    void registeredActionExecutesCallback() {

        AtomicInteger executions = new AtomicInteger();

        registration = UiActionRegistry.register(
                "example:hello",
                "Say Hello",
                executions::incrementAndGet);

        binding().execute(
                action("MOD_ACTION", "example:hello"));

        assertEquals(1, executions.get());
    }

    @Test
    void unregisterDisablesAction() {

        registration = UiActionRegistry.register(
                "example:hello",
                "Say Hello",
                () -> {
                });

        var binding = binding();
        var item = action(
                "MOD_ACTION", "example:hello");

        assertTrue(binding.isAvailable(item));

        registration.unregister();

        assertFalse(binding.isAvailable(item));

        assertThrows(
                IllegalStateException.class,
                () -> binding.execute(item));
    }

    @Test
    void originalCommandsStillUseLegacyBinding() {

        AtomicInteger executions = new AtomicInteger();

        UiGameplayActionBinding legacy = new UiGameplayActionBinding() {

            @Override
            public boolean isAvailable(
                    MenuItemDefinition item) {

                return true;
            }

            @Override
            public void execute(
                    MenuItemDefinition item) {

                executions.incrementAndGet();
            }
        };

        var binding = new UiRegisteredGameplayActionBinding(
                legacy);

        var item = action(
                "QUEUEANDPLACEAREA", "qlogwall");

        assertTrue(binding.isAvailable(item));

        binding.execute(item);

        assertEquals(1, executions.get());
    }
}
