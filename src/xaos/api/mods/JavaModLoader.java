package xaos.api.mods;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import xaos.api.GameApi;
import xaos.api.mods.ModContext;
import xaos.api.mods.TownsMod;
import xaos.main.Game;
import xaos.utils.Log;

import xaos.api.mods.ModMetadata;

import xaos.internal.api.mods.ModSaveDataRegistry;

/**
 * Loads optional Java plugins from enabled Towns mod folders.
 *
 * A Java-enabled mod contains:
 *
 * mod.properties
 * <mod jar>
 *
 * Existing XML/content-only mods require no changes.
 */
public final class JavaModLoader {

        private static final String METADATA_FILE = "mod.properties";

        private static final List<LoadedJavaMod> loadedMods = new ArrayList<>();

        private static final Set<String> worldActiveMods = new HashSet<>();

        private JavaModLoader() {
        }

        /**
         * Loads Java plugins belonging to currently enabled Towns mods.
         */
        public static void loadEnabledMods() {

                List<String> enabledMods = Game.getModsLoaded();

                if (enabledMods == null
                                || enabledMods.isEmpty()) {
                        return;
                }

                for (String folderName : enabledMods) {
                        if (!isFolderLoaded(folderName)) {
                                loadMod(folderName);
                        }
                }

                Log.log(
                                Log.LEVEL_DEBUG,
                                "Loaded "
                                                + loadedMods.size()
                                                + " Java mod(s)",
                                "JavaModLoader");
        }

        private static boolean isFolderLoaded(String folderName) {
                for (LoadedJavaMod loadedMod : loadedMods) {
                        if (loadedMod.folderName().equals(folderName)) {
                                return true;
                        }
                }

                return false;
        }

        private static void loadMod(String folderName) {

                File modFolder = new File(
                                new File(
                                                Game.getUserFolder(),
                                                Game.MODS_FOLDER1),
                                folderName);

                File metadataFile = new File(modFolder, METADATA_FILE);

                /*
                 * No mod.properties simply means this is an existing
                 * XML/content-only mod.
                 */
                if (!metadataFile.isFile()) {
                        return;
                }

                try {
                        loadJavaMod(
                                        folderName,
                                        modFolder,
                                        metadataFile);
                } catch (Exception | LinkageError exception) {
                        Log.log(
                                        Log.LEVEL_ERROR,
                                        "Failed to load Java mod from folder ["
                                                        + folderName
                                                        + "]: "
                                                        + exception,
                                        "JavaModLoader");
                }
        }

        private static void loadJavaMod(
                        String folderName,
                        File modFolder,
                        File metadataFile) throws Exception {

                Properties properties = loadProperties(metadataFile);

                String modId = requiredProperty(properties, "id");

                String jarName = requiredProperty(properties, "jar");

                String mainClassName = requiredProperty(properties, "main");

                String modName = optionalProperty(
                                properties,
                                "name",
                                modId);

                String version = optionalProperty(
                                properties,
                                "version",
                                "unknown");

                String author = optionalProperty(
                                properties,
                                "author",
                                "unknown");
                ModMetadata metadata = new ModMetadata(
                                modId,
                                modName,
                                version,
                                author);
                validateModId(modId);

                File jarFile = new File(modFolder, jarName);

                if (!jarFile.isFile()) {
                        throw new IOException(
                                        "Mod JAR does not exist: "
                                                        + jarFile.getAbsolutePath());
                }

                URL jarUrl = jarFile.toURI().toURL();

                URLClassLoader classLoader = new URLClassLoader(
                                new URL[] { jarUrl },
                                TownsMod.class.getClassLoader());

                try {
                        TownsMod mod = createModInstance(
                                        classLoader,
                                        mainClassName);

                        ModContext context = new ModContext(
                                        metadata,
                                        GameApi.get(),
                                        new DefaultModLogger(
                                                        modId),
                                        ModSaveDataRegistry.forMod(
                                                        modId));

                        /*
                         * Let the mod initialise before considering it
                         * successfully loaded.
                         */
                        mod.onLoad(context);

                        loadedMods.add(
                                        new LoadedJavaMod(
                                                        folderName,
                                                        modId,
                                                        mod,
                                                        context,
                                                        classLoader));

                        context.logger().info(
                                        "Loaded "
                                                        + metadata.name()
                                                        + " v"
                                                        + metadata.version()
                                                        + " by "
                                                        + metadata.author());

                } catch (Exception | LinkageError exception) {
                        classLoader.close();
                        throw exception;
                }
        }

        private static Properties loadProperties(
                        File file) throws IOException {

                Properties properties = new Properties();

                try (FileInputStream input = new FileInputStream(file)) {

                        properties.load(input);
                }

                return properties;
        }

