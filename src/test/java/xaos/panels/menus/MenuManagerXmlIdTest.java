
package xaos.panels.menus;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MenuManagerXmlIdTest {

    @Test
    void submenuRetainsItsXmlItemId() {

        MenuDefinition root = new MenuDefinition(
                "test.root",
                "MainMenuPanel.4");

        MenuDefinition credits = new MenuDefinition(
                "test.credits.root",
                "MainMenuPanel.34");

        root.addItem(new MenuItemDefinition(
                "main.credits",
                "submenu",
                "MainMenuPanel.34",
                "test.credits.root",
                null,
                false,
                false,
                null,
                null,
                null,
                null,
                false));

        MenuManager manager = new MenuManager();

        manager.getMenusById().put(
                root.getId(), root);

        manager.getMenusById().put(
                credits.getId(), credits);

        SmartMenu legacyRoot =
                manager.buildSmartMenu(root, null);

        assertEquals(
                1,
                legacyRoot.getItems().size());

        SmartMenu creditsSubmenu =
                legacyRoot.getItems().get(0);

        // The submenu retains the identifier
        // from the parent XML menu item.
        assertEquals(
                "main.credits",
                creditsSubmenu.getID());

        assertEquals(
                SmartMenu.TYPE_MENU,
                creditsSubmenu.getType());
    }
}
