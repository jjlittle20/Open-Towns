package xaos.panels;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import xaos.platform.lwjgl3.input.Keyboard;
import xaos.platform.lwjgl3.input.Mouse;
import xaos.platform.lwjgl3.opengl.Display;
import org.lwjgl.opengl.GL11;
import xaos.property.PropertyFile;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiKeyBindingRow;
import xaos.ui.components.UiWindow;
import xaos.ui.integration.UiLegacyCycleBindings;
import xaos.ui.integration.UiLegacyKeyBindings;
import xaos.ui.integration.UiLegacySubmenuBridge;
import xaos.ui.integration.UiLegacyToggleBindings;
import xaos.ui.integration.UiLegacyVolumeBindings;
import xaos.ui.integration.UiMenuNavigation;
import xaos.ui.integration.UiMenuWindowController;
import xaos.ui.integration.UiSimpleMenuWindowBuilder;
import xaos.ui.integration.UiToggleBinding;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.opengl.TownsUiTextMetrics;
import xaos.Towns;
import xaos.TownsProperties;
import xaos.main.Game;
import xaos.panels.menus.ContextMenu;
import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuManager;
import xaos.panels.menus.MenuStateResolver;
import xaos.panels.menus.SmartMenu;
import xaos.utils.ColorGL;
import xaos.utils.Log;
import xaos.utils.Messages;
import xaos.utils.TextureData;
import xaos.utils.UtilFont;
import xaos.utils.Utils;
import xaos.utils.UtilsGL;
import xaos.utils.UtilsKeyboard;
import static xaos.panels.UI.UIPanelState.*;

public final class MainMenuPanel implements Runnable {

        public static int TEXTURE_MAIN_MENU_ID;
        public static int TEXTURE_SMP_LOGO_ID;
        public static int TEXTURE_LOADING_ID;
        public static int TEXTURE_TOWNS_LOGO_ID;

        public boolean active;

        private int renderX;
        private int renderY;
        private int renderWidth;
        private int renderHeight;
        private int xMenu;
        private int yMenu;

        private int imageLoadingWidth;
        private int imageLoadingHeight;
        private int imageTownsLogoWidth;
        private int imageTownsLogoHeight;

        private ContextMenu menu;

        // ------------------------------------------------------------
        // Migrated XML menus
        // ------------------------------------------------------------

        private MenuDefinition rootMenuDefinition;
        private static final String CREDITS_MENU_ID = "main.credits.root";

        private MenuDefinition creditsDefinition;

        private static final String OPTIONS_MENU_ID = "main.options.root";

        private MenuDefinition optionsDefinition;

        private static final String GRAPHICS_MENU_ID = "main.options.graphics.root";

        private MenuDefinition graphicsDefinition;
        private static final String GAME_MENU_ID = "main.options.game.root";

        private static final String AUDIO_MENU_ID = "main.options.audio.root";

        private MenuDefinition audioDefinition;
        private static final String CONTROLS_MENU_ID = "main.options.controls.root";

        private MenuDefinition controlsDefinition;

        private MenuDefinition gameDefinition;

        // Retains the original Options menu and its
        // six legacy submenus during the migration.
        private SmartMenu legacyOptionsMenu;
        private final UiLegacyVolumeBindings legacyVolumeBindings = UiLegacyVolumeBindings.forGame();

        // All migrated menu windows are now managed here.
        private final UiMenuWindowController migratedMenus = new UiMenuWindowController(Game.uiSystem());

        private final UiLegacyToggleBindings legacyToggleBindings = UiLegacyToggleBindings.forGame();
        private final UiLegacyCycleBindings legacyCycleBindings = UiLegacyCycleBindings.forGame();

        public final ColorGL COLORGL_BLACK = new ColorGL(Color.BLACK);
        public final ColorGL COLORGL_WHITE = new ColorGL(Color.WHITE);
        public final ColorGL COLORGL_RED = new ColorGL(Color.RED);

        public static Color textColor = Color.WHITE;
        public static Color creditsColor = Color.YELLOW;
        public static ColorGL borderColor = new ColorGL(Color.BLACK);

        public static float startingGame = 80f;
        public static boolean deleteLogoTexture = false;
        public static boolean loadingGame = false;
        public static boolean useBuryTemporary = true;
        public static ColorGL startingGameColor;
        private String loadingText = new String();

        private boolean settingSavegameName = false;
        private int settingHotkey = 0; // Entero, 0 = cerrado, 1 = abierto y seteando la primera hotkey, 2 = abierto y
                                       // seteando la segunda hotkey
        private boolean settingNewServer = false;
        private String saveGameCampaignID;
        private String saveGameMissionID;

        private String errorToShow = null;

        // Main menu background slideshow
        // Main menu background slideshow
        private static final int MENU_BG_SWITCH_TIME_MS = 30000; // 30 seconds per image
        private static final float MENU_BG_PAN_PIXELS_PER_SECOND = 8f;
        private static final float MENU_BG_OVERSIZE_MULTIPLIER = 1.25f;
        private static final long NANOS_PER_SECOND = 1000000000L;

        private int[] mainMenuBackgroundTextureIds;
        private int[] mainMenuBackgroundWidths;
        private int[] mainMenuBackgroundHeights;

        private int currentMainMenuBackgroundIndex = 0;
        private long mainMenuBackgroundStartTimeNanos;

