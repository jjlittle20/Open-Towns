
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuExtensionTabsTest {

    @TempDir
    Path tempDir;

    private MenuManager menus() throws Exception {

        Path directory = Files.createDirectory(
                tempDir.resolve("menus"));

        Files.writeString(
                directory.resolve("panel.xml"),
                """
                        <menu id="test.panel" title="Panel">

                            <tabs id="test.tabs" placement="left">

                                <tab id="general" title="General">
                                    <item id="general.first"
                                          type="text"
                                          title="First" />

                                    <item id="general.back"
                                          type="back" />
                                </tab>

                                <tab id="advanced" title="Advanced">
                                    <item id="advanced.first"
                                          type="text"
                                          title="Advanced" />
                                </tab>

                            </tabs>

                            <item id="panel.back" type="back" />

                        </menu>
                        """);

        MenuManager manager = new MenuManager();

        manager.loadMenus(directory.toFile());

        return manager;
    }

    private Path extension(String xml) throws Exception {

        Path file = tempDir.resolve("extension.xml");

        Files.writeString(file, xml);

        return file;
    }

    private static MenuTabsDefinition tabs(
            MenuManager manager) {

        return (MenuTabsDefinition) manager
                .requireMenu("test.panel")
                .getContent().get(0);
    }

    @Test
    void modCanAddNewTab() throws Exception {

        MenuManager manager = menus();

        manager.applyExtension(extension("""
                <menu-extension
                    id="examplemod:tabs"
                    targetMenu="test.panel">

                    <tab
                        id="examplemod:logistics"
                        targetTabs="test.tabs"
                        title="Logistics"
                        after="general">

                        <item
                            id="examplemod:overview"
                            type="action"
                            title="Overview"
                            command="CUSTOMACTION"
                            parameter="examplemod:overview" />

                    </tab>

                </menu-extension>
                """).toFile());

        MenuTabsDefinition group = tabs(manager);

        assertEquals(
                MenuTabsDefinition.Placement.LEFT,
                group.getPlacement());

        assertEquals(
                List.of(
                        "general",
                        "examplemod:logistics",
                        "advanced"),
                group.getTabs().stream()
                        .map(MenuTabDefinition::getId)
                        .toList());

        assertEquals(
                "examplemod:overview",
                group.getTabs().get(1)
                        .getItems().get(0).getId());

        assertDoesNotThrow(() -> manager.validateMenuTree("test.panel"));
    }

    @Test
    void modCanExtendExistingTab() throws Exception {

        MenuManager manager = menus();

        manager.applyExtension(extension("""
                <menu-extension
                    id="examplemod:general"
                    targetMenu="test.panel">

                    <tab-extension
                        targetTabs="test.tabs"
                        targetTab="general">

                        <item
                            id="examplemod:after-first"
                            type="action"
                            command="CUSTOMACTION"
                            after="general.first" />

                        <item
                            id="examplemod:before-back"
                            type="action"
                            command="CUSTOMACTION"
                            before="general.back" />

                    </tab-extension>

                </menu-extension>
                """).toFile());

        MenuTabDefinition general = tabs(manager).getTabs().get(0);

        assertEquals(
                List.of(
                        "general.first",
                        "examplemod:after-first",
                        "examplemod:before-back",
                        "general.back"),
                general.getItems().stream()
                        .map(MenuItemDefinition::getId)
                        .toList());

        // Flattened view must contain the same
        // items in the correct order.
        assertEquals(
                List.of(
                        "general.first",
                        "examplemod:after-first",
                        "examplemod:before-back",
                        "general.back",
                        "advanced.first",
                        "panel.back"),
                manager.requireMenu("test.panel")
                        .getItems().stream()
                        .map(MenuItemDefinition::getId)
                        .toList());
    }

    @Test
    void extensionCanTargetTabCreatedInSameFile()
            throws Exception {

        MenuManager manager = menus();

        manager.applyExtension(extension("""
                <menu-extension
                    id="examplemod:new-tab-and-items"
                    targetMenu="test.panel">

                    <tab
                        id="examplemod:logistics"
                        targetTabs="test.tabs"
                        title="Logistics">

                        <item id="examplemod:tab-back"
                              type="back" />

                    </tab>

                    <tab-extension
                        targetTabs="test.tabs"
                        targetTab="examplemod:logistics">

                        <item
                            id="examplemod:transport"
                            type="action"
                            command="CUSTOMACTION" />

                    </tab-extension>

                </menu-extension>
                """).toFile());

        MenuTabDefinition logistics = tabs(manager).getTabs().get(2);

        assertEquals(
                List.of(
                        "examplemod:transport",
                        "examplemod:tab-back"),
                logistics.getItems().stream()
                        .map(MenuItemDefinition::getId)
                        .toList());
    }

    @Test
    void invalidTabTargetDoesNotModifyMenu()
            throws Exception {

        MenuManager manager = menus();

        MenuDefinition original = manager.requireMenu("test.panel");

        Path file = extension("""
                <menu-extension
                    id="examplemod:invalid"
                    targetMenu="test.panel">

                    <tab-extension
                        targetTabs="test.tabs"
                        targetTab="does-not-exist">

                        <item id="examplemod:action"
                              type="action"
                              command="CUSTOMACTION" />

                    </tab-extension>

                </menu-extension>
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.applyExtension(
                        file.toFile()));

        assertSame(
                original,
                manager.requireMenu("test.panel"));
    }

    @Test
    void duplicateItemAcrossTabsIsRejected()
            throws Exception {

        MenuManager manager = menus();

        MenuDefinition original = manager.requireMenu("test.panel");

        Path file = extension("""
                <menu-extension
                    id="examplemod:duplicate"
                    targetMenu="test.panel">

                    <tab
                        id="examplemod:new-tab"
                        targetTabs="test.tabs"
                        title="New tab">

                        <item id="general.first"
                              type="action"
                              command="CUSTOMACTION" />

                    </tab>

                </menu-extension>
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.applyExtension(
                        file.toFile()));

        assertSame(
                original,
                manager.requireMenu("test.panel"));
    }
}
