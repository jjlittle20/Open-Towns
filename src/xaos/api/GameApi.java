package xaos.api;

import xaos.api.definitions.DefinitionsApi;
import xaos.api.world.WorldApi;
import xaos.internal.api.definitions.DefaultDefinitionsApi;
import xaos.internal.api.entity.DefaultEntityApi;
import xaos.internal.api.ui.DefaultUiApi;
import xaos.internal.api.world.DefaultWorldApi;
import xaos.api.entity.EntityApi;
import xaos.api.ui.UiApi;
import xaos.api.tasks.TaskApi;
import xaos.internal.api.tasks.DefaultTaskApi;
import xaos.api.selection.SelectionApi;
import xaos.internal.api.selection.DefaultSelectionApi;
import xaos.api.events.EventApi;
import xaos.internal.api.events.DefaultEventApi;
import xaos.api.inventory.InventoryApi;
import xaos.internal.api.inventory.DefaultInventoryApi;
import xaos.api.stockpiles.StockpileApi;
import xaos.internal.api.stockpiles.DefaultStockpileApi;
import xaos.api.items.ItemQueryApi;
import xaos.internal.api.items.DefaultItemQueryApi;
import xaos.api.buildings.BuildingApi;
import xaos.internal.api.buildings.DefaultBuildingApi;

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
    private final TaskApi tasks = new DefaultTaskApi();
    private final SelectionApi selection = new DefaultSelectionApi();
    private final EventApi events = new DefaultEventApi();
    private final InventoryApi inventory = new DefaultInventoryApi();
    private final StockpileApi stockpiles = new DefaultStockpileApi();
    private final ItemQueryApi items = new DefaultItemQueryApi();
    private final BuildingApi buildings = new DefaultBuildingApi();

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

    public TaskApi tasks() {
        return tasks;
    }

    public SelectionApi selection() {
        return selection;
    }

    public EventApi events() {
        return events;
    }

    public InventoryApi inventory() {
        return inventory;
    }

    public StockpileApi stockpiles() {
        return stockpiles;
    }

    public ItemQueryApi items() {
        return items;
    }

    public BuildingApi buildings() {
        return buildings;
    }
}