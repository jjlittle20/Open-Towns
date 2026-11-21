
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuManager;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiRightMenuPreviewTest {

    @TempDir
    Path tempDir;

    private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

    @Test
    void navigatesXmlMenusWithoutExecutingActions()
            throws Exception {

        Path menusFolder = Files.createDirectory(
                tempDir.resolve("menus"));

        Files.writeString(
                menusFolder.resolve("root.xml"),
                """
                        <menu id="game.right.root"
                              title="Construction">

                            <item id="game.right.walls"
                                  type="submenu"
                                  title="Walls"
                                  targetMenu="game.right.walls" />

                        </menu>
                        """);

        Files.writeString(
                menusFolder.resolve("walls.xml"),
                """
                        <menu id="game.right.walls"
                              title="Walls">

                            <item id="game.right.walls.log"
                                  type="action"
                                  command="QUEUEANDPLACEAREA"
                                  parameter="qlogwall" />

                            <item id="game.right.walls.back"
                                  type="back" />

                        </menu>
                        """);

        MenuManager manager = new MenuManager();
        manager.loadMenus(menusFolder.toFile());

        UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                key -> key,
                (command, parameter) -> new UiGameplayMenuResolver.DefinitionInfo(
                        "Resolved Log Wall",
                        "log_wall"));

        UiRightMenuPreview preview = new UiRightMenuPreview(
                new UiSystem(),
                manager,
                resolver,
                FONT,
                key -> key);

        assertEquals(
                "game.right.root",
                preview.currentMenuId());

        // Content order:
        // 0 = Back button
        // 1 = Preview notice
        // 2 = Scroll container
        UiScrollContainer scroll = (UiScrollContainer) preview.window()
                .content().children().get(2);

        UiButton walls = (UiButton) scroll.content()
                .children().get(0);

        assertTrue(walls.enabled());
        assertEquals("> Walls", walls.text());

        walls.activate();

        assertEquals(
                "game.right.walls",
                preview.currentMenuId());

        UiButton logWall = (UiButton) scroll.content()
                .children().get(0);

        assertEquals(
                "Resolved Log Wall",
                logWall.text());

        // Gameplay actions cannot yet execute.
        assertFalse(logWall.enabled());

        UiButton back = (UiButton) preview.window()
                .content().children().get(0);

        assertTrue(back.enabled());

        back.activate();

        assertEquals(
                "game.right.root",
                preview.currentMenuId());

        assertFalse(back.enabled());
    }

    @Test
    void gameplayActionsUseInjectedBinding()
            throws Exception {

        Path menusFolder = Files.createDirectory(
                tempDir.resolve("action-menus"));

        Files.writeString(
                menusFolder.resolve("root.xml"),
                """
                        <menu id="game.right.root"
                              title="Construction">

                            <item
                                id="test.log-wall"
                                type="action"
                                title="Log Wall"
                                command="QUEUEANDPLACEAREA"
                                parameter="qlogwall" />

                        </menu>
                        """);

        MenuManager manager = new MenuManager();
        manager.loadMenus(menusFolder.toFile());

        UiGameplayMenuResolver resolver = new UiGameplayMenuResolver(
                key -> key,
                (command, parameter) -> null);

        UiRightMenuPreview preview = new UiRightMenuPreview(
                new UiSystem(),
                manager,
                resolver,
                FONT,
                key -> key);

        UiScrollContainer scroll = (UiScrollContainer) preview.window()
                .content().children().get(2);

        UiButton button = (UiButton) scroll.content()
                .children().get(0);

        // Existing preview behaviour:
        // actions are disabled by default.
        assertFalse(button.enabled());

        AtomicReference<String> executed = new AtomicReference<>();

        // Install an isolated fake executor.
        // No actual Towns world is required.
        preview.setActionBinding(
                new UiGameplayActionBinding() {

                    @Override
                    public boolean isAvailable(
                            MenuItemDefinition item) {

                        return "QUEUEANDPLACEAREA"
                                .equals(item.getCommand());
                    }

                    @Override
                    public void execute(
                            MenuItemDefinition item) {

                        executed.set(
                                item.getParameter());
                    }
                });

        // The menu was rebuilt with the new binding.
        button = (UiButton) scroll.content()
                .children().get(0);

        assertTrue(button.enabled());

        button.activate();

        assertEquals(
                "qlogwall",
                executed.get());

        // Executing an action does not navigate
        // away from the current menu.
        assertEquals(
                "game.right.root",
                preview.currentMenuId());
    }

}
