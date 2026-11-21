package xaos.panels.menus;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.awt.Color;

import xaos.TownsProperties;
import xaos.campaign.CampaignData;
import xaos.campaign.CampaignManager;
import xaos.utils.Point3D;
import xaos.main.Game;
import xaos.panels.CommandPanel;
import xaos.panels.MainMenuPanel;
import xaos.panels.menus.MenuExtensionDefinition.Contribution;
import xaos.utils.LanguageData;
import xaos.utils.Messages;
import xaos.utils.Utils;
import xaos.utils.UtilsKeyboard;

public final class MenuManager {

    private final Map<String, MenuDefinition> menusById = new HashMap<>();
    private final Set<String> appliedExtensionIds = new HashSet<>();

    public void loadMenus(File rootFolder) {
        if (rootFolder == null || !rootFolder.exists()) {
            throw new IllegalArgumentException("Menu folder does not exist: " + rootFolder);
        }

        if (!rootFolder.isDirectory()) {
            throw new IllegalArgumentException("Menu path is not a folder: " + rootFolder);
        }

        menusById.clear();
        appliedExtensionIds.clear();
        loadMenusRecursive(rootFolder);
    }

    private void loadMenusRecursive(File folder) {
        File[] files = folder.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                loadMenusRecursive(file);
                continue;
            }

            if (!file.getName().toLowerCase().endsWith(".xml")) {
                continue;
            }

            MenuDefinition menu = MenuXmlLoader.load(file);

            if (menusById.containsKey(menu.getId())) {
                throw new IllegalArgumentException(
                        "Duplicate menu id found: " + menu.getId()
                                + " in file: " + file.getPath());
            }

