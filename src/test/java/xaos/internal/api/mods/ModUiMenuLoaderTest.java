
package xaos.internal.api.mods;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import xaos.panels.menus.MenuManager;

class ModUiMenuLoaderTest {

    @TempDir
    Path tempDir;

    private Path write(
            String relativePath,
            String contents) throws Exception {

        Path file = tempDir.resolve(relativePath);

        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);

        return file;
    }

    @Test
    void loadsDefinitionsThenExtensionsInDependencyOrder()
            throws Exception {

        ModUiErrors.clear();

        Path base = tempDir.resolve("data/menus/right");

        write("data/menus/right/root.xml", """
                <menu id="game.right.root" title="Root">
                    <item id="game.right.back"
                          type="back" />
                </menu>
                """);

        // XML-only mod Alpha adds a new menu.
        write("mods/alpha/menus/definitions/new.xml", """
                <menu id="alpha:menu" title="Alpha">
                    <item id="alpha:back"
                          type="back" />
                </menu>
                """);

        // Alpha links that menu into the root.
        write("mods/alpha/menus/extensions/link.xml", """
                <menu-extension
                    id="alpha:link-extension"
                    targetMenu="game.right.root">

                    <item id="alpha:link"
                          type="submenu"
                          title="Alpha"
                          targetMenu="alpha:menu" />

                </menu-extension>
                """);

        // Beta depends on Alpha and inserts
        // another item immediately after its link.
        write("mods/beta/menus/menu.properties", """
                id=beta
                dependencies=alpha
                """);

        write("mods/beta/menus/extensions/add.xml", """
                <menu-extension
                    id="beta:add"
                    targetMenu="game.right.root">

                    <item id="beta:action"
                          type="action"
                          command="CUSTOMACTION"
                          after="alpha:link" />

                </menu-extension>
                """);

        MenuManager manager = ModUiMenuLoader.load(
                base.toFile(),
                tempDir.resolve("mods").toFile(),
                List.of("beta", "alpha"));

        assertTrue(manager.hasMenu("alpha:menu"));

        assertEquals(
                List.of(
                        "alpha:link",
                        "beta:action",
                        "game.right.back"),
                manager.requireMenu("game.right.root")
                        .getItems().stream()
                        .map(item -> item.getId())
                        .toList());

        assertDoesNotThrow(() -> manager.validateMenuTree(
                "game.right.root"));

        assertTrue(ModUiErrors.snapshot().isEmpty());
    }

    @Test
    void badExtensionDoesNotPreventAnotherModLoading()
            throws Exception {

        ModUiErrors.clear();

        write("data/menus/right/root.xml", """
                <menu id="game.right.root" title="Root">
                    <item id="game.right.back"
                          type="back" />
                </menu>
                """);

        write("mods/broken/menus/extensions/bad.xml", """
                <menu-extension
                    id="broken:bad"
                    targetMenu="game.right.root">

                    <item id="broken:action"
                          type="action"
                          command="CUSTOMACTION"
                          after="missing.item" />

                </menu-extension>
                """);

        write("mods/working/menus/extensions/good.xml", """
                <menu-extension
                    id="working:good"
                    targetMenu="game.right.root">

                    <item id="working:action"
                          type="action"
                          command="CUSTOMACTION" />

                </menu-extension>
                """);

        MenuManager manager = ModUiMenuLoader.load(
                tempDir.resolve("data/menus/right").toFile(),
                tempDir.resolve("mods").toFile(),
                List.of("broken", "working"));

        assertEquals(
                2,
                manager.requireMenu("game.right.root")
                        .getItems().size());

        assertEquals(
                "working:action",
                manager.requireMenu("game.right.root")
                        .getItems().get(0).getId());

        assertTrue(
                ModUiErrors.snapshot().stream()
                        .anyMatch(error -> error.modId().equals("broken")));
    }
}
