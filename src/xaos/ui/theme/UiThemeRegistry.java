
package xaos.ui.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stores available UI themes and manages the active theme.
 *
 * Theme registration is independent of rendering,
 * layout and the game's modding API.
 */
public final class UiThemeRegistry {

    private final Map<String, UiTheme> themes =
            new LinkedHashMap<>();

    private String activeId;

    /**
     * Registers a theme under a unique identifier.
     *
     * The first registered theme becomes active by default.
     */
    public void register(String id, UiTheme theme) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(theme, "theme");

        if (id.isBlank()) {
            throw new IllegalArgumentException(
                    "Theme ID cannot be blank");
        }

        if (themes.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Theme already registered: " + id);
        }

        themes.put(id, theme);

        if (activeId == null) {
            activeId = id;
        }
    }

    /**
     * Selects an existing theme by ID.
     */
    public void select(String id) {
        Objects.requireNonNull(id, "id");

        if (!themes.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Unknown UI theme: " + id);
        }

        activeId = id;
    }

    /**
     * Returns the currently selected theme.
     */
    public UiTheme activeTheme() {
        if (activeId == null) {
            throw new IllegalStateException(
                    "No UI themes have been registered");
        }

        return themes.get(activeId);
    }

    /**
     * Returns the identifier of the active theme.
     */
    public String activeId() {
        if (activeId == null) {
            throw new IllegalStateException(
                    "No UI themes have been registered");
        }

        return activeId;
    }

    /**
     * Returns a read-only view of registered themes.
     */
    public Map<String, UiTheme> themes() {
        return Collections.unmodifiableMap(themes);
    }
}
