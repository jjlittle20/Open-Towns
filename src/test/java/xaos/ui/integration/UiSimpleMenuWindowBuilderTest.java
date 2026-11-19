
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuXmlLoader;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiSimpleMenuWindowBuilderTest {

    private static final UiTextMetrics FONT =
            text -> new UiSize(text.length() * 7, 16);

    private static String localize(String key) {
        return "loc:" + key;
    }

    /**
     * Loads a real menu XML resource from the
     * project's existing menu definitions.
     */
    private static MenuDefinition loadMenu(
            String resourcePath) throws Exception {

        URL resource = UiSimpleMenuWindowBuilderTest.class
                .getClassLoader()
                .getResource(resourcePath);

        assertNotNull(
                resource,
                "Missing menu resource: " + resourcePath);

        File file = Path.of(resource.toURI()).toFile();

        return MenuXmlLoader.load(file);
    }

    @Test
    void buildsRealCreditsMenuFromXml() throws Exception {

        MenuDefinition definition = loadMenu(
                "data/menus/main/credits/root.xml");

        assertEquals(
                "main.credits.root",
                definition.getId());

        assertEquals(14, definition.getItems().size());

        UiWindow window =
                UiSimpleMenuWindowBuilder.build(
                        definition,
                        UiSimpleMenuWindowBuilderTest::localize,
                        FONT,
                        () -> {});

        assertEquals(
                "loc:MainMenuPanel.34",
                window.title());

        // The menu contains a scrollable content
        // area and a stationary Back button.
        assertEquals(
                2,
                window.content().children().size());

        assertInstanceOf(
                UiScrollContainer.class,
                window.content().children().get(0));

        assertInstanceOf(
                UiButton.class,
                window.content().children().get(1));

        UiScrollContainer scroll =
                (UiScrollContainer)
                        window.content().children().get(0);

        // Eight text entries and five spacers.
        assertEquals(
                13,
                scroll.content().children().size());

        long labels = scroll.content().children()
                .stream()
                .filter(UiLabel.class::isInstance)
                .count();

        assertEquals(8, labels);

        assertTrue(scroll.clipChildren());

        UiLabel firstCredit = (UiLabel)
                scroll.content().children().get(1);

        assertEquals(
                "loc:MainMenuPanel.35",
                firstCredit.text());

        UiLabel lastCredit = (UiLabel)
                scroll.content().children().get(11);

        assertEquals(
                "loc:MainMenuPanel.89",
                lastCredit.text());
    }

    @Test
    void backButtonDelegatesToCaller() throws Exception {

        MenuDefinition definition = loadMenu(
                "data/menus/main/credits/root.xml");

        AtomicInteger backCount =
                new AtomicInteger();

        UiWindow window =
                UiSimpleMenuWindowBuilder.build(
                        definition,
                        UiSimpleMenuWindowBuilderTest::localize,
                        FONT,
                        backCount::incrementAndGet);

        UiButton back = (UiButton)
                window.content().children().get(1);

        assertEquals(
                "loc:MainMenuPanel.7",
                back.text());

        back.activate();

        assertEquals(1, backCount.get());
    }

    @Test
    void creditsWindowWorksWithPermanentUiSystem()
            throws Exception {

        MenuDefinition definition = loadMenu(
                "data/menus/main/credits/root.xml");

        UiWindow window =
                UiSimpleMenuWindowBuilder.build(
                        definition,
                        UiSimpleMenuWindowBuilderTest::localize,
                        FONT,
                        () -> {});

        UiSystem system = new UiSystem();

        system.windows().register(window);
        system.updateLayout(new UiSize(800, 600));

        assertTrue(
                system.windows().isManaged(window));

        UiScrollContainer scroll =
                (UiScrollContainer)
                        window.content().children().get(0);

        assertTrue(scroll.bounds().height() > 0);

        // A smaller window must be able to
        // scroll through all of the credits.
        window.setUserSize(new UiSize(280, 180));

        system.updateLayout(new UiSize(800, 600));

        assertTrue(scroll.maxScrollY() > 0);

        assertTrue(scroll.scrollBy(40));

        system.updateLayout(new UiSize(800, 600));

        assertEquals(40, scroll.scrollY());
    }

    @Test
    void unsupportedMenuTypesAreNotSilentlyDiscarded()
            throws Exception {

        // The original main menu contains submenu
        // items that this first adapter does not
        // support yet.
        MenuDefinition definition = loadMenu(
                "data/menus/main/root.xml");

        UnsupportedOperationException exception =
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> UiSimpleMenuWindowBuilder.build(
                                definition,
                                UiSimpleMenuWindowBuilderTest::localize,
                                FONT,
                                () -> {}));

        assertTrue(
                exception.getMessage().contains("submenu"));
    }
}
