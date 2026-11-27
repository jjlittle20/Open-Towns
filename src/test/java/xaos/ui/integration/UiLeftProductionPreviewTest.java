
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import xaos.api.ui.UiActionRegistration;
import xaos.internal.api.ui.UiActionRegistry;
import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;

import xaos.ui.components.UiButton;
import xaos.ui.components.UiLabel;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiGridLayout;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiSize;
import xaos.ui.layout.UiWindowAnchor;
import xaos.ui.layout.UiWindowDockMode;
import xaos.ui.rendering.UiTextMetrics;

class UiLeftProductionPreviewTest {

        private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

        private static MenuItemDefinition item(
                        String id,
                        String type,
                        String target,
                        String command,
                        String parameter,
                        String title) {

                return new MenuItemDefinition(
                                id, type, null, target, command,
                                false, true, null, null,
                                null, null, true,
                                title, null, parameter, null);
        }

        private static MenuManager menus() {

                MenuManager manager = new MenuManager();

                MenuDefinition root = new MenuDefinition(
                                "game.left.production.root",
                                null, "Production");

                root.addItem(item(
                                "game.left.production.food",
                                "submenu",
                                "game.left.production.food",
                                null, null, "Food"));

                MenuDefinition food = new MenuDefinition(
                                "game.left.production.food",
                                null, "Food");

                food.addItem(item(
                                "game.left.production.food.bread",
                                "action", null,
                                "QUEUE", "qbread", null));

                food.addItem(item(
                                "game.left.production.food.back",
                                "back", null,
                                null, null, "Back"));

                manager.registerMenu(root);
                manager.registerMenu(food);

                return manager;
        }

        private static UiLeftProductionPreview preview(
                        UiSystem ui,
                        AtomicInteger regular) {

                UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                                key -> key,
                                (command, parameter) -> new UiGameplayMenuResolver.DefinitionInfo(
                                                "Bread", "bread"));

                UiProductionMenuModel model = new UiProductionMenuModel(
                                menus(),
                                resolver,
                                actionId -> new UiProductionQueueCounts(
                                                regular.get(), 2, 8));

