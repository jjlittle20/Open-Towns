package xaos.api.definitions;

import xaos.actions.ActionManager;
import xaos.actions.ActionManagerItem;
import xaos.events.EventManager;
import xaos.events.EventManagerItem;
import xaos.tiles.entities.buildings.BuildingManager;
import xaos.tiles.entities.buildings.BuildingManagerItem;
import xaos.tiles.entities.items.ItemManager;
import xaos.tiles.entities.items.ItemManagerItem;
import xaos.tiles.entities.living.LivingEntityManager;
import xaos.tiles.entities.living.LivingEntityManagerItem;

/** Default bridge from Towns' existing XML managers to the public API. */
public final class DefaultDefinitionsApi implements DefinitionsApi {

    private final DefinitionRegistry<ItemDefinition> items = new MappedDefinitionRegistry<>(
            ItemManager::getItem,
            ItemManager::getAllItems,
            DefaultDefinitionsApi::mapItem);

    private final DefinitionRegistry<LivingDefinition> living = new MappedDefinitionRegistry<>(
            LivingEntityManager::getItem,
            LivingEntityManager::getAllItems,
            DefaultDefinitionsApi::mapLiving);

    private final DefinitionRegistry<BuildingDefinition> buildings = new MappedDefinitionRegistry<>(
            BuildingManager::getItem,
            BuildingManager::getAllItems,
            DefaultDefinitionsApi::mapBuilding);

    private final DefinitionRegistry<ActionDefinition> actions = new MappedDefinitionRegistry<>(
            ActionManager::getItem,
            ActionManager::getAllItems,
            DefaultDefinitionsApi::mapAction);

    private final DefinitionRegistry<EventDefinition> events = new MappedDefinitionRegistry<>(
            EventManager::getItem,
            EventManager::getAllItems,
            DefaultDefinitionsApi::mapEvent);

    @Override
    public DefinitionRegistry<ItemDefinition> items() {
        return items;
    }

    @Override
    public DefinitionRegistry<LivingDefinition> living() {
        return living;
    }

    @Override
    public DefinitionRegistry<BuildingDefinition> buildings() {
        return buildings;
    }

    @Override
    public DefinitionRegistry<ActionDefinition> actions() {
        return actions;
    }

    @Override
    public DefinitionRegistry<EventDefinition> events() {
        return events;
    }

    private static ItemDefinition mapItem(ItemManagerItem item) {
        return new ItemDefinition(
                item.getIniHeader(),
                item.getName(),
                item.getType(),
                item.getLevel(),
                item.getBuilding(),
                item.getBuildingTime(),
                item.getValue(),
                item.getTags(),
                item.getActions(),
                item.isWall(),
                item.isDoor(),
                item.isContainer(),
                item.isStackable(),
                item.isMilitaryItem());
    }

    private static LivingDefinition mapLiving(LivingEntityManagerItem living) {
        return new LivingDefinition(
                living.getIniHeader(),
                living.getName(),
                living.getType(),
                living.getLevel(),
                living.getBuilding(),
                living.getBuildingTime(),
                living.isFemale(),
                living.getActions(),
                living.getEffects());
    }

    private static BuildingDefinition mapBuilding(BuildingManagerItem building) {
        return new BuildingDefinition(
                building.getIniHeader(),
                building.getName(),
                building.getType(),
                building.getWidth(),
                building.getHeight(),
                building.isAutomatic(),
                building.getDescriptions());
    }

    private static ActionDefinition mapAction(ActionManagerItem action) {
        return new ActionDefinition(
                action.getId(),
                action.getName(),
                action.getPriorityID(),
                action.getTurns(),
                action.getEffect(),
                action.getGeneratedItem(),
                action.isKillsSource(),
                action.isInverted());
    }

    private static EventDefinition mapEvent(EventManagerItem event) {
        return new EventDefinition(
                event.getId(),
                event.getName(),
                event.getOrder(),
                event.getMinPopulation(),
                event.isSpawnAtRandom(),
                event.getTurns(),
                event.getEventCooldown(),
                event.isSiege(),
                event.getPrerequisites(),
                event.getAfterEvents(),
                event.getEffects(),
                event.getItems());
    }
}
