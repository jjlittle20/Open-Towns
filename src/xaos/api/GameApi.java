package xaos.api;

import xaos.api.definitions.DefaultDefinitionsApi;
import xaos.api.definitions.DefinitionsApi;
import xaos.api.world.DefaultWorldApi;
import xaos.api.world.WorldApi;
import xaos.api.entity.DefaultEntityApi;
import xaos.api.entity.EntityApi;
import xaos.api.ui.DefaultUiApi;
import xaos.api.ui.UiApi;

/**
 * Main entry point for the public Open Towns API.
 *
 * Mods and external tools should access game systems through this
 * class rather than depending directly on internal engine classes.
 */
public final class GameApi {

    private static final GameApi INSTANCE = new GameApi();

    private final DefinitionsApi definitions = new DefaultDefinitionsApi();
    private final WorldApi world = new DefaultWorldApi();
    private final EntityApi entities = new DefaultEntityApi();
    private final UiApi ui = new DefaultUiApi();

    private GameApi() {
    }

    /**
     * Returns the shared game API instance.
     */
    public static GameApi get() {
        return INSTANCE;
    }

    /**
     * Access to game content definitions such as items,
     * buildings, living entities, actions and events.
     */
    public DefinitionsApi definitions() {
        return definitions;
    }

    /**
     * Access to the current game world.
     */
    public WorldApi world() {
        return world;
    }

    /**
     * Access to runtime entities in the current world.
     */
    public EntityApi entities() {
        return entities;
    }

    /**
     * Access to public user-interface functionality.
     */
    public UiApi ui() {
        return ui;
    }
}