                return new UiLeftProductionPreview(
                                ui, model, FONT);
        }

        private static UiButton firstButton(
                        UiLeftProductionPreview preview) {

                UiContainer row = (UiContainer) preview
                                .scroll().content()
                                .children().get(0);

                UiButton button = findButton(row);

                if (button == null) {
                        throw new AssertionError(
                                        "Could not find production button");
                }

                return button;
        }

        /**
         * Finds the first button inside a nested
         * production UI container.
         */
        private static UiButton findButton(
                        UiContainer container) {

                for (var child : container.children()) {

                        if (child instanceof UiButton button) {
                                return button;
                        }

                        if (child instanceof UiContainer nested) {

                                UiButton found = findButton(nested);

                                if (found != null) {
                                        return found;
                                }
                        }
                }

                return null;
        }

        @Test
        void createsHiddenPreviewAndDocksLeft() {

                UiSystem ui = new UiSystem();

                UiLeftProductionPreview preview = preview(ui, new AtomicInteger(3));

                assertFalse(preview.window().isOpen());
                assertFalse(preview.window().draggable());
                assertFalse(preview.window().resizable());
                assertFalse(preview.window().titleBar().visible());

                preview.dockLeft(
                                new UiInsets(80, 0, 24, 12));

                assertEquals(
                                UiWindowAnchor.LEFT,
                                preview.window().anchor());

                assertEquals(
                                UiWindowDockMode.FILL_HEIGHT,
                                preview.window().dockMode());

                preview.open();

                ui.updateLayout(new UiSize(1280, 720));

                assertEquals(
                                12,
                                preview.window().bounds().x());

                assertEquals(
                                80,
                                preview.window().bounds().y());

                assertEquals(
                                720 - 80 - 24,
                                preview.window().bounds().height());
        }

        @Test
        void submenuButtonsNavigateAndBackWorks() {

                UiLeftProductionPreview preview = preview(
                                new UiSystem(),
                                new AtomicInteger(3));

                preview.open();

                assertEquals(
                                "game.left.production.root",
                                preview.currentMenuId());

                UiButton food = firstButton(preview);

                assertTrue(food.enabled());

                food.activate();

                assertEquals(
                                "game.left.production.food",
                                preview.currentMenuId());

                // Second row is the Back entry.
                UiContainer backRow = (UiContainer) preview
                                .scroll().content()
                                .children().get(1);

                UiButton back = (UiButton) backRow.children().get(0);
                assertTrue(back.enabled());
                back.activate();

                assertEquals(
                                "game.left.production.root",
                                preview.currentMenuId());
        }

        @Test
        void countsUpdateWithoutRebuildingButtons() {

                AtomicInteger regular = new AtomicInteger(3);

                UiLeftProductionPreview preview = preview(
                                new UiSystem(), regular);

                preview.open();
                firstButton(preview).activate();

                String actionId = "game.left.production.food.bread";

                UiButton breadButton = firstButton(preview);

                // Refresh the initial overlay values.
                preview.refreshCounts();

                UiLabel originalCountLabel = preview.countLabel(actionId);

                UiLabel worldLabel = preview.worldCountLabelForTesting(actionId);

                // Regular and automated counts.
                assertNotNull(originalCountLabel);

                assertEquals("3", originalCountLabel.text());
                UiLabel automatedLabel = preview.automatedCountLabel(actionId);

                assertNotNull(automatedLabel);
                assertEquals("2", automatedLabel.text());
                assertEquals(32, automatedLabel.position().y());
                // World count appears separately over the icon.
                assertNotNull(worldLabel);
                assertEquals("8", worldLabel.text());
                assertTrue(worldLabel.visible());

                // The isolated preview does not have a
                // registered gameplay action binding.
                assertFalse(breadButton.enabled());

                // Simulate the regular queue changing.
                regular.set(11);

                preview.refreshCounts();
                assertSame(
                                automatedLabel,
                                preview.automatedCountLabel(actionId));

                assertEquals("2", automatedLabel.text());
                // Refreshing counts must not recreate buttons.
                assertSame(
                                breadButton,
                                firstButton(preview));

                // Regular count label must also be reused.
                assertSame(
                                originalCountLabel,
                                preview.countLabel(actionId));

                assertEquals("11", originalCountLabel.text());

                // The in-world overlay must also be reused.
                assertSame(
                                worldLabel,
                                preview.worldCountLabelForTesting(actionId));

                assertEquals("8", worldLabel.text());
                assertTrue(worldLabel.visible());
        }

        @Test
        void menuReloadResetsNavigation() {

                UiLeftProductionPreview preview = preview(
                                new UiSystem(),
                                new AtomicInteger(0));

                preview.open();
                firstButton(preview).activate();

                assertEquals(
                                "game.left.production.food",
                                preview.currentMenuId());

                preview.setMenus(menus());

                assertEquals(
                                "game.left.production.root",
                                preview.currentMenuId());

                assertEquals(
                                1,
                                preview.scroll().content()
                                                .children().size());
        }

        @Test
        void usesTwoColumnGridWithoutHeading() {

                UiSystem ui = new UiSystem();

                UiLeftProductionPreview preview = preview(ui, new AtomicInteger(3));

                // Add a second root entry to test both columns.
                MenuManager manager = menus();

                manager.requireMenu(
                                "game.left.production.root")
                                .addItem(item(
                                                "game.left.production.more",
                                                "submenu",
                                                "game.left.production.food",
                                                null,
                                                null,
                                                "More"));

                preview.setMenus(manager);

                // The window content should contain only
                // the scroll area, not a heading label.
                assertEquals(
                                1,
                                preview.window().content()
                                                .children().size());

                UiContainer grid = (UiContainer) preview
                                .scroll().content()
                                .children().get(0);

                assertInstanceOf(
                                UiGridLayout.class,
                                grid.layoutStrategy());

                UiGridLayout layout = (UiGridLayout) grid.layoutStrategy();

                assertEquals(2, layout.columns());
                assertEquals(2, grid.children().size());

                preview.open();
                ui.updateLayout(new UiSize(1280, 720));

                var first = grid.children().get(0).bounds();
                var second = grid.children().get(1).bounds();

                assertEquals(first.y(), second.y());
                assertTrue(second.x() > first.x());
        }

        @Test
        void worldCountVisibilityUpdatesWithoutRebuilding() {

                AtomicInteger worldCount = new AtomicInteger(8);

                UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                                key -> key,
                                (command, parameter) -> new UiGameplayMenuResolver.DefinitionInfo(
                                                "Bread", "bread"));

                UiProductionMenuModel model = new UiProductionMenuModel(
                                menus(),
                                resolver,
                                actionId -> new UiProductionQueueCounts(
                                                3, 2, worldCount.get()));

                UiLeftProductionPreview preview = new UiLeftProductionPreview(
                                new UiSystem(), model, FONT);

                preview.open();
                firstButton(preview).activate();

                String id = "game.left.production.food.bread";

                UiButton originalButton = firstButton(preview);
                UiLabel label = preview.worldCountLabelForTesting(id);

                assertNotNull(label);
                assertTrue(label.visible());
                assertEquals("8", label.text());

                // No items remaining in the world.
                worldCount.set(0);
                preview.refreshCounts();

                assertFalse(label.visible());
                assertSame(originalButton, firstButton(preview));

                // Items become available again.
                worldCount.set(12);
                preview.refreshCounts();

                assertTrue(label.visible());
                assertEquals("12", label.text());

                // The existing label and icon are reused.
                assertSame(
                                label,
                                preview.worldCountLabelForTesting(id));

                assertSame(originalButton, firstButton(preview));
        }

        @TempDir
        Path tempDir;

        @Test
        void modCanExtendProductionPanel()
                        throws Exception {

                UiSystem ui = new UiSystem();

                MenuManager manager = menus();

                UiLeftProductionPreview preview = preview(ui, new AtomicInteger(3));

                preview.setMenus(manager);
                preview.open();

                // Initially only the built-in Food submenu exists.
                UiContainer grid = (UiContainer) preview
                                .scroll().content()
                                .children().get(0);

                assertEquals(1, grid.children().size());

                // --------------------------------------------------
                // Register a mod-defined production submenu.
                // --------------------------------------------------

                MenuDefinition modMenu = new MenuDefinition(
                                "examplemod:production",
                                null,
                                "Example Production");

                modMenu.addItem(item(
                                "examplemod:production.back",
                                "back",
                                null,
                                null,
                                null,
                                "Back"));

                manager.registerMenu(modMenu);

                // --------------------------------------------------
                // Extend the built-in production root.
                // --------------------------------------------------

                Path extension = tempDir.resolve(
                                "production-extension.xml");

                Files.writeString(extension, """
                                <menu-extension
                                    id="examplemod:production-extension"
                                    targetMenu="game.left.production.root">

                                    <item
                                        id="examplemod:production-link"
                                        type="submenu"
                                        title="Example Production"
                                        targetMenu="examplemod:production"
                                        after="game.left.production.food" />

                                </menu-extension>
                                """);

                manager.applyExtension(extension.toFile());

                // The menu registry is the same instance.
                // Refresh without recreating the preview.
                preview.refreshCurrentMenu();

                grid = (UiContainer) preview
                                .scroll().content()
                                .children().get(0);

                assertEquals(2, grid.children().size());

                // --------------------------------------------------
                // Navigate into the mod's submenu.
                // --------------------------------------------------

                UiContainer modCell = (UiContainer) grid.children().get(1);

                UiButton modButton = findButton(modCell);

                assertNotNull(modButton);
                assertTrue(modButton.enabled());

                modButton.activate();

                assertEquals(
                                "examplemod:production",
                                preview.currentMenuId());

                // --------------------------------------------------
                // Back must return to the production root.
                // --------------------------------------------------

                UiContainer backRow = (UiContainer) preview
                                .scroll().content()
                                .children().get(1);

                UiButton back = findButton(backRow);

                assertNotNull(back);

                back.activate();

                assertEquals(
                                "game.left.production.root",
                                preview.currentMenuId());

                // The mod contribution remains registered.
                grid = (UiContainer) preview
                                .scroll().content()
                                .children().get(0);

                assertEquals(2, grid.children().size());
        }

        @Test
        void modActionUsesRegisteredGameplayBinding() {

                MenuManager manager = menus();

                manager.requireMenu(
                                "game.left.production.root")
                                .addItem(item(
                                                "examplemod:production-action",
                                                "action",
                                                null,
                                                "MOD_ACTION",
                                                "examplemod:run",
                                                "Run Mod Action"));

                UiLeftProductionPreview preview = preview(new UiSystem(), new AtomicInteger(0));

                preview.setMenus(manager);

                AtomicBoolean available = new AtomicBoolean(false);
                AtomicInteger executions = new AtomicInteger();

                preview.setActionBinding(new UiGameplayActionBinding() {

                        @Override
                        public boolean isAvailable(MenuItemDefinition item) {
                                return available.get();
                        }

                        @Override
                        public void execute(MenuItemDefinition item) {
                                executions.incrementAndGet();
                        }
                });

                preview.open();

                UiContainer grid = (UiContainer) preview
                                .scroll().content()
                                .children().get(0);

                UiContainer modCell = (UiContainer) grid.children().get(1);

                UiButton modButton = findButton(modCell);

                assertNotNull(modButton);
                assertFalse(modButton.enabled());

                // Register or enable the action.
                available.set(true);
                preview.refreshActionAvailability();

                assertTrue(modButton.enabled());

                modButton.activate();

                assertEquals(1, executions.get());

                // Simulate the action becoming unavailable.
                // A stale enabled button must still recheck
                // availability before dispatching its click.
                available.set(false);

                modButton.activate();

                assertEquals(1, executions.get());
                assertFalse(modButton.enabled());

                // Re-enable without replacing the button.
                available.set(true);
                preview.refreshActionAvailability();

                assertSame(modButton, findButton(modCell));
                assertTrue(modButton.enabled());

                modButton.activate();

                assertEquals(2, executions.get());
        }

        @Test
        void xmlModActionExecutesAndDisablesOnUnload()
                        throws Exception {

                MenuManager manager = menus();

                String actionId = "left-panel-test:run";

                // ---------------------------------------------
                // Mod contributes an action through XML.
                // ---------------------------------------------

                Path extension = tempDir.resolve(
                                "mod-production-action.xml");

                Files.writeString(extension, """
                                <menu-extension
                                    id="left-panel-test:extension"
                                    targetMenu="game.left.production.root">

                                    <item
                                        id="left-panel-test:button"
                                        type="action"
                                        title="Run Test Mod Action"
                                        command="MOD_ACTION"
                                        parameter="left-panel-test:run"
                                        after="game.left.production.food" />

                                </menu-extension>
                                """);

                manager.applyExtension(extension.toFile());

                UiLeftProductionPreview preview = preview(
                                new UiSystem(),
                                new AtomicInteger(0));

                preview.setMenus(manager);

                // Use the real registered mod action binding.
                preview.setActionBinding(
                                new UiRegisteredGameplayActionBinding(
                                                UiGameplayActionBinding.disabled()));

                preview.open();

                UiContainer grid = (UiContainer) preview
                                .scroll().content()
                                .children().get(0);

                UiContainer modCell = (UiContainer) grid.children().get(1);

                UiButton modButton = findButton(modCell);

                assertNotNull(modButton);

                // No mod action registered yet.
                assertFalse(modButton.enabled());

                AtomicInteger executions = new AtomicInteger();

                UiActionRegistration registration = null;

                try {

                        // -----------------------------------------
                        // Simulate a mod registering its action.
                        // -----------------------------------------

                        registration = UiActionRegistry.register(
                                        actionId,
                                        "Run Test Mod Action",
                                        executions::incrementAndGet);

                        preview.refreshActionAvailability();

                        assertTrue(modButton.enabled());

                        // Clicking the XML-created button must
                        // execute the registered Java callback.
                        modButton.activate();

                        assertEquals(1, executions.get());

                        // -----------------------------------------
                        // Simulate the mod unloading.
                        // -----------------------------------------

                        registration.unregister();

                        preview.refreshActionAvailability();

                        assertFalse(modButton.enabled());

                        // Unregistered actions cannot execute.
                        modButton.activate();

                        assertEquals(1, executions.get());

                        // Refreshing availability must not
                        // recreate the original button.
                        assertSame(
                                        modButton,
                                        findButton(modCell));

                } finally {

                        // Never leave a registered action behind,
                        // even when an assertion fails.
                        if (registration != null) {
                                registration.unregister();
                        }
                }
        }

}
