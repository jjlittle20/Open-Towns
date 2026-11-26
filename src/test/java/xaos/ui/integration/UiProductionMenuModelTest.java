
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import xaos.panels.menus.MenuManager;

class UiProductionMenuModelTest {

    @TempDir
    Path tempDir;

    private MenuManager createMenus()
            throws Exception {

        Files.writeString(
                tempDir.resolve("root.xml"),
                """
                        <menu id="game.left.production.root"
                              title="Production">

                            <item
                                id="game.left.production.food"
                                type="submenu"
                                title="Food"
                                targetMenu="game.left.production.food" />

                        </menu>
                        """);

        Files.writeString(
                tempDir.resolve("food.xml"),
                """
                        <menu id="game.left.production.food"
                              title="Food">

                            <item
                                id="game.left.production.food.bread"
                                type="action"
                                command="QUEUE"
                                parameter="qbread" />

                            <item
                                id="game.left.production.food.back"
                                type="back"
                                title="Back" />

                        </menu>
                        """);

        MenuManager menus = new MenuManager();
        menus.loadMenus(tempDir.toFile());

        return menus;
    }

    private UiGameplayMenuResolver resolver() {

        return new UiGameplayMenuResolver(
                key -> key,
                (command, parameter) -> {

                    if ("qbread".equals(parameter)) {
                        return new UiGameplayMenuResolver.DefinitionInfo(
                                "Bread",
                                "bread");
                    }

                    return null;
                });
    }

    @Test
    void navigatesThroughProductionMenus()
            throws Exception {

        UiProductionMenuModel model = new UiProductionMenuModel(
                createMenus(),
                resolver(),
                id -> new UiProductionQueueCounts(
                        0, 0, 0));

        assertEquals(
                "game.left.production.root",
                model.currentMenuId());

        assertFalse(model.canGoBack());

        var rootEntries = model.entries();

        assertEquals(1, rootEntries.size());

        model.navigate(
                rootEntries.get(0).definition());

        assertEquals(
                "game.left.production.food",
                model.currentMenuId());

        assertTrue(model.canGoBack());

        model.back();

        assertEquals(
                "game.left.production.root",
                model.currentMenuId());

        assertFalse(model.canGoBack());
    }

    @Test
    void productionEntriesContainLiveCounts()
            throws Exception {

        AtomicInteger regularQueue = new AtomicInteger(3);

        UiProductionMenuModel model = new UiProductionMenuModel(
                createMenus(),
                resolver(),
                id -> {
                    assertEquals("qbread", id);

                    return new UiProductionQueueCounts(
                            regularQueue.get(),
                            2,
                            8);
                });

        model.navigate(
                model.entries().get(0).definition());

        var entries = model.entries();

        assertEquals(2, entries.size());

        var bread = entries.get(0);

        assertEquals(
                "Bread",
                bread.presentation().title());

        assertEquals(
                "bread",
                bread.presentation().icon().id());

        assertEquals(
                3,
                bread.counts().regular());

        assertEquals(
                2,
                bread.counts().automated());

        assertEquals(
                8,
                bread.counts().inWorld());

        // The model must not cache queue values.
        regularQueue.set(12);

        assertEquals(
                12,
                model.entries().get(0)
                        .counts().regular());

        // Back is not a production action.
        assertNull(
                entries.get(1).counts());
    }

    @Test
    void cannotNavigateThroughAnAction()
            throws Exception {

        UiProductionMenuModel model = new UiProductionMenuModel(
                createMenus(),
                resolver(),
                id -> new UiProductionQueueCounts(
                        0, 0, 0));

        model.navigate(
                model.entries().get(0).definition());

        var bread = model.entries()
                .get(0).definition();

        assertThrows(
                IllegalArgumentException.class,
                () -> model.navigate(bread));
    }
}