        public MainMenuPanel(int renderX, int renderY, int renderWidth, int renderHeight) {
                resize(renderX, renderY, renderWidth, renderHeight);

                loadMenuTexture(false);

                TextureData textureSMPLogo = UtilsGL.loadTexture(
                                Towns.getPropertiesString(PropertyFile.PROPERTY_FILE_GRAPHICS, "SMP_LOGO_FILE"), //$NON-NLS-1$
                                GL11.GL_MODULATE);
                if (textureSMPLogo == null) {
                        Log.log(Log.LEVEL_ERROR, Messages.getString("MainMenuPanel.21"), getClass().getName()); //$NON-NLS-1$
                        Game.exit();
                }
                TEXTURE_SMP_LOGO_ID = textureSMPLogo.getTextureID();

                TextureData textureLoading = UtilsGL.loadTexture(
                                Towns.getPropertiesString(PropertyFile.PROPERTY_FILE_GRAPHICS, "LOADING_FILE"), //$NON-NLS-1$
                                GL11.GL_REPLACE);
                if (textureLoading == null) {
                        Log.log(Log.LEVEL_ERROR, Messages.getString("MainMenuPanel.24"), getClass().getName()); //$NON-NLS-1$
                        Game.exit();
                }
                TEXTURE_LOADING_ID = textureLoading.getTextureID();

                imageLoadingWidth = textureLoading.getWidth();
                imageLoadingHeight = textureLoading.getHeight();

                textureLoading = UtilsGL.loadTexture(
                                Towns.getPropertiesString(PropertyFile.PROPERTY_FILE_GRAPHICS, "TOWNS_LOGO_FILE"), //$NON-NLS-1$
                                GL11.GL_REPLACE); // $NON-NLS-2$
                if (textureLoading == null) {
                        Log.log(Log.LEVEL_ERROR, Messages.getString("MainMenuPanel.25"), getClass().getName()); //$NON-NLS-1$
                        Game.exit();
                }

                TEXTURE_TOWNS_LOGO_ID = textureLoading.getTextureID();
                imageTownsLogoWidth = textureLoading.getWidth();
                imageTownsLogoHeight = textureLoading.getHeight();

                setErrorToShow(null);

                startingGame = 80;
                startingGameColor = new ColorGL(new Color(255, 255, 255));
                loadingGame = false;
                deleteLogoTexture = false;
                settingSavegameName = false;
                settingHotkey = 0;

                settingNewServer = false;

                // Register the menus already migrated to UiSystem.
                //
                // Future migrations only need another registration
                // here, plus their window implementation.

                migratedMenus.register(
                                CREDITS_MENU_ID,
                                this::buildCreditsWindow);

                // Migrate the Options root screen.
                // Child settings screens still use SmartMenu.
                migratedMenus.register(
                                OPTIONS_MENU_ID,
                                this::buildOptionsWindow);

                // Graphics now has a new-framework implementation.
                migratedMenus.register(
                                GRAPHICS_MENU_ID,
                                this::buildGraphicsWindow);
                // Game settings now use the new UI framework.
                migratedMenus.register(
                                GAME_MENU_ID,
                                this::buildGameWindow);
                migratedMenus.register(
                                AUDIO_MENU_ID,
                                this::buildAudioWindow);
                migratedMenus.register(
                                CONTROLS_MENU_ID,
                                this::buildControlsWindow);

                createMenu();
                new Thread(this).start();

                if (TownsProperties.DEBUG_MODE) {
                        startingGame = 0;
                }
        }

        // MAIN MENU
        public void createMenu() {
                // Rebuilding the main menu closes any migrated screen.
                migratedMenus.closeAll();
                legacyOptionsMenu = null;
                setLoadingText(new String());
                loadingGame = false;
                MenuManager menuManager = new MenuManager();
                menuManager.loadMenus(new File("data/menus/main"));

                MenuDefinition menuRoot = menuManager.getMenu("main.root");
                rootMenuDefinition = menuRoot;

                // Keep the XML Credits definition for the new UI.
                creditsDefinition = menuManager.getMenu(CREDITS_MENU_ID);

                if (creditsDefinition == null) {
                        throw new IllegalStateException(
                                        "Missing Credits menu: " + CREDITS_MENU_ID);
                }

                optionsDefinition = menuManager.getMenu(OPTIONS_MENU_ID);

                if (optionsDefinition == null) {
                        throw new IllegalStateException(
                                        "Missing Options menu: " + OPTIONS_MENU_ID);
                }

                graphicsDefinition = menuManager.getMenu(GRAPHICS_MENU_ID);

                if (graphicsDefinition == null) {
                        throw new IllegalStateException(
                                        "Missing Graphics menu: "
                                                        + GRAPHICS_MENU_ID);
                }
                gameDefinition = menuManager.getMenu(GAME_MENU_ID);

                if (gameDefinition == null) {
                        throw new IllegalStateException(
                                        "Missing Game settings menu: "
                                                        + GAME_MENU_ID);
                }

                audioDefinition = menuManager.getMenu(AUDIO_MENU_ID);

                if (audioDefinition == null) {
                        throw new IllegalStateException(
                                        "Missing Audio settings menu: "
                                                        + AUDIO_MENU_ID);
                }

                controlsDefinition = menuManager.getMenu(CONTROLS_MENU_ID);

                if (controlsDefinition == null) {
                        throw new IllegalStateException(
                                        "Missing Controls settings menu: "
                                                        + CONTROLS_MENU_ID);
                }

                SmartMenu mainMenu = menuManager.buildSmartMenu(menuRoot, null);

                menu = new ContextMenu();
                menu.setHeight(MainFrame.MIN_HEIGHT - UtilFont.MAX_HEIGHT * 8);
                menu.setSmartMenu(mainMenu);

                repositionMenu();
        }

