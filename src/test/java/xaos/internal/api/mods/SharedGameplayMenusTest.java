
package xaos.internal.api.mods;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import xaos.panels.menus.MenuManager;

class SharedGameplayMenusTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsBothGameplayMenuTrees()
            throws Exception {

        URL rightRoot = Objects.requireNonNull(
                getClass().getClassLoader()
                        .getResource(
                                "data/menus/right/root.xml"));

        // From data/menus/right/root.xml,
        // locate the shared data/menus folder.
        File builtInMenus = Path.of(
                rightRoot.toURI())
                .getParent()
                .getParent()
                .toFile();

        MenuManager menus = ModUiMenuLoader.load(
                builtInMenus,
                tempDir.resolve("mods").toFile(),
                List.of());

        assertTrue(
                menus.hasMenu("game.right.root"));

        assertTrue(
                menus.hasMenu("game.bottom.root"));

        assertTrue(
                menus.hasMenu(
                        "game.bottom.stockpiles"));

        assertTrue(
                menus.hasMenu("game.bottom.zones"));

        assertEquals(
                10,
                menus.requireMenu("game.bottom.root")
                        .getItems().size());

        assertEquals(
                10,
                menus.requireMenu(
                        "game.bottom.stockpiles")
                        .getItems().size());

        assertEquals(
                16,
                menus.requireMenu(
                        "game.bottom.zones")
                        .getItems().size());

        assertDoesNotThrow(
                () -> menus.validateMenuTree(
                        "game.right.root"));

        assertDoesNotThrow(
                () -> menus.validateMenuTree(
                        "game.bottom.root"));
    }
}
