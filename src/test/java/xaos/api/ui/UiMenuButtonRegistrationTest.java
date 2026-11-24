
package xaos.api.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import xaos.internal.api.ui.DefaultUiApi;
import xaos.internal.api.ui.UiActionRegistry;
import xaos.internal.api.ui.UiRuntimeMenuActions;
import xaos.panels.UI.UiDispatcher;
import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuManager;

class UiMenuButtonRegistrationTest {

    private static final String ROOT = "game.right.root";

    private UiMenuButtonRegistration registration;

    private static MenuManager manager() {

        MenuManager manager = new MenuManager();

        manager.registerMenu(
                new MenuDefinition(
                        ROOT,
                        null,
                        "Construction"));

        return manager;
    }

    @AfterEach
    void cleanup() {

        if (registration != null) {
            registration.unregister();
        }

        UiDispatcher.processPending();

        UiRuntimeMenuActions.bind(
                null, () -> {
                });

        UiDispatcher.processPending();
    }

    @Test
    void createsBothButtonAndCallback() {

        MenuManager menus = manager();

        UiRuntimeMenuActions.bind(
                menus, () -> {
                });

        UiApi api = new DefaultUiApi();

        AtomicInteger executions = new AtomicInteger();

        registration = api.registerMenuButton(
                ROOT,
                "test:combined-button",
                "Combined Button",
                executions::incrementAndGet);

        UiDispatcher.processPending();

        assertTrue(registration.isActive());

        assertTrue(
                UiActionRegistry.isRegistered(
                        "test:combined-button"));

        assertTrue(
                menus.requireMenu(ROOT)
                        .getItems()
                        .stream()
                        .anyMatch(item -> item.getId().equals(
                                "test:combined-button")));

        assertTrue(
                UiActionRegistry.execute(
                        "test:combined-button"));

        assertEquals(1, executions.get());
    }

    @Test
    void disposingRemovesBothRegistrations() {

        MenuManager menus = manager();

        UiRuntimeMenuActions.bind(
                menus, () -> {
                });

        UiApi api = new DefaultUiApi();

        registration = api.registerMenuButton(
                ROOT,
                "test:dispose-button",
                "Dispose Button",
                () -> {
                });

        UiDispatcher.processPending();

        registration.unregister();
        UiDispatcher.processPending();

        assertFalse(registration.isActive());

        assertFalse(
                UiActionRegistry.isRegistered(
                        "test:dispose-button"));

        assertTrue(
                menus.requireMenu(ROOT)
                        .getItems()
                        .isEmpty());

        // Repeated cleanup must be harmless.
        assertDoesNotThrow(
                registration::unregister);
    }

    @Test
    void failedMenuRegistrationCleansUpAction() {

        UiApi api = new DefaultUiApi();

        // Null menu ID fails synchronously during
        // menu registration.
        assertThrows(
                IllegalArgumentException.class,
                () -> api.registerMenuButton(
                        null,
                        "test:failed-button",
                        "Failed Button",
                        () -> {
                        }));

        assertFalse(
                UiActionRegistry.isRegistered(
                        "test:failed-button"));
    }
}