        /**
         * Creates the Credits screen from its existing
         * XML definition.
         *
         * The UiMenuWindowController handles registration,
         * opening, closing and reuse.
         */
        private UiWindow buildCreditsWindow() {

                if (creditsDefinition == null) {
                        throw new IllegalStateException(
                                        "Credits XML definition not loaded");
                }

                // Build controls from the original XML.
                UiWindow window = UiSimpleMenuWindowBuilder.build(
                                creditsDefinition,
                                Messages::getString,
                                TownsUiTextMetrics.INSTANCE,
                                () -> migratedMenus.close(
                                                CREDITS_MENU_ID));

                // --------------------------------------------------------
                // Menu-style window configuration
                // --------------------------------------------------------

                window.setCentered(true);
                window.setDraggable(false);
                window.setResizable(false);
                window.setModal(true);

                return window;
        }

        /**
         * Builds the new Options root menu from XML.
         *
         * Individual settings screens remain legacy
         * until their controls are migrated.
         */
        private UiWindow buildOptionsWindow() {

                if (optionsDefinition == null) {
                        throw new IllegalStateException(
                                        "Options XML definition not loaded");
                }

                UiWindow window = UiSimpleMenuWindowBuilder.build(
                                optionsDefinition,
                                Messages::getString,
                                TownsUiTextMetrics.INSTANCE,

                                // Back returns to the main menu.
                                () -> migratedMenus.close(
                                                OPTIONS_MENU_ID),

                                // Until migrated, child menus
                                // use their original SmartMenu.
                                this::openOptionsSubmenu);

                // Same menu-screen behaviour as Credits.
                window.setCentered(true);
                window.setDraggable(false);
                window.setResizable(false);
                window.setModal(true);

                return window;
        }

        /**
         * Builds the Graphics settings window from
         * the original Towns XML definition.
         */
        private UiWindow buildGraphicsWindow() {

                if (graphicsDefinition == null) {
                        throw new IllegalStateException(
                                        "Graphics XML definition not loaded");
                }

                UiWindow window = UiSimpleMenuWindowBuilder.build(
                                graphicsDefinition,
                                Messages::getString,
                                TownsUiTextMetrics.INSTANCE,

                                // Back returns to the new Options root.
                                () -> returnToOptions(GRAPHICS_MENU_ID),

                                // Graphics has no child submenus.
                                null,

                                // Shared XML-to-command toggle bindings.
                                legacyToggleBindings::forItem);

                // Same behaviour as Credits and Options.
                window.setCentered(true);
                window.setDraggable(false);
                window.setResizable(false);
                window.setModal(true);

                // The XML builder automatically installs
                // the standard X button from Step 37B.1.

                return window;
        }

        /**
         * Builds the Game settings screen from the original
         * Towns XML definition.
         *
         * Uses the existing game commands and settings state.
         *
         * The UiMenuWindowController handles registration,
         * opening, closing and window reuse.
         */
        private UiWindow buildGameWindow() {

                if (gameDefinition == null) {
                        throw new IllegalStateException(
                                        "Game settings XML definition not loaded");
                }

                // --------------------------------------------------------
                // Construct controls directly from XML
                // --------------------------------------------------------

                UiWindow window = UiSimpleMenuWindowBuilder.build(
                                gameDefinition,
                                Messages::getString,
                                TownsUiTextMetrics.INSTANCE,

                                // Back and X return to Options.
                                () -> returnToOptions(GAME_MENU_ID),

                                // No child submenus in Game settings.
                                null,

                                // Seven boolean settings.
                                legacyToggleBindings::forItem,

                                // Two cycle selectors.
                                legacyCycleBindings::forItem);

                // --------------------------------------------------------
                // Window behaviour
                // --------------------------------------------------------

                window.setCentered(true);
                window.setDraggable(false);
                window.setResizable(false);
                window.setModal(true);

                // Slightly taller than Credits or Graphics.
                // Remaining content can be scrolled.
                window.setPreferredSize(
                                new UiSize(480, 380));

                // The XML builder already installs the
                // standard title-bar X close button.
                return window;
        }

        /**
         * Returns from a migrated Options child
         * to the new Options root window.
         *
         * Both the XML Back button and standard
         * title-bar X use this method.
         */
        private void returnToOptions(String childMenuId) {

                migratedMenus.close(childMenuId);

                migratedMenus.open(OPTIONS_MENU_ID);
        }

        /**
         * Opens an Options category using the new
         * framework when available.
         *
         * Unmigrated categories continue using the
         * original SmartMenu implementation.
         */
        private void openOptionsSubmenu(
                        String targetMenuId) {

                if (migratedMenus.isRegistered(targetMenuId)) {

                        // Replace the Options root modal
                        // with the selected migrated category.
                        migratedMenus.close(OPTIONS_MENU_ID);

                        migratedMenus.open(targetMenuId);

                        return;
                }

                // Temporary compatibility path for
                // unmigrated settings categories.
                openLegacyOptionsSubmenu(targetMenuId);
        }

