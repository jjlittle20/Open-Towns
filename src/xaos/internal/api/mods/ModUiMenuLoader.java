
package xaos.internal.api.mods;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import xaos.internal.api.mods.ModDependencyResolver.Result;
import xaos.panels.menus.MenuManager;

/**
 * Builds the internal gameplay-menu registry from
 * built-in XML and contributions from enabled mods.
 *
 * Does not create windows or execute commands.
 */
public final class ModUiMenuLoader {

    private record ModFolder(
            String id,
            Path folder,
            List<String> dependencies) {
    }

    private ModUiMenuLoader() {
    }

    public static MenuManager load(
            File builtInMenus,
            File modsRoot,
            List<String> enabledFolders) {

        MenuManager manager = new MenuManager();

        // --------------------------------------------------------
        // 1. Built-in menu definitions
        // --------------------------------------------------------

        try {
            manager.loadMenus(builtInMenus);
        } catch (Exception exception) {

            ModUiErrors.report(
                    "open-towns",
                    "Built-in menu loading",
                    "Could not load " + builtInMenus,
                    exception);
        }

        // --------------------------------------------------------
        // 2. Discover enabled mods
        // --------------------------------------------------------

        Map<String, ModFolder> discovered = new HashMap<>();

        Set<String> duplicateIds = new HashSet<>();

        Path modsPath = modsRoot.toPath()
                .toAbsolutePath()
                .normalize();

        for (String folderName : enabledFolders) {

            try {

                Path folder = modsPath
                        .resolve(folderName)
                        .normalize();

                // Never allow a mod folder name to escape
                // the configured mods directory.
                if (folder.equals(modsPath)
                        || !folder.startsWith(modsPath)) {

                    throw new IllegalArgumentException(
                            "Invalid mod folder: " + folderName);
                }

                if (!Files.isDirectory(folder)) {
                    throw new IllegalArgumentException(
                            "Enabled mod folder not found: "
                                    + folder);
                }

                Properties properties = readMetadata(folder);

                String id = properties.getProperty(
                        "id", folderName).trim();

                if (id.isBlank()) {
                    throw new IllegalArgumentException(
                            "Missing mod ID");
                }

                List<String> dependencies = parseDependencies(
                        properties.getProperty(
                                "dependencies", ""));

                if (duplicateIds.contains(id)) {
                    continue;
                }

                ModFolder mod = new ModFolder(
                        id, folder, dependencies);

                if (discovered.containsKey(id)) {

                    discovered.remove(id);
                    duplicateIds.add(id);

                    ModUiErrors.report(
                            id,
                            "Menu discovery",
                            "Duplicate mod ID: " + id);

                    continue;
                }

                discovered.put(id, mod);

            } catch (Exception exception) {

                ModUiErrors.report(
                        folderName,
                        "Menu discovery",
                        "Could not discover mod UI files",
                        exception);
            }
        }

        // --------------------------------------------------------
        // 3. Resolve dependency order
        // --------------------------------------------------------

        Map<String, List<String>> dependencies = new HashMap<>();

        discovered.forEach((id, mod) -> dependencies.put(
                id, mod.dependencies()));

        ModDependencyResolver.Result resolution = ModDependencyResolver.resolve(dependencies);

        resolution.failures().forEach(
                (id, message) -> ModUiErrors.report(
                        id,
                        "Menu dependencies",
                        message));

        List<ModFolder> ordered = new ArrayList<>();

        for (String id : resolution.loadOrder()) {
            ordered.add(discovered.get(id));
        }

        // --------------------------------------------------------
        // 4. Register ALL mod menu definitions
        // --------------------------------------------------------

        for (ModFolder mod : ordered) {

            File directory = mod.folder()
                    .resolve("menus/definitions")
                    .toFile();

            for (File file : xmlFiles(
                    directory, mod.id(), "Menu definitions")) {

                try {
                    manager.registerMenu(file);

                } catch (Exception exception) {

                    ModUiErrors.report(
                            mod.id(),
                            "Menu definition",
                            "Failed: " + file.getPath(),
                            exception);
                }
            }
        }

        // --------------------------------------------------------
        // 5. Apply extensions in dependency order
        // --------------------------------------------------------

        for (ModFolder mod : ordered) {

            File directory = mod.folder()
                    .resolve("menus/extensions")
                    .toFile();

            for (File file : xmlFiles(
                    directory, mod.id(), "Menu extensions")) {

                try {
                    manager.applyExtension(file);

                } catch (Exception exception) {

                    ModUiErrors.report(
                            mod.id(),
                            "Menu extension",
                            "Failed: " + file.getPath(),
                            exception);
                }
            }
        }

        // --------------------------------------------------------
        // 6. Validate the complete gameplay hierarchy
        // --------------------------------------------------------

        if (manager.hasMenu("game.right.root")) {

            try {
                manager.validateMenuTree(
                        "game.right.root");

            } catch (Exception exception) {

                ModUiErrors.report(
                        "open-towns",
                        "Right panel validation",
                        "Invalid gameplay menu hierarchy",
                        exception);
            }
        }

        /*
         * Validate the converted bottom toolbar hierarchy.
         *
         * Stockpiles and Zones must both resolve through
         * their stable menu IDs.
         */
        if (manager.hasMenu("game.bottom.root")) {

            try {

                manager.validateMenuTree(
                        "game.bottom.root");

            } catch (Exception exception) {

                ModUiErrors.report(
                        "open-towns",
                        "Bottom panel validation",
                        "Invalid bottom gameplay menu hierarchy",
                        exception);
            }
        }

        return manager;
    }

    /**
     * Java mods use their existing mod.properties.
     *
     * XML-only mods may optionally provide
     * menus/menu.properties without being treated
     * as Java plugins by JavaModLoader.
     */
    private static Properties readMetadata(
            Path modFolder) throws IOException {

        File javaMetadata = modFolder.resolve("mod.properties").toFile();

        File menuMetadata = modFolder.resolve(
                "menus/menu.properties").toFile();

        File selected = javaMetadata.isFile()
                ? javaMetadata
                : menuMetadata;

        Properties properties = new Properties();

        if (selected.isFile()) {

            try (FileInputStream input = new FileInputStream(selected)) {

                properties.load(input);
            }
        }

        return properties;
    }

    private static List<String> parseDependencies(
            String value) {

        if (value == null || value.isBlank()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();

        for (String part : value.split(",")) {

            String dependency = part.trim();

            if (!dependency.isEmpty()
                    && !result.contains(dependency)) {

                result.add(dependency);
            }
        }

        return List.copyOf(result);
    }

    /**
     * Lists XML files in deterministic path order.
     *
     * A damaged directory is reported without
     * stopping other mods from loading.
     */
    private static List<File> xmlFiles(
            File directory,
            String modId,
            String stage) {

        if (!directory.exists()) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(directory.toPath())) {

            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString()
                            .toLowerCase()
                            .endsWith(".xml"))
                    .sorted(Comparator.comparing(
                            Path::toString))
                    .map(Path::toFile)
                    .toList();

        } catch (IOException exception) {

            ModUiErrors.report(
                    modId,
                    stage,
                    "Could not list " + directory,
                    exception);

            return List.of();
        }
    }
}
