
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiCycleButton;
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiScrollContainer;
import xaos.ui.components.UiToggleButton;
import xaos.ui.components.UiWindow;
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

                UiScrollContainer scroll = new UiScrollContainer();

                scroll.content().setSpacing(4);

                UiButton backButton = null;

                for (MenuItemDefinition item : definition.getItems()) {

                        switch (item.getType()) {

                                case "text" -> {

                                        String label = resolve(
                                                        localize, item.getTitleKey());

                                        scroll.content().add(
                                                        new UiLabel(label, textMetrics));
                                }

                                case "spacer" -> {

                                        UiElement spacer = new UiElement();

                                        spacer.setPreferredSize(
                                                        new UiSize(0, SPACER_HEIGHT));

                                        scroll.content().add(spacer);
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

                                        scroll.content().add(submenuButton);
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

                                        String label = resolve(
                                                        localize,
                                                        item.getTitleKey());

                                        UiToggleButton toggle = new UiToggleButton(
                                                        label,
                                                        textMetrics,
                                                        binding.state(),
                                                        binding.action());

                                        scroll.content().add(toggle);
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

                                        String label = resolve(
                                                        localize,
                                                        item.getTitleKey());

                                        UiCycleButton cycle = new UiCycleButton(
                                                        label,
                                                        textMetrics,
                                                        binding.value(),
                                                        binding.action());

                                        scroll.content().add(cycle);
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

                window.content().add(scroll);

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
