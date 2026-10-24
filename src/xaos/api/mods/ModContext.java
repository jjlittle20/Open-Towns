package xaos.api.mods;

import xaos.api.GameApi;

/**
 * Context supplied to a Java mod by the mod loader.
 *
 * Provides access to the public game API and information about
 * the currently loaded mod.
 */
public final class ModContext {

    private final ModMetadata metadata;
    private final GameApi api;
    private final ModLogger logger;
    private final ModSaveData saveData;

    public ModContext(
            ModMetadata metadata,
            GameApi api,
            ModLogger logger,
            ModSaveData saveData) {

        if (metadata == null) {
            throw new IllegalArgumentException(
                    "Mod metadata cannot be null");
        }

        if (api == null) {
            throw new IllegalArgumentException(
                    "Game API cannot be null");
        }

        if (logger == null) {
            throw new IllegalArgumentException(
                    "Mod logger cannot be null");
        }

        if (saveData == null) {
            throw new IllegalArgumentException(
                    "Mod save data cannot be null");
        }

        this.metadata = metadata;
        this.api = api;
        this.logger = logger;
        this.saveData = saveData;
    }

    /**
     * Metadata describing the currently loaded mod.
     */
    public ModMetadata metadata() {
        return metadata;
    }

    /**
     * Convenience accessor for the mod ID.
     */
    public String modId() {
        return metadata.id();
    }

    /**
     * Entry point to the public Open Towns API.
     */
    public GameApi api() {
        return api;
    }

    /**
     * Logger scoped to this mod.
     */
    public ModLogger logger() {
        return logger;
    }

    /**
     * Persistent data belonging exclusively to this mod.
     */
    public ModSaveData saveData() {
        return saveData;
    }
}