        /**
         * Builds the Controls settings screen from XML.
         *
         * Five XML-defined tabs organise the original
         * 37 keyboard actions.
         *
         * Assignments continue to be owned and saved
         * by UtilsKeyboard.
         */
        private UiWindow buildControlsWindow() {

                if (controlsDefinition == null) {
                        throw new IllegalStateException(
                                        "Controls settings XML definition not loaded");
                }

                // --------------------------------------------------------
                // Track the binding rows
                // --------------------------------------------------------

                // Reassigning one key can clear that key from
                // another action. All visible rows must refresh.
                //
                // Keep this list local to the window so it stays
                // associated with the controller's cached window.
                List<UiKeyBindingRow> bindingRows = new ArrayList<>();

                Runnable refreshAllBindings = () -> {
                        for (UiKeyBindingRow row : bindingRows) {
                                row.refresh();
                        }
                };

                // --------------------------------------------------------
                // Original Towns keyboard integration
                // --------------------------------------------------------

                UiLegacyKeyBindings keyBindings = new UiLegacyKeyBindings(
                                Game.uiSystem().input(),
                                TownsUiTextMetrics.INSTANCE,
                                refreshAllBindings);

                // --------------------------------------------------------
                // Build the menu from the XML
                // --------------------------------------------------------

                UiWindow window = UiSimpleMenuWindowBuilder.build(
                                controlsDefinition,
                                Messages::getString,
                                TownsUiTextMetrics.INSTANCE,

                                // Back and X return to Options.
                                () -> returnToOptions(CONTROLS_MENU_ID),

                                // No child submenus.
                                null,

                                // No toggles, cycles or sliders.
                                null,
                                null,
                                null,

                                // XML keyboard item factory.
                                item -> {

                                        UiKeyBindingRow row = keyBindings.forItem(item);

                                        bindingRows.add(row);

                                        return row;
                                });

                // --------------------------------------------------------
                // Window configuration
                // --------------------------------------------------------

                window.setCentered(true);
                window.setDraggable(false);
                window.setResizable(false);
                window.setModal(true);

                // Wider than the other settings screens
                // to accommodate two key buttons per action.
                window.setPreferredSize(
                                new UiSize(660, 460));

                return window;
        }

        /**
         * Opens an unmigrated Options submenu using
         * the original SmartMenu implementation.
         *
         * The new Options root is closed temporarily.
         * Returning via the legacy Back button will
         * reopen the new Options root.
         */
        private void openLegacyOptionsSubmenu(
                        String targetMenuId) {

                if (legacyOptionsMenu == null) {
                        throw new IllegalStateException(
                                        "Legacy Options menu is unavailable");
                }

                // Resolve the exact original submenu using
                // the XML target ID.
                SmartMenu child = UiLegacySubmenuBridge.find(
                                optionsDefinition,
                                legacyOptionsMenu,
                                targetMenuId);

                // Close the new modal before showing the
                // original submenu.
                migratedMenus.close(OPTIONS_MENU_ID);

                // Activate the original settings menu.
                // Its commands, dynamic values and controls
                // continue working unchanged.
                menu.setSmartMenu(child);

                repositionMenu();
        }

        /**
         * Builds the Audio settings screen using
         * the original Towns XML definition.
         *
         * Music and FX toggles use existing commands.
         * Volume sliders use live audio bindings.
         */
        private UiWindow buildAudioWindow() {

                if (audioDefinition == null) {
                        throw new IllegalStateException(
                                        "Audio settings XML definition not loaded");
                }

                UiWindow window = UiSimpleMenuWindowBuilder.build(
                                audioDefinition,
                                Messages::getString,
                                TownsUiTextMetrics.INSTANCE,

                                // Back and X return to Options.
                                () -> returnToOptions(AUDIO_MENU_ID),

                                // No child submenus.
                                null,

                                // Music and FX toggles.
                                legacyToggleBindings::forItem,

                                // No cycle selectors.
                                null,

                                // Music and FX volume sliders.
                                legacyVolumeBindings::forItem);

                // Standard menu-screen behaviour.
                window.setCentered(true);
                window.setDraggable(false);
                window.setResizable(false);
                window.setModal(true);

                window.setPreferredSize(
                                new UiSize(460, 340));

                // X button is added automatically by the
                // XML builder, just like Credits and Options.
                return window;
        }

        public void resize(int renderX, int renderY, int renderWidth, int renderHeight) {
                this.renderX = renderX;
                this.renderY = renderY;
                this.renderWidth = renderWidth;
                this.renderHeight = renderHeight;

                if (menu != null) {
                        repositionMenu();
                }
        }

        public static SmartMenu createKeyboardMenu(int iFN, Color textColor, ColorGL borderColor) {
                SmartMenu menuAux = new SmartMenu(
                                SmartMenu.TYPE_KEYBOARD,
                                UtilsKeyboard.getFNHumanString(iFN) + UtilsKeyboard.getTooltip(iFN),
                                null,
                                CommandPanel.COMMAND_CHANGE_HOTKEY,
                                Integer.toString(iFN),
                                null,
                                null,
                                textColor);

                menuAux.setBorderColor(borderColor);
                return menuAux;
        }

        public boolean isSettingSavegameName() {
                return settingSavegameName;
        }

        public void setSettingSavegameName(boolean settingSavegameName, String sCampaign, String sMission) {
                this.settingSavegameName = settingSavegameName;
                setSaveGameCampaignID(sCampaign);
                setSaveGameMissionID(sMission);
                if (settingSavegameName) {
                        new TypingPanel(renderWidth, renderHeight, Messages.getString("MainMenuPanel.47"), new String(), //$NON-NLS-1$
                                        TypingPanel.TYPE_SAVEGAME_NAME, 0);
                }
        }

        public boolean isSettingHotkey() {
                return settingHotkey != 0;
        }

        public void setSettingHotkey(boolean settingHotkey, int iFN) {
                if (settingHotkey) {
                        this.settingHotkey = 1;
                        new TypingPanel(renderWidth, renderHeight,
                                        Messages.getString("MainMenuPanel.48") + UtilsKeyboard.getFNHumanString(iFN) //$NON-NLS-1$
                                                        + ((UtilsKeyboard.getKey(iFN, 0) == Keyboard.KEY_NONE) ? "" //$NON-NLS-1$
                                                                        : Messages.getString("MainMenuPanel.50") //$NON-NLS-1$
                                                                                        + Keyboard.getKeyName(
                                                                                                        UtilsKeyboard.getKey(
                                                                                                                        iFN,
                                                                                                                        0))
                                                                                        + ")"), //$NON-NLS-1$
                                        new String(), TypingPanel.TYPE_REDEFINE_KEYS, iFN);
                } else {
                        this.settingHotkey = 0;
                        // El panel se cierra, cambiamos los textos de todos los menus
                        // COMMAND_CHANGE_HOTKEY
                        checkChangeHotkeyMenusText(menu.getSmartMenu());
                }
        }