        private static String requiredProperty(
                        Properties properties,
                        String key) {

                String value = properties.getProperty(key);

                if (value == null
                                || value.isBlank()) {

                        throw new IllegalArgumentException(
                                        "Missing required mod property: "
                                                        + key);
                }

                return value.trim();
        }

        private static void validateModId(String modId) {

                if (!modId.matches("[a-z0-9_.-]+")) {
                        throw new IllegalArgumentException(
                                        "Invalid mod ID ["
                                                        + modId
                                                        + "]. Mod IDs may contain "
                                                        + "lowercase letters, numbers, "
                                                        + "underscores, hyphens and dots.");
                }
        }

        private static String optionalProperty(
                        Properties properties,
                        String key,
                        String defaultValue) {

                String value = properties.getProperty(key);

                if (value == null
                                || value.isBlank()) {
                        return defaultValue;
                }

                return value.trim();
        }

        private static TownsMod createModInstance(
                        ClassLoader classLoader,
                        String mainClassName) throws Exception {

                Class<?> mainClass = Class.forName(
                                mainClassName,
                                true,
                                classLoader);

                if (!TownsMod.class
                                .isAssignableFrom(mainClass)) {

                        throw new IllegalArgumentException(
                                        "Main class ["
                                                        + mainClassName
                                                        + "] does not implement "
                                                        + TownsMod.class.getName());
                }

                Object instance = mainClass
                                .getDeclaredConstructor()
                                .newInstance();

                return (TownsMod) instance;
        }

        /**
         * Notifies all currently enabled Java mods that a playable
         * world has finished loading.
         */
        public static void notifyWorldLoaded() {

                /*
                 * A new world cannot become active while another world is
                 * still considered active.
                 *
                 * This is also a defensive fallback for any game path that
                 * changes worlds without going through exitToMainMenu().
                 */
                notifyWorldUnloaded();

                /*
                 * Pick up any Java mod that may have been enabled since
                 * the initial startup load.
                 */
                loadEnabledMods();

                for (LoadedJavaMod loadedMod : loadedMods) {

                        /*
                         * A Java mod cannot safely be unloaded from the JVM once
                         * its classes have been loaded, but if the user disables
                         * it we can stop sending lifecycle callbacks to it.
                         */
                        if (!Game.isModLoaded(loadedMod.folderName())) {
                                continue;
                        }

                        /*
                         * Mark the mod active before invoking the callback.
                         *
                         * If onWorldLoaded() partially registers resources and
                         * then fails, the next unload can still clean them up.
                         */
                        worldActiveMods.add(
                                        loadedMod.folderName());

                        try {
                                loadedMod.instance().onWorldLoaded(
                                                loadedMod.context());

                        } catch (Exception | LinkageError exception) {

                                loadedMod.context()
                                                .logger()
                                                .error(
                                                                "Error during onWorldLoaded()",
                                                                exception);
                        }
                }
        }

        /**
         * Notifies mods that the current playable world is about to be
         * unloaded, then releases all world-scoped resources.
         */
        public static void notifyWorldUnloaded() {

                for (LoadedJavaMod loadedMod : loadedMods) {

                        /*
                         * Only mods that received onWorldLoaded() for the current
                         * world should receive onWorldUnloaded().
                         */
                        if (!worldActiveMods.remove(
                                        loadedMod.folderName())) {
                                continue;
                        }

                        try {
                                loadedMod.instance().onWorldUnloaded(
                                                loadedMod.context());

                        } catch (Exception | LinkageError exception) {

                                loadedMod.context()
                                                .logger()
                                                .error(
                                                                "Error during onWorldUnloaded()",
                                                                exception);

                        } finally {

                                /*
                                 * World-scoped resources are always released,
                                 * even if the mod throws during its unload hook.
                                 */
                                loadedMod.context()
                                                .clearWorldResources();
                        }
                }
        }

        /**
         * Shuts down the Java mod loader.
         *
         * Any active world is unloaded first. Mods then receive onUnload()
         * in reverse load order before remaining resources and classloaders
         * are released.
         */
        public static void shutdown() {

                notifyWorldUnloaded();

                for (int i = loadedMods.size() - 1;
                                i >= 0;
                                i--) {

                        LoadedJavaMod loadedMod =
                                        loadedMods.get(i);

                        try {
                                loadedMod.instance().onUnload(
                                                loadedMod.context());

                        } catch (Exception | LinkageError exception) {

                                loadedMod.context()
                                                .logger()
                                                .error(
                                                                "Error during onUnload()",
                                                                exception);

                        } finally {

                                loadedMod.context()
                                                .clearAllResources();

                                try {
                                        loadedMod.classLoader().close();

                                } catch (IOException exception) {

                                        loadedMod.context()
                                                        .logger()
                                                        .error(
                                                                        "Error closing mod class loader",
                                                                        exception);
                                }
                        }
                }

                loadedMods.clear();
                worldActiveMods.clear();
        }
}
