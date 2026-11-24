
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuLegacyIdResolverTest {

    @TempDir
    Path tempDir;

    private MenuManager baseMenus() throws Exception {

        URL converted = Objects.requireNonNull(
                getClass().getClassLoader()
                        .getResource(
                                "data/menus/right/root.xml"));

        MenuManager manager = new MenuManager();

        manager.loadMenus(
                Path.of(converted.toURI())
                        .getParent()
                        .toFile());

        return manager;
    }

    private MenuLegacyIdResolver resolver()
            throws Exception {

        URL legacy = Objects.requireNonNull(
                getClass().getClassLoader()
                        .getResource("data/menu_right.xml"));

        return MenuLegacyIdResolver.fromBaseMenu(
                Path.of(legacy.toURI()).toFile(),
                baseMenus());
    }

    @Test
    void resolvesNestedLegacyMenuPaths()
            throws Exception {

        var resolver = resolver();

        assertEquals(
                "game.right.walls",
                resolver.resolvePath("walls"));

        assertEquals(
                "game.right.walls.log",
                resolver.resolvePath(
                        "walls", "log"));

        assertEquals(
                "game.right.buildings",
                resolver.resolvePath("buildings"));
    }

    @Test
    void resolvesOriginalActionIds()
            throws Exception {

        var resolver = resolver();

        String logMenu = resolver.resolvePath("walls", "log");

        assertEquals(
                "game.right.walls.log.wall",
                resolver.resolveItem(
                        logMenu, "qlogwall"));

        assertEquals(
                "game.right.walls.log.scaffold",
                resolver.resolveItem(
                        logMenu, "qscaffold"));
    }

    @Test
    void sameLegacyIdCanExistInDifferentMenus()
            throws Exception {

        var resolver = resolver();

        String log = resolver.resolvePath(
                "walls", "log");

        String wooden = resolver.resolvePath(
                "walls", "wooden");

        assertEquals(
                "game.right.walls.log.scaffold",
                resolver.resolveItem(log, "qscaffold"));

        assertEquals(
                "game.right.walls.wooden.qscaffold",
                resolver.resolveItem(
                        wooden, "qscaffold"));
    }

    @Test
    void legacyMatchingIsCaseInsensitive()
            throws Exception {

        var resolver = resolver();

        assertEquals(
                "game.right.walls.log.wall",
                resolver.resolveItem(
                        resolver.resolvePath(
                                "WALLS", "LOG"),
                        "QLOGWALL"));
    }

    @Test
    void ambiguousBrickPathIsRejected()
            throws Exception {

        var resolver = resolver();

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolvePath(
                        "walls", "stone", "brick"));

        assertTrue(
                exception.getMessage()
                        .contains("Ambiguous"));
    }

    @Test
    void unknownLegacyItemIsRejected()
            throws Exception {

        var resolver = resolver();

        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolveItem(
                        resolver.resolvePath(
                                "walls", "log"),
                        "qmissingwall"));
    }

    @Test
    void mismatchedBaseMenuIsRejected()
            throws Exception {

        Path incorrect = tempDir.resolve(
                "menu_right.xml");

        Files.writeString(
                incorrect,
                """
                        <menu>
                            <walls name="Walls" />
                        </menu>
                        """);

        assertThrows(
                IllegalArgumentException.class,
                () -> MenuLegacyIdResolver.fromBaseMenu(
                        incorrect.toFile(),
                        baseMenus()));
    }
}
