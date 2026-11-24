
package xaos.ui.integration;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Detects whether Towns has additional legacy
 * right-hand menu XML layers.
 *
 * The new XML menu system does not yet translate
 * legacy campaign/mission/mod overrides.
 *
 * Until then, the original right-hand panel
 * remains authoritative when overrides exist.
 */
public final class UiLegacyRightMenuCompatibility {

    private UiLegacyRightMenuCompatibility() {
    }

    /**
     * @param baseMenu      original data/menu_right.xml
     * @param resolvedFiles files returned by
     *                      Utils.getPathToFile()
     *
     * @return true if any additional legacy
     *         menu layer is present
     */
    public static boolean hasOverrides(
            File baseMenu,
            List<String> resolvedFiles) {

        Objects.requireNonNull(baseMenu, "baseMenu");
        Objects.requireNonNull(
                resolvedFiles, "resolvedFiles");

        Path base = baseMenu.toPath()
                .toAbsolutePath()
                .normalize();

        for (String filename : resolvedFiles) {

            if (filename == null || filename.isBlank()) {
                continue;
            }

            Path resolved = Path.of(filename)
                    .toAbsolutePath()
                    .normalize();

            if (!resolved.equals(base)) {
                return true;
            }
        }

        return false;
    }
}
