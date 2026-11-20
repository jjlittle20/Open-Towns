
package xaos.ui.debug;

import java.util.Objects;

import org.lwjgl.opengl.GL11;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.core.UiRoot;
import xaos.ui.core.UiSystem;
import xaos.ui.layout.UiAbsoluteLayout;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiFocusPainter;
import xaos.ui.rendering.UiRenderer;
import xaos.ui.rendering.UiSceneRenderer;
import xaos.ui.rendering.UiScrollBarPainter;
import xaos.ui.rendering.UiTextFieldPainter;
import xaos.ui.rendering.opengl.TownsUiRenderer;
import xaos.ui.rendering.opengl.TownsUiTexture;
import xaos.ui.theme.BasicUiTheme;
import xaos.ui.theme.ClassicTownsTheme;
import xaos.ui.theme.UiTheme;
import xaos.ui.theme.UiThemeRegistry;
import xaos.utils.TextureData;
import xaos.utils.UtilsGL;
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiScrollBar;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiTextField;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiLinearLayout;
import xaos.ui.rendering.opengl.TownsUiTextMetrics;
import xaos.ui.components.UiButton;
import xaos.ui.rendering.opengl.TownsUiTextMetrics;
import xaos.ui.input.UiInputRouter;
import xaos.ui.input.UiKey;
import xaos.ui.core.UiWindowManager;
import xaos.ui.rendering.UiResizeGripPainter;

/**
 * Temporary visual test for the new UI framework.
 *
 * This is not part of the public modding API.
 */
public final class UiDebugOverlay {

        public static final boolean ENABLED = false;

        private static final String CLASSIC_THEME = "classic-towns";
        private static final String BASIC_THEME = "basic";

        // Change this to select the starting theme.
        private static final String STARTING_THEME = CLASSIC_THEME;

        private static final UiWindow WINDOW = createWindow();

        private static final UiWindow SECOND_WINDOW = createSecondWindow();

        private static final UiWindow SCROLL_WINDOW = createScrollWindow();

        // ------------------------------------------------------------
        // References to the game-owned UI runtime
        // ------------------------------------------------------------

        // These are assigned by install().
        // This class does not construct its own UiSystem.
        private static UiSystem UI;

        private static UiWindowManager WINDOWS;
        private static UiThemeRegistry THEMES;

        private static UiRoot ROOT;
        private static UiInputRouter INPUT;

        private UiDebugOverlay() {
        }

        /**
         * Attaches the debug overlay to the permanent
         * game-owned UI system.
         *
         * Installs test windows only when the debug
         * overlay is enabled.
         *
         * Reinstalling the same system is harmless.
         */
        public static void install(UiSystem system) {

                Objects.requireNonNull(system, "system");

                if (UI == system) {
                        return;
                }

                if (UI != null) {
                        throw new IllegalStateException(
                                        "Debug overlay already installed");
                }

                UI = system;

                WINDOWS = system.windows();
                THEMES = system.themes();

                ROOT = system.root();
                INPUT = system.input();

                if (ENABLED) {
                        WINDOWS.register(WINDOW);
                        WINDOWS.register(SECOND_WINDOW);
                        WINDOWS.register(SCROLL_WINDOW);
                }
        }

        // ------------------------------------------------------------
        // Theme registration
        // ------------------------------------------------------------

        /**
         * Allows the debug overlay to change themes.
         *
         * Must be called on the UI/rendering thread.
         */
        public static void setTheme(String themeId) {
                THEMES.select(themeId);
        }

        // ------------------------------------------------------------
        // Window creation
        // ------------------------------------------------------------

        private static UiWindow createWindow() {
                UiWindow window = new UiWindow("New UI Framework");

                window.setPosition(new UiPoint(50, 50));
                window.setPreferredSize(new UiSize(320, 270));

                window.content().setPadding(
                                xaos.ui.layout.UiInsets.all(10));

                window.content().setSpacing(6);

                window.content().add(new xaos.ui.components.UiLabel(
                                "New UI renderer is working!",
                                TownsUiTextMetrics.INSTANCE));

                window.content().add(new xaos.ui.components.UiLabel(
                                "Button rendering preview:",
                                TownsUiTextMetrics.INSTANCE));

                UiButton normal = new UiButton(
                                "Normal Button",
                                TownsUiTextMetrics.INSTANCE);
                normal.setOnClick(() -> window.setTitle("Normal button clicked!"));

                UiButton selected = new UiButton(
                                "Selected Button",
                                TownsUiTextMetrics.INSTANCE);

                selected.setSelected(true);
                selected.setOnClick(() -> selected.setSelected(!selected.selected()));

                UiButton disabled = new UiButton(
                                "Disabled Button",
                                TownsUiTextMetrics.INSTANCE);

                disabled.setEnabled(false);

                window.content().add(normal);
                window.content().add(selected);
                window.content().add(disabled);

                UiButton reopenButton = new UiButton(
                                "Reopen Second Window",
                                TownsUiTextMetrics.INSTANCE);

                reopenButton.setOnClick(
                                UiDebugOverlay::showSecondWindow);

                window.content().add(reopenButton);

                return window;
        }

