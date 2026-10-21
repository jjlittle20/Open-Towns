package xaos.api.inventory;

import java.util.Optional;

import xaos.api.definitions.DefinitionId;
import xaos.api.entity.EntityId;

public interface InventoryApi {

    ContainerView container(
            EntityId containerId);

    default Optional<ContainerView> findContainer(
            EntityId containerId) {
        return Optional.ofNullable(
                container(containerId));
    }

    int count(
            EntityId containerId,
            DefinitionId itemDefinitionId);

    default boolean contains(
            EntityId containerId,
            DefinitionId itemDefinitionId) {
        return count(
                containerId,
                itemDefinitionId) > 0;
    }

    InventoryMutationResult consume(
            EntityId containerId,
            DefinitionId itemDefinitionId,
            int quantity);

    InventoryMutationResult create(
            EntityId containerId,
            DefinitionId itemDefinitionId,
            int quantity);
}