            menusById.put(menu.getId(), menu);
        }
    }

    public MenuDefinition getMenu(String id) {
        return menusById.get(id);
    }

    public boolean hasMenu(String id) {
        return menusById.containsKey(id);
    }

    /**
     * Returns a registered menu or reports a missing ID.
     *
     * Unlike getMenu(), this does not silently return null.
     */
    public MenuDefinition requireMenu(String id) {

        MenuDefinition menu = menusById.get(id);

        if (menu == null) {
            throw new IllegalArgumentException(
                    "Menu not found: " + id);
        }

        return menu;
    }

    /**
     * Resolves the target of a declarative submenu item.
     *
     * Both built-in menus and future mod contributions
     * can use this method.
     */
    public MenuDefinition resolveSubmenu(
            MenuItemDefinition item) {

        Objects.requireNonNull(item, "item");

        if (!"submenu".equals(item.getType())) {
            throw new IllegalArgumentException(
                    "Not a submenu item: " + item.getId());
        }

        String targetId = item.getTargetMenu();

        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException(
                    "Submenu has no targetMenu: "
                            + item.getId());
        }

        MenuDefinition target = menusById.get(targetId);

        if (target == null) {
            throw new IllegalArgumentException(
                    "Missing submenu target '"
                            + targetId
                            + "' referenced by '"
                            + item.getId()
                            + "'");
        }

        return target;
    }

    /**
     * Validates the menus reachable from a root menu.
     *
     * Checks:
     * - Every referenced submenu exists.
     * - Item IDs are unique within their menu.
     * - Submenu references contain no cycles.
     *
     * Only the requested hierarchy is validated,
     * allowing unfinished migrations to coexist.
     */
    public void validateMenuTree(String rootMenuId) {

        Objects.requireNonNull(
                rootMenuId, "rootMenuId");

        requireMenu(rootMenuId);

        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        Deque<String> path = new ArrayDeque<>();

        validateMenuTree(
                rootMenuId,
                visited,
                visiting,
                path);
    }

    /**
     * Depth-first validation of submenu references.
     */
    private void validateMenuTree(
            String menuId,
            Set<String> visited,
            Set<String> visiting,
            Deque<String> path) {

        // A menu reached again while it is still
        // being explored indicates a circular link.
        if (visiting.contains(menuId)) {

            throw new IllegalArgumentException(
                    "Circular submenu reference: "
                            + path
                            + " -> " + menuId);
        }

        // Already validated through another branch.
        if (visited.contains(menuId)) {
            return;
        }

        MenuDefinition menu = requireMenu(menuId);

        visiting.add(menuId);
        path.addLast(menuId);

        try {

            Set<String> itemIds = new HashSet<>();

            // getItems() is the flattened view,
            // so XML items inside tabs are included.
            for (MenuItemDefinition item : menu.getItems()) {

                String itemId = item.getId();

                if (itemId == null || itemId.isBlank()) {
                    throw new IllegalArgumentException(
                            "Blank item ID in menu: "
                                    + menuId);
                }

                if (!itemIds.add(itemId)) {

                    throw new IllegalArgumentException(
                            "Duplicate item ID '"
                                    + itemId
                                    + "' in menu '"
                                    + menuId
                                    + "'");
                }

                if ("submenu".equals(item.getType())) {

                    MenuDefinition target = resolveSubmenu(item);

                    validateMenuTree(
                            target.getId(),
                            visited,
                            visiting,
                            path);
                }
            }

            visited.add(menuId);

        } finally {

            path.removeLast();
            visiting.remove(menuId);
        }
    }

    /**
     * Loads and applies one menu extension XML file.
     *
     * Extension files are loaded explicitly, after
     * their target menus have been registered.
     */
    public void applyExtension(File file) {

        applyExtension(
                MenuExtensionXmlLoader.load(file));
    }

    /**
     * Applies a menu extension without modifying
     * the original definition on failure.
     */
    public void applyExtension(
            MenuExtensionDefinition extension) {

        Objects.requireNonNull(extension, "extension");

        if (appliedExtensionIds.contains(extension.id())) {
            throw new IllegalArgumentException(
                    "Extension already applied: "
                            + extension.id());
        }

        MenuDefinition original = requireMenu(extension.targetMenu());

        // Work on a copy of the structured menu.
        // This preserves existing tab groups and
        // the order of the original XML.
        List<MenuContentDefinition> merged = new ArrayList<>(original.getContent());

        Set<String> usedIds = new HashSet<>();

        for (MenuItemDefinition item : original.getItems()) {
            usedIds.add(item.getId());
        }

        for (Contribution contribution : extension.contributions()) {

            MenuItemDefinition item = contribution.item();

            if (item.getId() == null
                    || item.getId().isBlank()) {

                throw new IllegalArgumentException(
                        "Extension item ID cannot be blank");
            }

            if (!usedIds.add(item.getId())) {
                throw new IllegalArgumentException(
                        "Duplicate menu item ID: "
                                + item.getId()
                                + " in " + original.getId());
            }

            int position;

            if (contribution.before() != null) {

                position = findTopLevelItem(
                        merged,
                        contribution.before());

            } else if (contribution.after() != null) {

                position = findTopLevelItem(
                        merged,
                        contribution.after()) + 1;

            } else {

                position = defaultInsertionPoint(merged);
            }

            merged.add(position, item);
        }

        // Build a fresh definition using its normal
        // insertion methods. Both views remain consistent:
        //
        // getContent() = structured top-level content
        // getItems() = flattened items, including tabs
        MenuDefinition updated = new MenuDefinition(
                original.getId(),
                original.getTitleKey(),
                original.getTitle());

        for (MenuContentDefinition entry : merged) {

            if (entry instanceof MenuItemDefinition item) {

                updated.addItem(item);

            } else if (entry instanceof MenuTabsDefinition tabs) {

                updated.addTabs(tabs);

            } else {

                throw new IllegalStateException(
                        "Unsupported menu content: " + entry);
            }
        }

        // Commit only after the complete merge succeeds.
        menusById.put(updated.getId(), updated);
        appliedExtensionIds.add(extension.id());
    }

    /**
     * Finds an item directly inside the target menu.
     *
     * Items nested inside tabs require a separate
     * tab-targeting extension operation.
     */
    private static int findTopLevelItem(
            List<MenuContentDefinition> content,
            String itemId) {

        for (int i = 0; i < content.size(); i++) {

            if (content.get(i) instanceof MenuItemDefinition item
                    && itemId.equals(item.getId())) {

                return i;
            }
        }

        throw new IllegalArgumentException(
                "Extension placement target not found "
                        + "as a direct menu item: "
                        + itemId);
    }

    /**
     * Append items before trailing Back controls,
     * keeping Back at the end of the menu.
     */
    private static int defaultInsertionPoint(
            List<MenuContentDefinition> content) {

        int index = content.size();

        while (index > 0
                && content.get(index - 1) instanceof MenuItemDefinition item
                && "back".equals(item.getType())) {

            index--;
        }

        return index;
    }

    public Map<String, MenuDefinition> getMenusById() {
        return menusById;
    }

    public void clear() {
        menusById.clear();
        appliedExtensionIds.clear();
    }

    public SmartMenu buildSmartMenu(String menuId, SmartMenu parent) {
        MenuDefinition definition = menusById.get(menuId);

        if (definition == null) {
            throw new IllegalArgumentException("Menu not found: " + menuId);
        }

        return buildSmartMenu(definition, parent, new HashSet<String>());
    }

    public SmartMenu buildSmartMenu(MenuDefinition definition, SmartMenu parent) {
        return buildSmartMenu(definition, parent, new HashSet<String>());
    }

    private SmartMenu buildSmartMenu(MenuDefinition definition, SmartMenu parent, Set<String> buildStack) {
        if (definition == null) {
            throw new IllegalArgumentException("Cannot build null menu definition.");
        }

        if (buildStack.contains(definition.getId())) {
            throw new IllegalArgumentException("Circular menu reference detected: " + definition.getId());
        }

        buildStack.add(definition.getId());

        SmartMenu menu = new SmartMenu(
                SmartMenu.TYPE_MENU,
                Messages.getString(definition.getTitleKey()),
                parent,
                null,
                null,
                null,
                null,
                null);

        for (MenuItemDefinition item : definition.getItems()) {
            buildSmartMenuItems(item, menu, buildStack);
        }

        buildStack.remove(definition.getId());

        menu.setTrasparency(definition.isTransparent());

        return menu;
    }

    private void buildSmartMenuItems(MenuItemDefinition item, SmartMenu parent, Set<String> buildStack) {
        if (!isConditionMet(item.getCondition())) {
            return;
        }

        if ("generated".equals(item.getType())) {
            buildGeneratedItems(item, parent);
            return;
        }

        SmartMenu smartMenuItem = buildSmartMenuItem(item, parent, buildStack);
        applyXmlItemSettings(smartMenuItem, item);
        parent.addItem(smartMenuItem);
    }

    private SmartMenu buildSmartMenuItem(MenuItemDefinition item, SmartMenu parent, Set<String> buildStack) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot build null menu item.");
        }

        String label = item.getTitleKey() == null
                ? ""
                : Messages.getString(item.getTitleKey());

        if ("submenu".equals(item.getType())) {
            return buildSubmenuItem(item, parent, buildStack);
        }

        if ("keyboard".equals(item.getType())) {
            int keyboardKey = resolveKeyboardKey(item.getKey());

            if (keyboardKey == -1) {
                throw new IllegalArgumentException("Keyboard item is missing or has invalid key: " + item.getId());
            }

            return MainMenuPanel.createKeyboardMenu(
                    keyboardKey,
                    MainMenuPanel.textColor,
                    MainMenuPanel.borderColor);
        }

        if ("back".equals(item.getType())) {
            return createBackItem(parent);
        }

        if ("spacer".equals(item.getType())) {
            return createSpacerItem();
        }

        int smartMenuType = toSmartMenuType(item.getType());

        return new SmartMenu(
                smartMenuType,
                label,
                parent,
                resolveCommandKey(item.getCommand()),
                null,
                null,
                null,
                null);
    }

    private void applyXmlItemSettings(SmartMenu menuItem, MenuItemDefinition item) {

        // Preserve the ID from the XML definition.
        //
        // This lets compatibility adapters identify
        // menu actions independently of localisation.
        menuItem.setID(item.getId());

        Color textColor = resolveTextColor(item.getTextColorName());

        if (textColor != null) {
            menuItem.setColor(textColor);
        }

        menuItem.setDynamic(item.isDynamic());
        menuItem.setMaintainOpen(item.isMaintainOpen());
        menuItem.setTrasparency(item.isTransparent());
    }

    private Color resolveTextColor(String textColorName) {
        if (textColorName == null || textColorName.trim().isEmpty()) {
            return null;
        }

        String value = textColorName.trim().toLowerCase();

        if ("default".equals(value)) {
            return MainMenuPanel.textColor;
        }

        if ("credits".equals(value)) {
            return MainMenuPanel.creditsColor;
        }

        if ("red".equals(value)) {
            return Color.RED;
        }

        if ("white".equals(value)) {
            return Color.WHITE;
        }

        if ("black".equals(value)) {
            return Color.BLACK;
        }

        if ("gray".equals(value) || "grey".equals(value)) {
            return Color.GRAY;
        }

        if ("light_gray".equals(value) || "light_grey".equals(value)) {
            return Color.LIGHT_GRAY;
        }

        if ("dark_gray".equals(value) || "dark_grey".equals(value)) {
            return Color.DARK_GRAY;
        }

        if ("green".equals(value)) {
            return Color.GREEN;
        }

        if ("blue".equals(value)) {
            return Color.BLUE;
        }

        if ("yellow".equals(value)) {
            return Color.YELLOW;
        }

        if ("orange".equals(value)) {
            return Color.ORANGE;
        }

        if (value.startsWith("#")) {
            return Color.decode(value);
        }

        throw new IllegalArgumentException("Unknown menu text color: " + textColorName);
    }

    private SmartMenu buildSubmenuItem(MenuItemDefinition item, SmartMenu parent, Set<String> buildStack) {
        String targetMenuId = item.getTargetMenu();

        if (targetMenuId == null || targetMenuId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Submenu item is missing targetMenu. Item id: " + item.getId());
        }

        MenuDefinition targetMenu = menusById.get(targetMenuId);

        if (targetMenu == null) {
            throw new IllegalArgumentException(
                    "Target menu not found: " + targetMenuId
                            + " for submenu item: " + item.getId());
        }

        return buildSmartMenu(targetMenu, parent, buildStack);
    }

    private int toSmartMenuType(String type) {
        if ("text".equals(type)) {
            return SmartMenu.TYPE_TEXT;
        }

        if ("item".equals(type)) {
            return SmartMenu.TYPE_ITEM;
        }
        if ("submenu".equals(type)) {
            return SmartMenu.TYPE_MENU;
        }
        if ("button".equals(type)) {
            return SmartMenu.TYPE_BUTTON;
        }
        if ("keyboard".equals(type)) {
            return SmartMenu.TYPE_KEYBOARD;
        }

        if ("toggle".equals(type)) {
            return SmartMenu.TYPE_TOGGLE;
        }

        if ("slider".equals(type)) {
            return SmartMenu.TYPE_SLIDER;
        }
        if ("cycle".equals(type)) {
            return SmartMenu.TYPE_CYCLE;
        }

        throw new IllegalArgumentException("Unknown menu item type: " + type);
    }

    private static final Class<?> DEFAULT_COMMAND_CLASS = CommandPanel.class;

    private String resolveCommandKey(String commandKey) {
        return resolveCommandKey(DEFAULT_COMMAND_CLASS, commandKey);
    }

    private String resolveCommandKey(Class<?> commandClass, String commandKey) {
        if (commandKey == null || commandKey.trim().isEmpty()) {
            return null;
        }

        try {
            java.lang.reflect.Field field = commandClass.getDeclaredField(commandKey);
            field.setAccessible(true);

            Object value = field.get(null);

            if (!(value instanceof String)) {
                throw new IllegalArgumentException("Command field is not a String: " + commandKey);
            }

            return (String) value;
        } catch (NoSuchFieldException exception) {
            throw new IllegalArgumentException("Unknown command key: " + commandKey, exception);
        } catch (IllegalAccessException exception) {
            throw new IllegalArgumentException("Cannot access command key: " + commandKey, exception);
        }
    }

    private static final Class<?> DEFAULT_KEYBOARD_CLASS = UtilsKeyboard.class;

    private Integer resolveKeyboardKey(String keyboardKey) {
        return resolveKeyboardKey(DEFAULT_KEYBOARD_CLASS, keyboardKey);
    }

    private Integer resolveKeyboardKey(Class<?> keyboardClass, String keyboardKey) {
        if (keyboardKey == null || keyboardKey.trim().isEmpty()) {
            return -1;
        }

        try {
            java.lang.reflect.Field field = keyboardClass.getDeclaredField(keyboardKey);
            field.setAccessible(true);

            Object value = field.get(null);

            if (!(value instanceof Integer)) {
                throw new IllegalArgumentException("Keyboard field is not an Integer: " + keyboardKey);
            }

            return (Integer) value;
        } catch (NoSuchFieldException exception) {
            throw new IllegalArgumentException("Unknown keyboard key: " + keyboardKey, exception);
        } catch (IllegalAccessException exception) {
            throw new IllegalArgumentException("Cannot access keyboard key: " + keyboardKey, exception);
        }
    }

    private boolean isConditionMet(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        condition = condition.trim();

        if (condition.startsWith("!")) {
            return !isConditionMet(condition.substring(1));
        }

        if ("savegames.exists".equals(condition)) {
            return hasSavegames();
        }
        if ("debug_mode".equals(condition)) {
            return TownsProperties.DEBUG_MODE;
        }

        // if ("mods.exists".equals(condition)) {
        // return Game.getModsLoaded() != null && !Game.getModsLoaded().isEmpty();
        // }

        // if ("servers.exists".equals(condition)) {
        // return Game.getServerNames() != null && !Game.getServerNames().isEmpty();
        // }

        // if ("campaigns.exists".equals(condition)) {
        // return CampaignManager.getCampaigns() != null
        // && !CampaignManager.getCampaigns().isEmpty();
        // }

        throw new IllegalArgumentException("Unknown menu condition: " + condition);
    }

    private boolean hasSavegames() {
        ArrayList<File> savegames = Utils.getSaveFiles();
        return savegames != null && !savegames.isEmpty();
    }

    private void buildGeneratedItems(MenuItemDefinition item, SmartMenu parent) {
        String provider = item.getProvider();

        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("Generated menu item is missing provider: " + item.getId());
        }

        if ("main.options.languages.root".equals(provider)) {
            buildLanguageItems(parent);
            return;
        }

        if ("main.tutorials.root".equals(provider)) {
            buildCampaignMissionItems(parent, true);
            return;
        }

        if ("main.new_game.root".equals(provider)) {
            buildCampaignMissionItems(parent, false);
            return;
        }

        if ("main.load_game.saves".equals(provider)) {
            buildSavegameItems(parent);
            return;
        }

        if ("main.mod_manager.root".equals(provider)) {
            buildModItems(parent);
            return;
        }

        throw new IllegalArgumentException("Unknown generated menu provider: " + provider);
    }

    private void buildLanguageItems(SmartMenu parent) {
        ArrayList<LanguageData> languages = Utils.getLanguages();

        if (languages == null || languages.size() <= 1) {
            return;
        }

        for (LanguageData languageData : languages) {
            SmartMenu languageItem = createLanguageItem(languageData, parent);
            parent.addItem(languageItem);
        }
    }

    private SmartMenu createLanguageItem(LanguageData languageData, SmartMenu parent) {
        Point3D modIndexPoint = getLanguageModIndexPoint(languageData);

        return new SmartMenu(
                SmartMenu.TYPE_ITEM,
                languageData.name,
                parent,
                CommandPanel.COMMAND_CHANGE_LANGUAGE,
                languageData.language,
                languageData.country,
                modIndexPoint,
                null);
    }

    private Point3D getLanguageModIndexPoint(LanguageData languageData) {
        if (languageData == null || languageData.mod == null || Game.getModsLoaded() == null) {
            return null;
        }

        int modIndex = getLoadedModIndex(languageData.mod);

        if (modIndex == -1) {
            return null;
        }

        return new Point3D(modIndex, 0, 0);
    }

    private int getLoadedModIndex(Object languageMod) {
        for (int index = 0; index < Game.getModsLoaded().size(); index++) {
            if (Game.getModsLoaded().get(index).equals(languageMod)) {
                return index;
            }
        }

        return -1;
    }

    private void buildCampaignMissionItems(SmartMenu parent, boolean tutorialOnly) {
        ArrayList<CampaignData> campaigns = CampaignManager.getCampaigns();

        if (campaigns == null || campaigns.isEmpty()) {
            return;
        }

        for (CampaignData campaignData : campaigns) {
            if (campaignData.isTutorial() != tutorialOnly) {
                continue;
            }

            addCampaignMissions(parent, campaignData);
        }
    }

    private void addCampaignMissions(SmartMenu parent, CampaignData campaignData) {
        String campaignId = campaignData.getId();

        for (int missionIndex = 0; missionIndex < campaignData.getMissions().size(); missionIndex++) {
            String missionName = campaignData.getMissions().get(missionIndex).getName();
            String missionId = campaignData.getMissions().get(missionIndex).getId();

            if (Game.isAllowBury() && campaignData.getMissions().get(missionIndex).isAllowBury()) {
                SmartMenu buryMenu = createBuryMenu(parent, missionName, campaignId, missionId);
                parent.addItem(buryMenu);
            } else {
                SmartMenu missionItem = createMissionItem(parent, missionName, campaignId, missionId);
                parent.addItem(missionItem);
            }
        }
    }

    private SmartMenu createMissionItem(
            SmartMenu parent,
            String missionName,
            String campaignId,
            String missionId) {
        return new SmartMenu(
                SmartMenu.TYPE_ITEM,
                missionName,
                parent,
                CommandPanel.COMMAND_MM_NEWGAME_SET_SAVE_NAME,
                campaignId,
                missionId,
                null,
                null);
    }

    private SmartMenu createBuryMenu(
            SmartMenu parent,
            String missionName,
            String campaignId,
            String missionId) {
        SmartMenu buryMenu = new SmartMenu(
                SmartMenu.TYPE_MENU,
                missionName,
                parent,
                null,
                null,
                null,
                null,
                null);

        buryMenu.setTrasparency(true);

        buryMenu.addItem(new SmartMenu(
                SmartMenu.TYPE_TEXT,
                missionName,
                null,
                null,
                null,
                null));

        buryMenu.addItem(new SmartMenu(
                SmartMenu.TYPE_TEXT,
                null,
                null,
                null,
                null,
                null));

        addNoBuryItem(buryMenu, campaignId, missionId);
        addSpacer(buryMenu);

        addLocalBuryItem(buryMenu, campaignId, missionId);
        addSpacer(buryMenu);

        addServerBuryItems(buryMenu, campaignId, missionId);

        buryMenu.addItem(createBackItem(buryMenu));

        return buryMenu;
    }

    private void addNoBuryItem(SmartMenu parent, String campaignId, String missionId) {
        SmartMenu item = new SmartMenu(
                SmartMenu.TYPE_ITEM,
                Messages.getString("MainMenuPanel.69"),
                parent,
                CommandPanel.COMMAND_MM_NEWGAME_SET_SAVE_NAME_NO_BURY,
                campaignId,
                missionId,
                null,
                null);

        parent.addItem(item);
    }

    private void addLocalBuryItem(SmartMenu parent, String campaignId, String missionId) {
        SmartMenu item = new SmartMenu(
                SmartMenu.TYPE_ITEM,
                Messages.getString("MainMenuPanel.67"),
                parent,
                CommandPanel.COMMAND_MM_NEWGAME_SET_SAVE_NAME,
                campaignId,
                missionId,
                null,
                null);

        parent.addItem(item);
    }

    private void addServerBuryItems(SmartMenu parent, String campaignId, String missionId) {
        if (Game.getServerNames() == null || Game.getServerNames().isEmpty()) {
            return;
        }

        for (int serverIndex = 0; serverIndex < Game.getServerNames().size(); serverIndex++) {
            String serverName = Game.getServerNames().get(serverIndex);

            SmartMenu item = new SmartMenu(
                    SmartMenu.TYPE_ITEM,
                    Messages.getString("MainMenuPanel.68") + " [" + serverName + "]",
                    parent,
                    CommandPanel.COMMAND_MM_NEWGAME_SET_SAVE_NAME,
                    campaignId,
                    missionId,
                    new Point3D(serverIndex, serverIndex, serverIndex),
                    null);

            parent.addItem(item);
            addSpacer(parent);
        }
    }

    private void addSpacer(SmartMenu parent) {
        parent.addItem(createSpacerItem());
    }

    private SmartMenu createSpacerItem() {
        return new SmartMenu(
                SmartMenu.TYPE_SPACER,
                null,
                null,
                null,
                null,
                null);
    }

    private SmartMenu createBackItem(SmartMenu parent) {
        return new SmartMenu(
                SmartMenu.TYPE_BUTTON,
                Messages.getString("MainMenuPanel.7"),
                parent,
                CommandPanel.COMMAND_BACK,
                null,
                null,
                null,
                null);
    }

    private void buildSavegameItems(SmartMenu parent) {
        ArrayList<File> savegames = Utils.getSaveFiles();

        if (savegames == null || savegames.isEmpty()) {
            return;
        }

        for (File saveFile : savegames) {
            addSavegameItems(parent, saveFile);
            addSpacer(parent);
        }
    }

    private void addSavegameItems(SmartMenu parent, File saveFile) {
        String saveName = Utils.removeExtension(saveFile.getName());
        String dateText = getSavegameDateText(saveFile);
        String displayName = saveName + dateText;

        SmartMenu loadItem = new SmartMenu(
                SmartMenu.TYPE_ITEM,
                Messages.getString("MainMenuPanel.40") + displayName,
                parent,
                CommandPanel.COMMAND_MM_CONTINUEGAME,
                saveFile.getName(),
                null,
                null,
                null);

        parent.addItem(loadItem);

        SmartMenu deleteMenu = createDeleteSavegameMenu(parent, saveFile, displayName);
        parent.addItem(deleteMenu);
    }

    private SmartMenu createDeleteSavegameMenu(SmartMenu parent, File saveFile, String displayName) {
        SmartMenu deleteMenu = new SmartMenu(
                SmartMenu.TYPE_MENU,
                Messages.getString("MainMenuPanel.49") + displayName,
                parent,
                null,
                null,
                null,
                null,
                null);

        deleteMenu.addItem(new SmartMenu(
                SmartMenu.TYPE_TEXT,
                Messages.getString("MainMenuPanel.49") + displayName,
                deleteMenu,
                null,
                null,
                null));

        addSpacer(deleteMenu);

        deleteMenu.addItem(new SmartMenu(
                SmartMenu.TYPE_TEXT,
                Messages.getString("MainMenuPanel.54"),
                deleteMenu,
                null,
                null,
                null));

        SmartMenu confirmDelete = new SmartMenu(
                SmartMenu.TYPE_ITEM,
                Messages.getString("MainMenuPanel.55") + displayName,
                deleteMenu,
                CommandPanel.COMMAND_MM_DELETEGAME,
                saveFile.getName(),
                null,
                null,
                null);

        deleteMenu.addItem(confirmDelete);

        addSpacer(deleteMenu);
        deleteMenu.addItem(createBackItem(deleteMenu));

        return deleteMenu;
    }

    private String getSavegameDateText(File saveFile) {
        Calendar saveDate = Calendar.getInstance();
        saveDate.setTimeInMillis(saveFile.lastModified());

        Calendar today = Calendar.getInstance();

        String dateText;

        if (isSameDay(saveDate, today)) {
            dateText = " (" + Messages.getString("MainMenuPanel.42");
        } else if (isYesterday(saveDate, today)) {
            dateText = " (" + Messages.getString("MainMenuPanel.51");
        } else {
            dateText = " ("
                    + saveDate.get(Calendar.DAY_OF_MONTH)
                    + "/"
                    + twoDigit(saveDate.get(Calendar.MONTH) + 1)
                    + "/"
                    + saveDate.get(Calendar.YEAR);
        }

        dateText += " "
                + twoDigit(saveDate.get(Calendar.HOUR_OF_DAY))
                + ":"
                + twoDigit(saveDate.get(Calendar.MINUTE))
                + ")";

        return dateText;
    }

    private boolean isSameDay(Calendar first, Calendar second) {
        return first.get(Calendar.DAY_OF_MONTH) == second.get(Calendar.DAY_OF_MONTH)
                && first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
                && first.get(Calendar.YEAR) == second.get(Calendar.YEAR);
    }

    private boolean isYesterday(Calendar saveDate, Calendar today) {
        Calendar yesterday = Calendar.getInstance();
        yesterday.setTimeInMillis(today.getTimeInMillis());
        yesterday.add(Calendar.DAY_OF_MONTH, -1);

        return isSameDay(saveDate, yesterday);
    }

    private String twoDigit(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private void buildModItems(SmartMenu parent) {
        addModsFolderItem(parent);
        addSpacer(parent);

        ArrayList<File> mods = Utils.getModsFolders();

        if (mods == null || mods.isEmpty()) {
            addNoModsItem(parent);
            return;
        }

        for (File modFolder : mods) {
            SmartMenu modItem = createModToggleItem(modFolder, parent);
            parent.addItem(modItem);
        }
    }

    private void addModsFolderItem(SmartMenu parent) {
        String modsFolder = Game.getUserFolder()
                + Game.getFileSeparator()
                + Game.MODS_FOLDER1
                + Game.getFileSeparator();

        SmartMenu modsFolderItem = new SmartMenu(
                SmartMenu.TYPE_ITEM,
                Messages.getString("MainMenuPanel.70") + " [" + modsFolder + "]",
                parent,
                CommandPanel.COMMAND_OPEN_FOLDER,
                modsFolder,
                null,
                null,
                null);

        parent.addItem(modsFolderItem);
    }

    private void addNoModsItem(SmartMenu parent) {
        parent.addItem(new SmartMenu(
                SmartMenu.TYPE_TEXT,
                Messages.getString("MainMenuPanel.60"),
                parent,
                null,
                null,
                null));
    }

    private SmartMenu createModToggleItem(File modFolder, SmartMenu parent) {
        String modName = modFolder.getName();

        SmartMenu modItem = new SmartMenu(
                SmartMenu.TYPE_ITEM,
                modName + " __MOD__" + modName + "__/MOD__",
                parent,
                CommandPanel.COMMAND_TOGGLE_MOD,
                modName,
                null,
                null,
                null);

        modItem.setDynamic(true);

        return modItem;
    }
}