package xaos.internal.api.inventory;

import java.util.List;

import xaos.api.definitions.DefinitionId;
import xaos.api.entity.EntityId;
import xaos.api.inventory.ContainerView;
import xaos.api.inventory.InventoryApi;
import xaos.api.inventory.InventoryMutationResult;

import xaos.internal.api.definitions.DefinitionResolver;

import xaos.main.Game;
import xaos.tiles.entities.items.Container;
import xaos.tiles.entities.items.Item;
import xaos.tiles.entities.items.ItemManager;
import xaos.tiles.entities.items.ItemManagerItem;

public final class DefaultInventoryApi
        implements InventoryApi {

    @Override
    public ContainerView container(
            EntityId containerId
    ) {
        if (containerId == null) {
            return null;
        }

        Container container =
                Game.getWorld()
                        .getContainer(
                                containerId.value()
                        );

        if (container == null) {
            return null;
        }

        List<EntityId> items =
                container.getItemsInside()
                        .stream()
                        .map(Item::getID)
                        .map(EntityId::new)
                        .toList();

        return new ContainerView(
                containerId,
                items,
                container.isFull()
        );
    }

    @Override
    public int count(
            EntityId containerId,
            DefinitionId itemDefinitionId
    ) {
        if (containerId == null
                || itemDefinitionId == null) {
            return 0;
        }

        String legacyId =
                DefinitionResolver.resolveItem(
                        itemDefinitionId
                );

        if (legacyId == null) {
            return 0;
        }

        Container container =
                Game.getWorld()
                        .getContainer(
                                containerId.value()
                        );

        if (container == null) {
            return 0;
        }

        int count = 0;

        for (Item item : container.getItemsInside()) {
            if (legacyId.equals(
                    item.getIniHeader()
            )) {
                count++;
            }
        }

        return count;
    }

    @Override
    public InventoryMutationResult consume(
            EntityId containerId,
            DefinitionId itemDefinitionId,
            int quantity
    ) {
        if (quantity <= 0) {
            return new InventoryMutationResult(
                    Math.max(quantity, 0),
                    0,
                    "Quantity must be greater than zero"
            );
        }

        if (containerId == null
                || itemDefinitionId == null) {

            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Container and item definition are required"
            );
        }

        String legacyId =
                DefinitionResolver.resolveItem(
                        itemDefinitionId
                );

        if (legacyId == null) {
            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Unknown item definition: "
                            + itemDefinitionId
            );
        }

        Container container =
                Game.getWorld()
                        .getContainer(
                                containerId.value()
                        );

        if (container == null) {
            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Container was not found"
            );
        }

        int removed = 0;

        for (int i = 0; i < quantity; i++) {

            Item item =
                    container.removeItem(
                            legacyId
                    );

            if (item == null) {
                break;
            }

            removed++;
        }

        return new InventoryMutationResult(
                quantity,
                removed,
                removed == quantity
                        ? null
                        : "Not enough matching items"
        );
    }

    @Override
    public InventoryMutationResult create(
            EntityId containerId,
            DefinitionId itemDefinitionId,
            int quantity
    ) {
        if (quantity <= 0) {
            return new InventoryMutationResult(
                    Math.max(quantity, 0),
                    0,
                    "Quantity must be greater than zero"
            );
        }

        if (containerId == null
                || itemDefinitionId == null) {

            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Container and item definition are required"
            );
        }

        String legacyId =
                DefinitionResolver.resolveItem(
                        itemDefinitionId
                );

        if (legacyId == null) {
            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Unknown item definition: "
                            + itemDefinitionId
            );
        }

        ItemManagerItem definition =
                ItemManager.getItem(
                        legacyId
                );

        if (definition == null) {
            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Unknown item definition: "
                            + itemDefinitionId
            );
        }

        Container container =
                Game.getWorld()
                        .getContainer(
                                containerId.value()
                        );

        if (container == null) {
            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Container was not found"
            );
        }

        int created = 0;

        for (int i = 0; i < quantity; i++) {

            Item item =
                    Item.createItem(
                            definition
                    );

            if (item == null) {
                break;
            }

            /*
             * Let the existing Towns container rules
             * decide whether this item can be stored.
             */
            if (!container.itemAllowed(item)) {
                break;
            }

            container.addItem(item);
            created++;
        }

        return new InventoryMutationResult(
                quantity,
                created,
                created == quantity
                        ? null
                        : "Container could not accept all requested items"
        );
    }
}