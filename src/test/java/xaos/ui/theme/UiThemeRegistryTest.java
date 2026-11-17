
package xaos.ui.theme;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UiThemeRegistryTest {

    @Test
    void firstRegisteredThemeBecomesActive() {
        UiThemeRegistry registry = new UiThemeRegistry();
        UiTheme theme = new BasicUiTheme();

        registry.register("basic", theme);

        assertEquals("basic", registry.activeId());
        assertSame(theme, registry.activeTheme());
    }

    @Test
    void selectingThemeChangesActiveTheme() {
        UiThemeRegistry registry = new UiThemeRegistry();

        UiTheme basic = new BasicUiTheme();
        UiTheme classic = new ClassicTownsTheme();

        registry.register("basic", basic);
        registry.register("classic-towns", classic);

        registry.select("classic-towns");

        assertSame(classic, registry.activeTheme());

        registry.select("basic");

        assertSame(basic, registry.activeTheme());
    }

    @Test
    void duplicateThemeIdsAreRejected() {
        UiThemeRegistry registry = new UiThemeRegistry();

        registry.register("basic", new BasicUiTheme());

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(
                        "basic", new BasicUiTheme()));
    }

    @Test
    void unknownThemesAreRejected() {
        UiThemeRegistry registry = new UiThemeRegistry();

        registry.register("basic", new BasicUiTheme());

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.select("missing"));

        assertEquals("basic", registry.activeId());
    }

    @Test
    void registeredThemesCannotBeModifiedExternally() {
        UiThemeRegistry registry = new UiThemeRegistry();

        registry.register("basic", new BasicUiTheme());

        assertThrows(
                UnsupportedOperationException.class,
                () -> registry.themes().clear());
    }

    @Test
    void emptyRegistryHasNoActiveTheme() {
        UiThemeRegistry registry = new UiThemeRegistry();

        assertThrows(
                IllegalStateException.class,
                registry::activeTheme);

        assertThrows(
                IllegalStateException.class,
                registry::activeId);
    }
}
