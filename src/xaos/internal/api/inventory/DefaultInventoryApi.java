package xaos.internal.api.inventory;

import java.util.List;

import xaos.api.definitions.DefinitionId;
import xaos.api.entity.EntityId;
import xaos.api.inventory.ContainerView;
import xaos.api.inventory.InventoryApi;
import xaos.api.inventory.InventoryMutationResult;
import xaos.main.Game;
import xaos.tiles.entities.items.Container;
import xaos.tiles.entities.items.Item;
import xaos.tiles.entities.items.ItemManager;
import xaos.tiles.entities.items.ItemManagerItem;

public final class DefaultInventoryApi
        implements InventoryApi {

    @Override
    public ContainerView container(
            EntityId containerId) {
        if (containerId == null) {
            return null;
        }

        Container container = Game.getWorld()
                .getContainer(
                        containerId.value());

        if (container == null) {
            return null;
        }

        List<EntityId> items = container.getItemsInside()
                .stream()
                .map(Item::getID)
                .map(EntityId::new)
                .toList();

        return new ContainerView(
                containerId,
                items,
                container.isFull());
    }

    @Override
    public int count(
            EntityId containerId,
            DefinitionId itemDefinitionId) {
        if (containerId == null
                || itemDefinitionId == null) {
            return 0;
        }

        /*
         * Existing Towns item definitions currently use
         * the default towns namespace.
         */
        if (!itemDefinitionId.isDefaultNamespace()) {
            return 0;
        }

        Container container = Game.getWorld()
                .getContainer(
                        containerId.value());

        if (container == null) {
            return 0;
        }

        int count = 0;

        for (Item item : container.getItemsInside()) {

            if (itemDefinitionId.path()
                    .equals(item.getIniHeader())) {

                count++;
            }
        }

        return count;
    }

    @Override
    public InventoryMutationResult consume(
            EntityId containerId,
            DefinitionId itemDefinitionId,
            int quantity) {
        if (quantity <= 0) {
            return new InventoryMutationResult(
                    Math.max(quantity, 0),
                    0,
                    "Quantity must be greater than zero");
        }

        if (containerId == null
                || itemDefinitionId == null) {

            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Container and item definition are required");
        }

        if (!itemDefinitionId.isDefaultNamespace()) {
            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Only Towns item definitions are supported currently");
        }

        Container container = Game.getWorld()
                .getContainer(
                        containerId.value());

        if (container == null) {
            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Container was not found");
        }

        int removed = 0;

        for (int i = 0; i < quantity; i++) {
            Item item = container.removeItem(
                    itemDefinitionId.path());

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
                        : "Not enough matching items");
    }

    @Override
    public InventoryMutationResult create(
            EntityId containerId,
            DefinitionId itemDefinitionId,
            int quantity) {
        if (quantity <= 0) {
            return new InventoryMutationResult(
                    Math.max(quantity, 0),
                    0,
                    "Quantity must be greater than zero");
        }

        if (containerId == null
                || itemDefinitionId == null) {

            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Container and item definition are required");
        }

        if (!itemDefinitionId.isDefaultNamespace()) {
            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Only Towns item definitions are supported currently");
        }

        ItemManagerItem definition = ItemManager.getItem(
                itemDefinitionId.path());

        if (definition == null) {
            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Unknown item definition: "
                            + itemDefinitionId);
        }

        Container container = Game.getWorld()
                .getContainer(
                        containerId.value());

        if (container == null) {
            return new InventoryMutationResult(
                    quantity,
                    0,
                    "Container was not found");
        }

        int created = 0;

        for (int i = 0; i < quantity; i++) {

            Item item = Item.createItem(definition);

            if (item == null) {
                break;
            }

            /*
             * Let Towns enforce container size/type rules.
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
                        : "Container could not accept all requested items");
    }
}