        public void setSettingNewServer(boolean bSettingNewServer) {
                this.settingNewServer = bSettingNewServer;

                if (bSettingNewServer) {
                        new TypingPanel(renderWidth, renderHeight, Messages.getString("MainMenuPanel.76"), new String(), //$NON-NLS-1$
                                        TypingPanel.TYPE_ADD_SERVER, 0);
                }
        }

        public boolean isSettingNewServer() {
                return settingNewServer;
        }

        /**
         * Cambia los textos de todos los menus COMMAND_CHANGE_HOTKEY Se llama
         * después deredifinir alguna tecla.
         *
         */
        private void checkChangeHotkeyMenusText(SmartMenu sm) {
                if (sm.getType() == SmartMenu.TYPE_MENU) {
                        for (int i = 0; i < sm.getItems().size(); i++) {
                                checkChangeHotkeyMenusText(sm.getItems().get(i));
                        }
                } else if (sm.getType() == SmartMenu.TYPE_ITEM && sm.getCommand() != null
                                && sm.getCommand().equals(CommandPanel.COMMAND_CHANGE_HOTKEY)) {
                        // Bingo, cambiamos el texto
                        int iFN = Integer.parseInt(sm.getParameter());
                        sm.setName(UtilsKeyboard.getFNHumanString(iFN) + UtilsKeyboard.getTooltip(iFN));
                }
        }

        public void render() {

                GL11.glColor4f(1, 1, 1, 1);

                int iMaxSize = renderWidth;
                if (renderHeight > iMaxSize) {
                        iMaxSize = renderHeight;
                }
                if (iMaxSize % 2 != 0) {
                        iMaxSize++;
                }
                iMaxSize /= 2;

                int centerX = (renderWidth - renderX) / 2;
                int centerY = (renderHeight - renderY) / 2;

                if (startingGame > 0) {
                        // Pintamos el logo
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, TEXTURE_SMP_LOGO_ID);
                        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
                        UtilsGL.glBegin(GL11.GL_QUADS);
                        UtilsGL.drawTexture(centerX - iMaxSize, centerY - iMaxSize, centerX + iMaxSize,
                                        centerY + iMaxSize, 0, 0, 1,
                                        1, startingGameColor);
                        UtilsGL.glEnd();
                        return;
                } else {
                        if (deleteLogoTexture) {
                                deleteLogoTexture = false;

                                // Descargamos el logo de la memoria
                                GL11.glDeleteTextures(TEXTURE_SMP_LOGO_ID);
                        }
                }

                // Pintamos el fondo
                renderMainMenuBackground();

