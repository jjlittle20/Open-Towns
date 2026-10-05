package xaos.api;

import xaos.api.definitions.DefaultDefinitionsApi;
import xaos.api.definitions.DefinitionsApi;

/**
 * Root of the stable public API exposed to future plugins and modding tools.
 */
public final class GameApi {

    private static final GameApi INSTANCE = new GameApi();

    private final DefinitionsApi definitions = new DefaultDefinitionsApi();

    private GameApi() {
    }

    public static GameApi get() {
        return INSTANCE;
    }

    public DefinitionsApi definitions() {
        return definitions;
    }
}
