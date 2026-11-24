package xaos.internal.api.ui;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import xaos.api.ui.UiActionRegistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiActionRegistryTest {

        @AfterEach
        void tearDown() {

                /*
                 * Remove any actions created by a test.
                 */
                for (var action : UiActionRegistry.getActions()) {
                        UiActionRegistry.unregister(
                                        action.id());
                }
        }

        @Test
        void registrationBecomesActive() {

                UiActionRegistration registration = UiActionRegistry.register(
                                "test:action",
                                "Test Action",
                                () -> {
                                });

                assertTrue(
                                registration.isActive());

                assertTrue(
                                UiActionRegistry.hasActions());

                assertEquals(
                                1,
                                UiActionRegistry.getActions()
                                                .size());
        }

        @Test
        void unregisterRemovesAction() {

                UiActionRegistration registration = UiActionRegistry.register(
                                "test:action",
                                "Test Action",
                                () -> {
                                });

                registration.unregister();

                assertFalse(
                                registration.isActive());

                assertFalse(
                                UiActionRegistry.hasActions());
        }

        @Test
        void closeUnregistersAction() {

                UiActionRegistration registration = UiActionRegistry.register(
                                "test:action",
                                "Test Action",
                                () -> {
                                });

                registration.close();

                assertFalse(
                                registration.isActive());

                assertFalse(
                                UiActionRegistry.hasActions());
        }

        @Test
        void actionCanBeExecuted() {

                AtomicInteger executions = new AtomicInteger();

                UiActionRegistry.register(
                                "test:action",
                                "Test Action",
                                executions::incrementAndGet);

                assertTrue(
                                UiActionRegistry.execute(
                                                "test:action"));

                assertEquals(
                                1,
                                executions.get());
        }

        @Test
        void unknownActionCannotBeExecuted() {

                assertFalse(
                                UiActionRegistry.execute(
                                                "test:missing"));
        }

        @Test
        void duplicateIdCannotReplaceExistingAction() {

                AtomicInteger firstExecutions = new AtomicInteger();

                AtomicInteger secondExecutions = new AtomicInteger();

                UiActionRegistration first = UiActionRegistry.register(
                                "test:action",
                                "First",
                                firstExecutions::incrementAndGet);

                IllegalArgumentException error = assertThrows(
                                IllegalArgumentException.class,
                                () -> UiActionRegistry.register(
                                                "test:action",
                                                "Second",
                                                secondExecutions::incrementAndGet));

                assertTrue(
                                error.getMessage().contains(
                                                "test:action"));

                // Original registration remains active.
                assertTrue(first.isActive());

                assertEquals(
                                1,
                                UiActionRegistry.getActions().size());

                assertEquals(
                                "First",
                                UiActionRegistry.getActions()
                                                .get(0).label());

                UiActionRegistry.execute("test:action");

                assertEquals(1, firstExecutions.get());
                assertEquals(0, secondExecutions.get());
        }

        @Test
        void actionIdCanBeReusedAfterUnregister() {

                UiActionRegistration first = UiActionRegistry.register(
                                "test:action",
                                "First",
                                () -> {
                                });

                first.unregister();

                assertFalse(first.isActive());

                // The ID becomes available again when
                // its original owner unregisters.
                UiActionRegistration second = UiActionRegistry.register(
                                "test:action",
                                "Second",
                                () -> {
                                });

                assertTrue(second.isActive());

                // Repeated cleanup of the old handle
                // must not affect the new registration.
                first.unregister();

                assertTrue(second.isActive());

                assertEquals(
                                "Second",
                                UiActionRegistry.getActions()
                                                .get(0).label());

                assertTrue(
                                UiActionRegistry.execute(
                                                "test:action"));
        }

        @Test
        void unregisterByIdDeactivatesRegistration() {

                UiActionRegistration registration = UiActionRegistry.register(
                                "test:action",
                                "Test Action",
                                () -> {
                                });

                UiActionRegistry.unregister(
                                "test:action");

                assertFalse(
                                registration.isActive());

                assertFalse(
                                UiActionRegistry.hasActions());
        }

        @Test
        void differentModsCanUseSameLocalActionName() {

                UiActionRegistration first = UiActionRegistry.register(
                                "mod-a:open",
                                "Open A",
                                () -> {
                                });

                UiActionRegistration second = UiActionRegistry.register(
                                "mod-b:open",
                                "Open B",
                                () -> {
                                });

                assertTrue(first.isActive());
                assertTrue(second.isActive());

                assertEquals(
                                2,
                                UiActionRegistry.getActions().size());
        }

}