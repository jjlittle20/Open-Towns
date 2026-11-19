
package xaos.ui.integration;

import java.util.Objects;
import java.util.function.Function;

import xaos.panels.menus.MenuDefinition;
import xaos.panels.menus.MenuItemDefinition;
import xaos.ui.components.UiButton;
import xaos.ui.components.UiLabel;
import xaos.ui.components.UiScrollContainer;
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

    private UiSimpleMenuWindowBuilder() {}

    public static UiWindow build(
            MenuDefinition definition,
            Function<String, String> localize,
            UiTextMetrics textMetrics,
            Runnable onBack) {

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

        UiScrollContainer scroll =
                new UiScrollContainer();

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

        // Keep Back outside the scrollable area.
        // It remains visible when scrolling the credits.
        if (backButton != null) {
            window.content().add(backButton);
        }

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
