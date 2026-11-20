
package xaos.ui.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import xaos.panels.menus.MenuContentDefinition;
import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.panels.menus.MenuTabDefinition;
import xaos.panels.menus.MenuTabsDefinition;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiCycleButton;
import xaos.ui.components.UiKeyBindingRow;
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiSlider;
import xaos.ui.components.UiTabbedPane;
import xaos.ui.components.UiToggleButton;
import xaos.ui.components.UiWindow;
import xaos.ui.core.UiContainer;
import xaos.ui.core.UiElement;
import xaos.ui.layout.UiInsets;
import xaos.ui.layout.UiPoint;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

/**
 * Converts simple legacy XML menu definitions
 * into windows in the new UI framework.
 *
 * Initially supports:
 * - text
 * - spacer
 * - back
 *
 * More complex menu types will be added as
 * original Towns menus are migrated.
 *
 * This is an internal compatibility adapter,
 * not part of the public modding API.
 */
public final class UiSimpleMenuWindowBuilder {

        private static final int SPACER_HEIGHT = 12;

        private UiSimpleMenuWindowBuilder() {
        }

        public static UiWindow build(
                        MenuDefinition definition,
                        Function<String, String> localize,
                        UiTextMetrics textMetrics,
                        Runnable onBack) {

                // Existing callers, including Credits, retain
                // their current behaviour.
                //
                // Submenus remain unsupported unless a
                // navigation callback is explicitly supplied.
                return build(
                                definition,
                                localize,
                                textMetrics,
                                onBack,
                                null);
        }

        /**
         * Builds an XML-defined menu window.
         *
         * onBack handles the menu's Back button.
         *
         * onSubmenu receives the targetMenu ID of a
         * selected submenu. The caller decides how
         * navigation should be performed.
         */

        public static UiWindow build(
                        MenuDefinition definition,
                        Function<String, String> localize,
                        UiTextMetrics textMetrics,
                        Runnable onBack,
                        Consumer<String> onSubmenu) {

                return build(
                                definition,
                                localize,
                                textMetrics,
                                onBack,
                                onSubmenu,
                                null);
        }

        /**
         * Builds an XML menu with optional submenu
         * navigation and boolean setting bindings.
         */

        public static UiWindow build(
                        MenuDefinition definition,
                        Function<String, String> localize,
                        UiTextMetrics textMetrics,
                        Runnable onBack,
                        Consumer<String> onSubmenu,
                        Function<MenuItemDefinition, UiToggleBinding> toggleBindings) {

                return build(
                                definition,
                                localize,
                                textMetrics,
                                onBack,
                                onSubmenu,
                                toggleBindings,
                                null);
        }

        /**
         * Builds an XML menu with optional submenu,
         * toggle and cycle bindings.
         */

        public static UiWindow build(
                        MenuDefinition definition,
                        Function<String, String> localize,
                        UiTextMetrics textMetrics,
                        Runnable onBack,
                        Consumer<String> onSubmenu,
                        Function<MenuItemDefinition, UiToggleBinding> toggleBindings,
                        Function<MenuItemDefinition, UiCycleBinding> cycleBindings) {

                return build(
                                definition,
                                localize,
                                textMetrics,
                                onBack,
                                onSubmenu,
                                toggleBindings,
                                cycleBindings,
                                null);
        }

        /**
         * Builds XML menus with optional submenu,
         * toggle, cycle and slider bindings.
         */
        public static UiWindow build(
                        MenuDefinition definition,
                        Function<String, String> localize,
                        UiTextMetrics textMetrics,
                        Runnable onBack,
                        Consumer<String> onSubmenu,
                        Function<MenuItemDefinition, UiToggleBinding> toggleBindings,
                        Function<MenuItemDefinition, UiCycleBinding> cycleBindings,
                        Function<MenuItemDefinition, UiSliderBinding> sliderBindings) {

                return build(
                                definition,
                                localize,
                                textMetrics,
                                onBack,
                                onSubmenu,
                                toggleBindings,
                                cycleBindings,
                                sliderBindings,
                                null);
        }

