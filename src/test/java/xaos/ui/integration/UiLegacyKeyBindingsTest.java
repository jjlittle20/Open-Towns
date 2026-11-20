
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuXmlLoader;
import xaos.ui.components.UiKeyBindingRow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiRoot;
import xaos.ui.input.UiInputRouter;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;
import xaos.utils.UtilsKeyboard;

class UiLegacyKeyBindingsTest {

        private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

        private static MenuDefinition loadControls()
                        throws Exception {

                URL resource = UiLegacyKeyBindingsTest.class
                                .getClassLoader()
                                .getResource(
                                                "data/menus/main/options/controls_menu.xml");

                assertNotNull(resource);

                return MenuXmlLoader.load(
                                Path.of(resource.toURI()).toFile());
        }

        @Test
        void allOriginalKeyboardFunctionsResolve()
                        throws Exception {

                MenuDefinition menu = loadControls();

                Set<Integer> functionIds = new HashSet<>();
                int count = 0;

                for (MenuItemDefinition item : menu.getItems()) {

                        if (!"keyboard".equals(item.getType())) {
                                continue;
                        }

                        int functionId = UiLegacyKeyBindings.resolveFunctionId(
                                        item.getKey());

                        assertTrue(functionId >= 0);

                        assertTrue(
                                        functionIds.add(functionId),
                                        "Duplicate function: " + item.getKey());

                        count++;
                }

                assertEquals(37, count);
                assertEquals(37, functionIds.size());
        }

        @Test
        void mapsMovementAndBuildFaceFunctions() {

                assertEquals(
                                UtilsKeyboard.FN_UP,
                                UiLegacyKeyBindings.resolveFunctionId(
                                                "FN_UP"));

                assertEquals(
                                UtilsKeyboard.FN_DOWN,
                                UiLegacyKeyBindings.resolveFunctionId(
                                                "FN_DOWN"));

                assertEquals(
                                UtilsKeyboard.FN_TOGGLE_ITEM_BUILD_FACE,
                                UiLegacyKeyBindings.resolveFunctionId(
                                                "FN_TOGGLE_ITEM_BUILD_FACE"));

                assertEquals(
                                UtilsKeyboard.FN_BOT_10,
                                UiLegacyKeyBindings.resolveFunctionId(
                                                "FN_BOT_10"));
        }

        @Test
        void rejectsUnknownKeyboardFunction() {

                assertThrows(
                                IllegalArgumentException.class,
                                () -> UiLegacyKeyBindings.resolveFunctionId(
                                                "FN_NOT_REAL"));
        }

        @Test
        void rowsRequestPhysicalCapture() throws Exception {

                UiInputRouter input = new UiInputRouter(
                                new UiRoot(new UiContainer()));

                UiLegacyKeyBindings bindings = new UiLegacyKeyBindings(
                                input,
                                FONT,
                                () -> {
                                });

                MenuItemDefinition up = loadControls()
                                .getItems()
                                .stream()
                                .filter(item -> "FN_UP".equals(item.getKey()))
                                .findFirst()
                                .orElseThrow();

                UiKeyBindingRow row = bindings.forItem(up);

                assertNotNull(row);
                assertNotNull(row.actionLabel().text());

                // Clicking either slot enters capture mode.
                row.primaryButton().activate();

                assertTrue(input.isCapturingPhysicalKey());
                assertEquals(0, row.capturingSlot());

                // Cancelling must not modify settings.
                input.cancelPhysicalKeyCapture();

                assertFalse(input.isCapturingPhysicalKey());
                assertEquals(-1, row.capturingSlot());

                row.secondaryButton().activate();

                assertTrue(input.isCapturingPhysicalKey());
                assertEquals(1, row.capturingSlot());

                input.cancelPhysicalKeyCapture();
        }
}
