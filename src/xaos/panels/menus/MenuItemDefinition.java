
package xaos.panels.menus;

/**
 * One declarative menu entry.
 *
 * Contains menu metadata only.
 * It does not execute gameplay commands.
 */
public final class MenuItemDefinition
        implements MenuContentDefinition {

    private final String id;
    private final String type;
    private final String titleKey;
    private final String targetMenu;
    private final String command;
    private final boolean dynamic;
    private final boolean maintainOpen;
    private final String provider;
    private final String key;
    private final String condition;
    private final String textColorName;
    private final boolean transparent;

    // Gameplay menu metadata.
    private final String title;
    private final String icon;
    private final String parameter;
    private final String parameter2;

    /**
     * Original constructor.
     *
     * Preserved for compatibility with existing
     * menus, tests and legacy adapters.
     */
    public MenuItemDefinition(
            String id,
            String type,
            String titleKey,
            String targetMenu,
            String command,
            boolean dynamic,
            boolean maintainOpen,
            String provider,
            String key,
            String condition,
            String textColorName,
            boolean transparent) {

        this(
                id,
                type,
                titleKey,
                targetMenu,
                command,
                dynamic,
                maintainOpen,
                provider,
                key,
                condition,
                textColorName,
                transparent,
                null,
                null,
                null,
                null);
    }

    /**
     * Extended constructor supporting gameplay
     * menu titles, icons and command parameters.
     */
    public MenuItemDefinition(
            String id,
            String type,
            String titleKey,
            String targetMenu,
            String command,
            boolean dynamic,
            boolean maintainOpen,
            String provider,
            String key,
            String condition,
            String textColorName,
            boolean transparent,
            String title,
            String icon,
            String parameter,
            String parameter2) {

        this.id = id;
        this.type = type;
        this.titleKey = titleKey;
        this.targetMenu = targetMenu;
        this.command = command;
        this.dynamic = dynamic;
        this.maintainOpen = maintainOpen;
        this.provider = provider;
        this.key = key;
        this.condition = condition;
        this.textColorName = textColorName;
        this.transparent = transparent;

        this.title = title;
        this.icon = icon;
        this.parameter = parameter;
        this.parameter2 = parameter2;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getTargetMenu() {
        return targetMenu;
    }

    public String getCommand() {
        return command;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public boolean isMaintainOpen() {
        return maintainOpen;
    }

    public String getProvider() {
        return provider;
    }

    public String getKey() {
        return key;
    }

    public String getCondition() {
        return condition;
    }

    public String getTextColorName() {
        return textColorName;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public String getParameter() {
        return parameter;
    }

    public String getParameter2() {
        return parameter2;
    }
}