        /**
         * Builds an XML menu, optionally including editable
         * keyboard-binding rows.
         *
         * Keyboard row creation is supplied by the caller
         * because different games and mods may own different
         * shortcut systems.
         */
        public static UiWindow build(
                        MenuDefinition definition,
                        Function<String, String> localize,
                        UiTextMetrics textMetrics,
                        Runnable onBack,
                        Consumer<String> onSubmenu,
                        Function<MenuItemDefinition, UiToggleBinding> toggleBindings,
                        Function<MenuItemDefinition, UiCycleBinding> cycleBindings,
                        Function<MenuItemDefinition, UiSliderBinding> sliderBindings,
                        Function<MenuItemDefinition, UiKeyBindingRow> keyboardRows) {
                Objects.requireNonNull(definition, "definition");
                Objects.requireNonNull(localize, "localize");
                Objects.requireNonNull(textMetrics, "textMetrics");
                Objects.requireNonNull(onBack, "onBack");

                // --------------------------------------------------------
                // Window
                // --------------------------------------------------------

                UiWindow window = new UiWindow(
                                resolve(localize, definition.getTitleKey()));

                window.setPosition(new UiPoint(90, 70));
                window.setPreferredSize(new UiSize(420, 260));

                window.content().setPadding(UiInsets.all(8));
                window.content().setSpacing(8);

                // --------------------------------------------------------
                // Scrollable menu contents
                // --------------------------------------------------------

                List<PlacedItem> placedItems = new ArrayList<>();

                UiScrollContainer currentScroll = null;

                // Read the structured XML rather than the flattened
                // legacy item list.
                for (MenuContentDefinition entry : definition.getContent()) {

                        if (entry instanceof MenuItemDefinition item) {

                                // Back remains a shared control outside tabs.
                                if ("back".equals(item.getType())) {
                                        placedItems.add(new PlacedItem(item, null));
                                        continue;
                                }

                                // Consecutive ordinary items share a scroll area.
                                if (currentScroll == null) {
                                        currentScroll = createScroll();
                                        window.content().add(currentScroll);
                                }

                                placedItems.add(new PlacedItem(
                                                item,
                                                currentScroll.content()));

                        } else if (entry instanceof MenuTabsDefinition group) {

                                // A tab group ends the preceding ordinary
                                // content section.
                                currentScroll = null;

                                UiTabbedPane tabs = new UiTabbedPane(
                                                textMetrics,
                                                UiTabbedPane.Placement.valueOf(
                                                                group.getPlacement().name()));

                                for (MenuTabDefinition tab : group.getTabs()) {

                                        String label = tab.getTitleKey() == null
                                                        ? tab.getTitle()
                                                        : resolve(localize, tab.getTitleKey());

                                        UiContainer page = tabs.addTab(
                                                        tab.getId(),
                                                        label);

                                        // Each tab receives its own scrollable area.
                                        UiScrollContainer tabScroll = createScroll();

                                        page.add(tabScroll);

                                        for (MenuItemDefinition item : tab.getItems()) {

                                                if ("back".equals(item.getType())) {
                                                        throw new IllegalArgumentException(
                                                                        "Back must be outside <tabs>: "
                                                                                        + item.getId());
                                                }

                                                placedItems.add(new PlacedItem(
                                                                item,
                                                                tabScroll.content()));
                                        }
                                }

                                window.content().add(tabs);

                        } else {
                                throw new IllegalArgumentException(
                                                "Unknown menu content: " + entry);
                        }
                }

                // Preserve the legacy scroll structure for empty
                // or Back-only menus.
                if (window.content().children().isEmpty()) {
                        window.content().add(createScroll());
                }

                UiButton backButton = null;

                // The existing switch below creates the controls.
                // We only change which container receives each one.
                for (PlacedItem placed : placedItems) {

                        MenuItemDefinition item = placed.item();
                        UiContainer destination = placed.destination();

                        switch (item.getType()) {

                                case "text" -> {

                                        String label = resolve(
                                                        localize, item.getTitleKey());

                                        destination.add(
                                                        new UiLabel(label, textMetrics));
                                }

                                case "spacer" -> {

                                        UiElement spacer = new UiElement();

                                        spacer.setPreferredSize(
                                                        new UiSize(0, SPACER_HEIGHT));

                                        destination.add(spacer);
                                }

                                case "submenu" -> {

                                        if (onSubmenu == null) {

                                                // The caller hasn't supplied navigation.
                                                // Never create a nonfunctional menu button.
                                                throw new UnsupportedOperationException(
                                                                "Unsupported submenu navigation "
                                                                                + "(no callback configured): "
                                                                                + item.getId());
                                        }

                                        String targetMenuId = item.getTargetMenu();

                                        if (targetMenuId == null
                                                        || targetMenuId.isBlank()) {

                                                throw new IllegalArgumentException(
                                                                "Submenu item has no targetMenu: "
                                                                                + item.getId());
                                        }

                                        // Display the existing translated XML title.
                                        String label = resolve(
                                                        localize,
                                                        item.getTitleKey());

                                        UiButton submenuButton = new UiButton(label, textMetrics);

                                        // Do not perform navigation in the builder.
                                        // Send the target ID to the menu controller.
                                        submenuButton.setOnClick(
                                                        () -> onSubmenu.accept(targetMenuId));

                                        destination.add(submenuButton);
                                }

                                case "toggle" -> {

                                        if (toggleBindings == null) {
                                                throw new UnsupportedOperationException(
                                                                "Toggle binding is not configured: "
                                                                                + item.getId());
                                        }

                                        UiToggleBinding binding = Objects.requireNonNull(
                                                        toggleBindings.apply(item),
                                                        "No toggle binding for XML item: "
                                                                        + item.getId());

                                        String label = settingLabel(
                                                        resolve(localize, item.getTitleKey()));

                                        UiToggleButton toggle = new UiToggleButton(
                                                        label,
                                                        textMetrics,
                                                        binding.state(),
                                                        binding.action());

                                        destination.add(toggle);
                                }

                                case "cycle" -> {

                                        if (cycleBindings == null) {
                                                throw new UnsupportedOperationException(
                                                                "Cycle binding is not configured: "
                                                                                + item.getId());
                                        }

                                        UiCycleBinding binding = Objects.requireNonNull(
                                                        cycleBindings.apply(item),
                                                        "No cycle binding for XML item: "
                                                                        + item.getId());

                                        String label = settingLabel(
                                                        resolve(localize, item.getTitleKey()));

                                        UiCycleButton cycle = new UiCycleButton(
                                                        label,
                                                        textMetrics,
                                                        binding.value(),
                                                        binding.action());

                                        destination.add(cycle);
                                }

                                case "slider" -> {

                                        if (sliderBindings == null) {
                                                throw new UnsupportedOperationException(
                                                                "Slider binding is not configured: "
                                                                                + item.getId());
                                        }

                                        UiSliderBinding binding = Objects.requireNonNull(
                                                        sliderBindings.apply(item),
                                                        "No slider binding for XML item: "
                                                                        + item.getId());

                                        // Strip legacy dynamic placeholders such as
                                        // __VOLMUSIC__ and __VOLFX__.
                                        String label = settingLabel(
                                                        resolve(localize, item.getTitleKey()));

                                        UiSlider slider = new UiSlider(
                                                        label,
                                                        textMetrics,
                                                        binding.minimum(),
                                                        binding.maximum(),
                                                        binding.step(),
                                                        binding::currentValue,
                                                        requested -> binding.setValue(requested),
                                                        binding::commit,
                                                        binding.formatter());

                                        destination.add(slider);
                                }

                                case "keyboard" -> {

                                        if (keyboardRows == null) {
                                                throw new UnsupportedOperationException(
                                                                "Keyboard binding is not configured: "
                                                                                + item.getId());
                                        }

                                        UiKeyBindingRow row = Objects.requireNonNull(
                                                        keyboardRows.apply(item),
                                                        "No keyboard row for XML item: "
                                                                        + item.getId());

                                        destination.add(row);
                                }

                                case "back" -> {

                                        if (backButton != null) {
                                                throw new IllegalArgumentException(
                                                                "Multiple Back items in menu: "
                                                                                + definition.getId());
                                        }

                                        // Matches MenuManager's existing
                                        // localisation key for Back.
                                        backButton = new UiButton(
                                                        resolve(
                                                                        localize,
                                                                        "MainMenuPanel.7"),
                                                        textMetrics);

                                        backButton.setOnClick(onBack);
                                }

                                default -> throw new UnsupportedOperationException(
                                                "Unsupported menu item type '"
                                                                + item.getType()
                                                                + "' in menu "
                                                                + definition.getId()
                                                                + " (item: "
                                                                + item.getId()
                                                                + ")");
                        }
                }

                if (backButton != null) {
                        window.content().add(backButton);
                }

                // Every XML-generated menu receives the same
                // standard title-bar close control.
                //
                // Closing the window and pressing Back use the
                // same navigation callback.
                window.setCloseAction(
                                textMetrics,
                                onBack);

                return window;
        }