                // Pintamos el logo Towns
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, TEXTURE_TOWNS_LOGO_ID);
                GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);

                int logoYOffset = 220;

                UtilsGL.glBegin(GL11.GL_QUADS);
                UtilsGL.drawTexture(
                                centerX - imageTownsLogoWidth / 2,
                                centerY - 10 - imageTownsLogoHeight - logoYOffset,
                                centerX + imageTownsLogoWidth / 2,
                                centerY - 10 - logoYOffset,
                                0,
                                0,
                                1,
                                1);
                UtilsGL.glEnd();

                boolean bTextureFontLoaded = false;
                if (loadingGame) {
                        // Pintamos el loading
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, TEXTURE_LOADING_ID);
                        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
                        UtilsGL.glBegin(GL11.GL_QUADS);
                        UtilsGL.drawTexture(centerX - imageLoadingWidth / 2, centerY + 10,
                                        centerX + imageLoadingWidth / 2,
                                        centerY + 10 + imageLoadingHeight, 0, 0, 1, 1);
                        UtilsGL.glEnd();

                        // Si hay texto de loading lo pintamos también
                        String sLoadingText = getLoadingText();
                        if (sLoadingText.length() > 0) {
                                bTextureFontLoaded = true;
                                GL11.glBindTexture(GL11.GL_TEXTURE_2D, Game.TEXTURE_FONT_ID);
                                GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);

                                UtilsGL.glBegin(GL11.GL_QUADS);
                                UtilsGL.drawStringWithBorder(sLoadingText,
                                                centerX - (UtilFont.getWidth(sLoadingText) / 2),
                                                centerY + 20 + imageLoadingHeight, COLORGL_WHITE, COLORGL_BLACK);
                                UtilsGL.glEnd();
                        }
                }

                // Versión del juego abajo a la derecha
                if (!bTextureFontLoaded) {
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, Game.TEXTURE_FONT_ID);
                        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
                }
                UtilsGL.glBegin(GL11.GL_QUADS);
                String sVersion = TownsProperties.GAME_VERSION;
                if (TownsProperties.DEMO_VERSION) {
                        sVersion += " (demo)"; //$NON-NLS-1$
                }
                UtilsGL.drawStringWithBorder(sVersion, renderX + renderWidth - UtilFont.getWidth(sVersion) - 10,
                                renderY + renderHeight - UtilFont.MAX_HEIGHT - 10, COLORGL_WHITE, COLORGL_BLACK);

                // Texto a la izquierda
                if (TownsProperties.DEMO_VERSION) {
                        UtilsGL.drawStringWithBorder(Messages.getString("MainMenuPanel.23"), 11, //$NON-NLS-1$
                                        renderY + renderHeight - UtilFont.MAX_HEIGHT - 10, COLORGL_WHITE,
                                        COLORGL_BLACK);
                }
                UtilsGL.glEnd();

                if (!loadingGame && !isSettingSavegameName() && !isSettingHotkey() && !isSettingNewServer()
                                && !migratedMenus.isAnyOpen()) {
                        // Pintamos el menú
                        menu.render();
                }

                if (isSettingSavegameName() || isSettingHotkey() || isSettingNewServer()) {
                        int mouseX = Mouse.getEventX();
                        int mouseY = UtilsGL.getHeight() - Mouse.getEventY() - 1;
                        TypingPanel.render(mouseX, mouseY);
                }
        }

        private void renderMainMenuBackground() {
                if (mainMenuBackgroundTextureIds == null || mainMenuBackgroundTextureIds.length == 0) {
                        return;
                }

                updateMainMenuBackground();

                int textureId = mainMenuBackgroundTextureIds[currentMainMenuBackgroundIndex];

                float screenWidth = renderWidth;
                float screenHeight = renderHeight;

                float drawWidth = screenWidth * MENU_BG_OVERSIZE_MULTIPLIER;
                float drawHeight = screenHeight * MENU_BG_OVERSIZE_MULTIPLIER;

                float maxOffsetX = drawWidth - screenWidth;
                float maxOffsetY = drawHeight - screenHeight;

                if (maxOffsetX <= 0 || maxOffsetY <= 0) {
                        return;
                }

                long now = System.nanoTime();
                float elapsedSeconds = (now - mainMenuBackgroundStartTimeNanos) / (float) NANOS_PER_SECOND;

                float offsetX = (elapsedSeconds * MENU_BG_PAN_PIXELS_PER_SECOND) % maxOffsetX;
                float offsetY = (elapsedSeconds * MENU_BG_PAN_PIXELS_PER_SECOND) % maxOffsetY;

                float drawX = renderX - offsetX;
                float drawY = renderY - offsetY;

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
                GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);

                GL11.glBegin(GL11.GL_QUADS);

                GL11.glTexCoord2f(0f, 0f);
                GL11.glVertex2f(drawX, drawY);

                GL11.glTexCoord2f(1f, 0f);
                GL11.glVertex2f(drawX + drawWidth, drawY);

                GL11.glTexCoord2f(1f, 1f);
                GL11.glVertex2f(drawX + drawWidth, drawY + drawHeight);

                GL11.glTexCoord2f(0f, 1f);
                GL11.glVertex2f(drawX, drawY + drawHeight);

                GL11.glEnd();
        }

        private void updateMainMenuBackground() {
                long now = System.nanoTime();

                long elapsedNanos = now - mainMenuBackgroundStartTimeNanos;
                long switchTimeNanos = MENU_BG_SWITCH_TIME_MS * 1000000L;

                if (elapsedNanos >= switchTimeNanos) {
                        currentMainMenuBackgroundIndex++;

                        if (currentMainMenuBackgroundIndex >= mainMenuBackgroundTextureIds.length) {
                                currentMainMenuBackgroundIndex = 0;
                        }

                        TEXTURE_MAIN_MENU_ID = mainMenuBackgroundTextureIds[currentMainMenuBackgroundIndex];

                        mainMenuBackgroundStartTimeNanos = now;
                }
        }

        public void loadMenuTexture(boolean bUnload) {
                String[] backgroundPropertyKeys = {
                                "MAINMENU_BG_FILE1",
                                "MAINMENU_BG_FILE2",
                                "MAINMENU_BG_FILE3"
                };

                mainMenuBackgroundTextureIds = new int[backgroundPropertyKeys.length];
                mainMenuBackgroundWidths = new int[backgroundPropertyKeys.length];
                mainMenuBackgroundHeights = new int[backgroundPropertyKeys.length];

                for (int i = 0; i < backgroundPropertyKeys.length; i++) {
                        TextureData textureMainMenu = UtilsGL.loadTexture(
                                        Towns.getPropertiesString(
                                                        PropertyFile.PROPERTY_FILE_GRAPHICS,
                                                        backgroundPropertyKeys[i]),
                                        GL11.GL_REPLACE,
                                        true);

                        if (textureMainMenu == null) {
                                Log.log(
                                                Log.LEVEL_ERROR,
                                                "Could not load main menu background: " + backgroundPropertyKeys[i],
                                                getClass().getName());
                                Game.exit();
                        }

                        mainMenuBackgroundTextureIds[i] = textureMainMenu.getTextureID();
                        mainMenuBackgroundWidths[i] = textureMainMenu.getWidth();
                        mainMenuBackgroundHeights[i] = textureMainMenu.getHeight();

                        // Smooth animated background panning.
                        // Keep this local to menu backgrounds only so pixel art/UI textures stay sharp.
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainMenuBackgroundTextureIds[i]);
                        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                }

                currentMainMenuBackgroundIndex = 0;
                mainMenuBackgroundStartTimeNanos = System.nanoTime();

                // Compatibility with any old code that still expects this texture id.
                TEXTURE_MAIN_MENU_ID = mainMenuBackgroundTextureIds[0];
        }

        /**
         * Método que se llama cuando pulse con el ratón.
         *
         * @param x Coordenada X
         * @param y Coordenada Y
         */
        public void mousePressed(int x, int y, int mouseButton) {
                if (startingGame > 0) {
                        startingGame = 0;
                        return;
                }

                if (mouseButton != 0) {
                        return;
                }

                // The migrated Credits window owns this screen.
                // Do not interact with the underlying main menu.
                if (migratedMenus.isAnyOpen()) {
                        return;
                }

                if (isSettingSavegameName()) {
                        int iMousePanel = TypingPanel.whereIsMouse(x, y);

                        if (iMousePanel == MOUSE_TYPING_PANEL_CLOSE) {
                                setSettingSavegameName(false, null, null);
                        } else if (iMousePanel == MOUSE_TYPING_PANEL_CONFIRM) {
                                if (TypingPanel.getNewText() != null
                                                && TypingPanel.getNewText().length() > 0) {
                                        if (!Utils.existsSavegame(TypingPanel.getNewText())) {
                                                startGame(TypingPanel.getNewText());
                                        }
                                }
                        }

                        return;
                }

                if (isSettingHotkey()) {
                        int iMousePanel = TypingPanel.whereIsMouse(x, y);

                        if (iMousePanel == MOUSE_TYPING_PANEL_CLOSE) {
                                setSettingHotkey(false, 0);
                        }

                        return;
                }

                if (isSettingNewServer()) {
                        int iMousePanel = TypingPanel.whereIsMouse(x, y);

                        if (iMousePanel == MOUSE_TYPING_PANEL_CLOSE) {
                                setSettingNewServer(false);
                        } else if (iMousePanel == MOUSE_TYPING_PANEL_CONFIRM) {
                                if (TypingPanel.getNewText() != null
                                                && TypingPanel.getNewText().length() > 0) {
                                        Game.addServer(TypingPanel.getNewText());
                                        Utils.saveOptions();

                                        setSettingNewServer(false);
                                        createMenu();
                                }
                        }

                        return;
                }

                if (!loadingGame && menu != null) {

                        SmartMenu beforeMenu = menu.getSmartMenu();

                        menu.mousePressed(
                                        x - menu.getX(),
                                        y - menu.getY());

                        SmartMenu afterMenu = menu.getSmartMenu();

                        if (beforeMenu != afterMenu) {

                                // --------------------------------------------------------
                                // Returning from a legacy Options child
                                // --------------------------------------------------------

                                if (legacyOptionsMenu != null
                                                && beforeMenu != null
                                                && beforeMenu.getParent() == legacyOptionsMenu
                                                && afterMenu == legacyOptionsMenu) {

                                        // The user clicked Back inside a legacy
                                        // settings submenu.
                                        //
                                        // Restore the legacy main-menu root,
                                        // then reopen the migrated Options root.
                                        menu.setSmartMenu(
                                                        legacyOptionsMenu.getParent());

                                        repositionMenu();

                                        migratedMenus.open(OPTIONS_MENU_ID);
                                        return;
                                }

                                // --------------------------------------------------------
                                // Existing migrated-menu navigation
                                // --------------------------------------------------------

                                Optional<Runnable> migratedAction = afterMenu == null
                                                ? Optional.empty()
                                                : migratedMenus.resolve(
                                                                rootMenuDefinition,
                                                                afterMenu.getID());

                                if (migratedAction.isPresent()) {

                                        // Save the original Options menu instance
                                        // for its still-unmigrated child screens.
                                        if (afterMenu != null
                                                        && "main.options".equals(
                                                                        afterMenu.getID())) {

                                                legacyOptionsMenu = afterMenu;
                                        }

                                        // Restore the original menu underneath
                                        // the new modal window.
                                        menu.setSmartMenu(beforeMenu);

                                        repositionMenu();

                                        migratedAction.get().run();

                                } else {

                                        // Unmigrated navigation remains unchanged.
                                        repositionMenu();
                                }
                        }

                }
        }

        private void repositionMenu() {
                if (menu == null || menu.getSmartMenu() == null) {
                        return;
                }

                menu.resize();

                xMenu = renderX + (renderWidth - menu.getWidth()) / 2;
                yMenu = renderY + (renderHeight - menu.getHeight()) / 2;

                if (xMenu < renderX + 20) {
                        xMenu = renderX + 20;
                }

                if (yMenu < renderY + 20) {
                        yMenu = renderY + 20;
                }

                menu.setX(xMenu);
                menu.setY(yMenu);
        }

        public void mouseWheelMoved(int amount, int mouseX, int mouseY) {
                if (migratedMenus.isAnyOpen()) {
                        return;
                }
                if (menu == null || menu.getSmartMenu() == null) {
                        return;
                }

                if (mouseX < menu.getX()
                                || mouseX >= menu.getX() + menu.getWidth()
                                || mouseY < menu.getY()
                                || mouseY >= menu.getY() + menu.getHeight()) {
                        return;
                }

                menu.mouseWheelMoved(amount);
        }

        /**
         * Método llamado al pulsar una tecla cuando estamos en el main menu
         *
         * @param iKey
         */
        public void keyPressed(int iKey) {
                if (isSettingSavegameName()) {
                        if (TypingPanel.keyPressed(iKey)) {
                                // Ya ha acabado (o ha pulsado ESC)
                                if (TypingPanel.getNewText() != null && TypingPanel.getNewText().length() > 0) {
                                        // Todo ok, toca empezar la partida (sólo si la partida no existe previamente en
                                        // disco)
                                        if (!Utils.existsSavegame(TypingPanel.getNewText())) {
                                                startGame(TypingPanel.getNewText());
                                        }
                                } else {
                                        setSettingSavegameName(false, null, null);
                                }
                        }
                } else if (isSettingHotkey()) {
                        if (TypingPanel.keyPressed(iKey)) {
                                // Ya ha acabado (o ha pulsado ESC)
                                if (TypingPanel.getNewText() != null && TypingPanel.getNewText().length() > 0) {
                                        // Key pulsada, la seteamos y saltamos a la siguiente (si hace falta)
                                        if (settingHotkey == 1) {
                                                UtilsKeyboard.redefineKey(0, TypingPanel.TYPING_PARAMETER,
                                                                Integer.parseInt(TypingPanel.getNewText()));
                                                settingHotkey = 2;
                                                TypingPanel
                                                                .setTitle(Messages.getString("MainMenuPanel.52") //$NON-NLS-1$
                                                                                + UtilsKeyboard.getFNHumanString(
                                                                                                TypingPanel.TYPING_PARAMETER)
                                                                                + ((UtilsKeyboard.getKey(
                                                                                                TypingPanel.TYPING_PARAMETER,
                                                                                                0) == Keyboard.KEY_NONE)
                                                                                                                ? "" //$NON-NLS-1$
                                                                                                                : Messages.getString(
                                                                                                                                "MainMenuPanel.50") //$NON-NLS-1$
                                                                                                                                + Keyboard.getKeyName(
                                                                                                                                                UtilsKeyboard.getKey(
                                                                                                                                                                TypingPanel.TYPING_PARAMETER,
                                                                                                                                                                1))
                                                                                                                                + ")")); //$NON-NLS-1$
                                                TypingPanel.setNewText(new String());
                                        } else {
                                                // Key 2
                                                UtilsKeyboard.redefineKey(1, TypingPanel.TYPING_PARAMETER,
                                                                Integer.parseInt(TypingPanel.getNewText()));
                                                setSettingHotkey(false, 0);
                                        }
                                } else {
                                        // Ha pulsado ESC, borramos hotkeys
                                        if (settingHotkey == 1) {
                                                UtilsKeyboard.redefineKey(0, TypingPanel.TYPING_PARAMETER,
                                                                Keyboard.KEY_NONE);
                                                UtilsKeyboard.redefineKey(1, TypingPanel.TYPING_PARAMETER,
                                                                Keyboard.KEY_NONE);
                                        } else {
                                                // Key 2
                                                UtilsKeyboard.redefineKey(1, TypingPanel.TYPING_PARAMETER,
                                                                Keyboard.KEY_NONE);
                                        }

                                        setSettingHotkey(false, 0);
                                }
                        }
                } else if (isSettingNewServer()) {
                        if (TypingPanel.keyPressed(iKey)) {
                                // Ya ha acabado (o ha pulsado ESC)
                                if (TypingPanel.getNewText() != null && TypingPanel.getNewText().length() > 0) {
                                        Game.addServer(TypingPanel.getNewText());
                                        Utils.saveOptions();

                                        createMenu();
                                }

                                setSettingNewServer(false);
                        }
                }
        }

        private void startGame(String savegameName) {
                setSettingSavegameName(false, getSaveGameCampaignID(), getSaveGameMissionID());
                CommandPanel.executeCommand(CommandPanel.COMMAND_MM_NEWGAME, getSaveGameCampaignID(),
                                getSaveGameMissionID(),
                                null, null, 0);
                Game.setSavegameName(savegameName);
                useBuryTemporary = true;
        }

        public void setActive(boolean bActive) {

                if (bActive) {
                        createMenu();
                } else {
                        // Migrated main-menu windows must not
                        // remain visible during gameplay.
                        migratedMenus.closeAll();
                }

                active = bActive;
        }

        public void setLoadingText(String sLoadingText) {
                loadingText = sLoadingText;
                if (sLoadingText.length() > 0) {
                        if (isActive()) {
                                Game.getPanelMainMenu().render();
                                // Updateamos la pantalla / ventana
                                Display.update();
                                Display.sync(Game.FPS_MAINMENU); // Para "capear" a 30 fps
                                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                                                | GL11.GL_ACCUM_BUFFER_BIT
                                                | GL11.GL_STENCIL_BUFFER_BIT);
                        }
                }
        }

        public String getLoadingText() {
                return loadingText;
        }

        public boolean isActive() {
                return active;
        }

        // public void resize(int renderX, int renderY, int renderWidth, int
        // renderHeight) {
        // this.renderX = renderX;
        // this.renderY = renderY;
        // this.renderWidth = renderWidth;
        // this.renderHeight = renderHeight;
        // this.xMenu = 20;
        // this.yMenu = 20;

        // if (menu != null) {
        // menu.setHeight(MainFrame.MIN_HEIGHT - UtilFont.MAX_HEIGHT * 8);
        // menu.setX(xMenu);
        // menu.setY(yMenu);
        // menu.resize();
        // }
        // }

        public void run() {
                // Starting
                while (startingGame > 0) {
                        try {
                                startingGame--;
                                Thread.sleep(48);
                        } catch (Exception e) {
                        }

                        if (startingGame < 25) {
                                startingGameColor.r = ((startingGame - 5) * 5f) / 100f;
                                startingGameColor.g = ((startingGame - 5) * 5f) / 100f;
                                startingGameColor.b = ((startingGame - 5) * 5f) / 100f;
                        }
                }

                startingGame = 0;
                deleteLogoTexture = true;
        }

        public void setSaveGameCampaignID(String saveGameCampaignID) {
                this.saveGameCampaignID = saveGameCampaignID;
        }

        public String getSaveGameCampaignID() {
                return saveGameCampaignID;
        }

        public void setSaveGameMissionID(String saveGameMissionID) {
                this.saveGameMissionID = saveGameMissionID;
        }

        public String getSaveGameMissionID() {
                return saveGameMissionID;
        }

        /**
         * @param errorToShow the errorToShow to set
         */
        public void setErrorToShow(String errorToShow) {
                this.errorToShow = errorToShow;
        }

        /**
         * @return the errorToShow
         */
        public String getErrorToShow() {
                return errorToShow;
        }
}