        private static UiWindow createSecondWindow() {

                UiWindow window = new UiWindow("Second Window");

                // Standard reusable title-bar close button.
                window.setCloseAction(
                                TownsUiTextMetrics.INSTANCE,
                                () -> WINDOWS.close(window));

                window.setPosition(new UiPoint(235, 95));
                window.setPreferredSize(new UiSize(230, 150));

                window.content().setPadding(
                                UiInsets.all(8));

                window.content().setSpacing(8);

                window.content().add(
                                new UiLabel(
                                                "Drag me by the title bar!",
                                                TownsUiTextMetrics.INSTANCE));

                UiButton closeButton = new UiButton(
                                "Close Window",
                                TownsUiTextMetrics.INSTANCE);

                closeButton.setOnClick(
                                () -> WINDOWS.close(window));

                window.content().add(closeButton);

                return window;
        }

        /**
         * Reopens the second window and brings it to the front.
         */

        private static void showSecondWindow() {
                WINDOWS.open(SECOND_WINDOW);
        }

        private static UiWindow createScrollWindow() {

                UiWindow window = new UiWindow("Scrollable UI Test");

                window.setPosition(new UiPoint(450, 105));
                window.setPreferredSize(new UiSize(260, 240));

                window.content().setPadding(UiInsets.all(8));
                window.content().setSpacing(6);

                // ------------------------------------------------------------
                // Text field preview
                // ------------------------------------------------------------

                UiTextField searchField = new UiTextField(
                                "",
                                TownsUiTextMetrics.INSTANCE);

                searchField.setPlaceholder("Search inventory...");

                window.content().add(searchField);

                // ------------------------------------------------------------
                // Scrolling viewport
                // ------------------------------------------------------------

                UiScrollContainer scroll = new UiScrollContainer();

                scroll.content().setSpacing(4);

                // Add enough items to exceed the visible height.
                for (int i = 1; i <= 12; i++) {

                        final int itemNumber = i;

                        UiButton item = new UiButton(
                                        "Inventory Item " + i,
                                        TownsUiTextMetrics.INSTANCE);

                        item.setOnClick(
                                        () -> window.setTitle(
                                                        "Selected Item " + itemNumber));

                        scroll.content().add(item);
                }

                window.content().add(scroll);

                // ------------------------------------------------------------
                // Temporary scrolling controls
                // ------------------------------------------------------------

                UiContainer controls = new UiContainer();

                controls.setLayoutStrategy(
                                UiLinearLayout.HORIZONTAL);

                controls.setSpacing(6);

                UiButton scrollUp = new UiButton(
                                "Scroll Up",
                                TownsUiTextMetrics.INSTANCE);

                scrollUp.setOnClick(
                                () -> scroll.scrollBy(-40));

                UiButton scrollDown = new UiButton(
                                "Scroll Down",
                                TownsUiTextMetrics.INSTANCE);

                scrollDown.setOnClick(
                                () -> scroll.scrollBy(40));

                controls.add(scrollUp);
                controls.add(scrollDown);

                window.content().add(controls);

                return window;
        }

        // ------------------------------------------------------------
        // Mouse input
        // ------------------------------------------------------------

        /**
         * Ensures element positions are current before
         * performing mouse hit testing.
         */
        private static boolean updateLayout() {
                int width = UtilsGL.getWidth();
                int height = UtilsGL.getHeight();

                if (width <= 0 || height <= 0) {
                        return false;
                }

                WINDOWS.updateLayout(new UiSize(width, height));
                return true;
        }

        public static boolean mouseMoved(int x, int y) {
                if (!ENABLED || !updateLayout()) {
                        return false;
                }

                return INPUT.mouseMoved(x, y);
        }

        public static boolean mousePressed(int x, int y) {
                if (!ENABLED || !updateLayout()) {
                        return false;
                }

                return INPUT.mousePressed(x, y);
        }

        public static boolean mouseReleased(int x, int y) {
                if (!ENABLED || !updateLayout()) {
                        return false;
                }

                return INPUT.mouseReleased(x, y);
        }

        /**
         * Routes mouse-wheel input to the new UI.
         */
        public static boolean mouseWheelMoved(
                        int x, int y, int wheelDelta) {

                if (!ENABLED || !updateLayout()) {
                        return false;
                }

                return INPUT.mouseWheelMoved(
                                x, y, wheelDelta);
        }

        // ------------------------------------------------------------
        // Keyboard input
        // ------------------------------------------------------------

        /**
         * Forwards a keyboard press to the new UI.
         *
         * Returns true when the event was consumed.
         */
        public static boolean keyPressed(
                        UiKey key,
                        boolean shiftDown) {

                return ENABLED
                                && INPUT.keyPressed(key, shiftDown);
        }

        /**
         * Returns whether an editable text field
         * currently owns keyboard input.
         */
        public static boolean textInputActive() {

                return ENABLED
                                && INPUT.textInputActive();
        }

        /**
         * Delivers a Unicode character to the
         * currently focused text field.
         */
        public static boolean characterTyped(int codePoint) {

                return ENABLED
                                && INPUT.characterTyped(codePoint);
        }

        /**
         * Releases a key captured by the new UI.
         */
        public static void keyReleased(UiKey key) {

                if (ENABLED) {
                        INPUT.keyReleased(key);
                }
        }

        public static void cancelPointer() {
                INPUT.cancelPointer();
        }

        /**
         * Cancels keyboard and mouse interactions when
         * the application loses focus.
         */

        public static void cancelInput() {
                if (ENABLED) {
                        UI.cancelInput();
                }
        }

}