        private record PlacedItem(
                        MenuItemDefinition item,
                        UiContainer destination) {
        }

        /**
         * Creates a standard scroll container for XML
         * menu contents or an individual tab page.
         */
        private static UiScrollContainer createScroll() {

                UiScrollContainer scroll = new UiScrollContainer();

                scroll.content().setSpacing(4);

                return scroll;
        }

        private static final Pattern LEGACY_DYNAMIC_SUFFIX = Pattern.compile("\\s*__[A-Z0-9_]+__\\s*$");

        /**
         * Removes a legacy dynamic-state placeholder from
         * the end of a translated setting label.
         *
         * The new toggle/cycle components display their
         * current value separately.
         *
         * Examples:
         *
         * "Mouse scroll __MOUSE_SCROLL__"
         * -> "Mouse scroll"
         *
         * "Autosave: __SAVE_DAYS__"
         * -> "Autosave"
         *
         * "Sieges __SIEGES__"
         * -> "Sieges"
         */
        private static String settingLabel(String translated) {

                Objects.requireNonNull(translated, "translated");

                var matcher = LEGACY_DYNAMIC_SUFFIX.matcher(translated);

                // Ordinary labels are left unchanged.
                if (!matcher.find()) {
                        return translated;
                }

                String label = translated
                                .substring(0, matcher.start())
                                .stripTrailing();

                // UiToggleButton and UiCycleButton add their
                // own ": value" suffix.
                if (label.endsWith(":")) {
                        label = label.substring(
                                        0, label.length() - 1)
                                        .stripTrailing();
                }

                return label;
        }

        private static String resolve(
                        Function<String, String> localize,
                        String key) {

                if (key == null || key.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Menu item has no localisation key");
                }

                return Objects.requireNonNull(
                                localize.apply(key),
                                "No translation for key: " + key);
        }
}
