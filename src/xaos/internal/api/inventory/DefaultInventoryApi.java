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

                String legacyId = DefinitionResolver.resolveItem(
                                itemDefinitionId);

                if (legacyId == null) {
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
                        if (legacyId.equals(
                                        item.getIniHeader())) {
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

                String legacyId = DefinitionResolver.resolveItem(
                                itemDefinitionId);

                if (legacyId == null) {
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

                int removed = 0;

                for (int i = 0; i < quantity; i++) {

                        Item item = container.removeItem(
                                        legacyId);

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

                String legacyId = DefinitionResolver.resolveItem(
                                itemDefinitionId);

                if (legacyId == null) {
                        return new InventoryMutationResult(
                                        quantity,
                                        0,
                                        "Unknown item definition: "
                                                        + itemDefinitionId);
                }

                ItemManagerItem definition = ItemManager.getItem(
                                legacyId);

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

                        Item item = Item.createItem(
                                        definition);

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
                                                : "Container could not accept all requested items");
        }

        @Override
        public InventoryMutationResult transferItem(
                        EntityId sourceContainerId,
                        EntityId targetContainerId,
                        EntityId itemId) {
                if (sourceContainerId == null
                                || targetContainerId == null
                                || itemId == null) {

                        return new InventoryMutationResult(
                                        1,
                                        0,
                                        "Source container, target container and item are required");
                }

                if (sourceContainerId.equals(
                                targetContainerId)) {
                        return new InventoryMutationResult(
                                        1,
                                        0,
                                        "Source and target containers must be different");
                }

                Container source = Game.getWorld()
                                .getContainer(
                                                sourceContainerId.value());

                if (source == null) {
                        return new InventoryMutationResult(
                                        1,
                                        0,
                                        "Source container was not found");
                }

                Container target = Game.getWorld()
                                .getContainer(
                                                targetContainerId.value());

                if (target == null) {
                        return new InventoryMutationResult(
                                        1,
                                        0,
                                        "Target container was not found");
                }

                Item item = findItem(
                                source,
                                itemId.value());

                if (item == null) {
                        return new InventoryMutationResult(
                                        1,
                                        0,
                                        "Item is not inside the source container");
                }

                /*
                 * Don't steal an item already reserved by the
                 * existing Towns custom-action system.
                 */
                if (isItemInUse(
                                item.getID())) {
                        return new InventoryMutationResult(
                                        1,
                                        0,
                                        "Item is currently in use");
                }

                /*
                 * Validate the destination before altering
                 * the source container.
                 */
                if (!target.itemAllowed(item)) {
                        return new InventoryMutationResult(
                                        1,
                                        0,
                                        "Target container cannot accept the item");
                }

                /*
                 * Remove the actual Item instance from the source.
                 *
                 * Container.removeItem() updates Towns' internal
                 * item counts, town value and container capacity.
                 */
                Item transferred = source.removeItem(
                                item.getID());

                if (transferred == null) {
                        return new InventoryMutationResult(
                                        1,
                                        0,
                                        "Item could not be removed from the source container");
                }

                /*
                 * Add that same instance to the destination.
                 *
                 * This preserves runtime ID and any per-item state.
                 */
                target.addItem(
                                transferred);

                return new InventoryMutationResult(
                                1,
                                1,
                                null);
        }

        @Override
        public InventoryMutationResult transfer(
                        EntityId sourceContainerId,
                        EntityId targetContainerId,
                        DefinitionId itemDefinitionId,
                        int quantity) {
                if (quantity <= 0) {
                        return new InventoryMutationResult(
                                        Math.max(
                                                        quantity,
                                                        0),
                                        0,
                                        "Quantity must be greater than zero");
                }

                if (sourceContainerId == null
                                || targetContainerId == null
                                || itemDefinitionId == null) {

                        return new InventoryMutationResult(
                                        quantity,
                                        0,
                                        "Source container, target container and item definition are required");
                }

                if (sourceContainerId.equals(
                                targetContainerId)) {
                        return new InventoryMutationResult(
                                        quantity,
                                        0,
                                        "Source and target containers must be different");
                }

                String legacyId = DefinitionResolver.resolveItem(
                                itemDefinitionId);

                if (legacyId == null) {
                        return new InventoryMutationResult(
                                        quantity,
                                        0,
                                        "Unknown item definition: "
                                                        + itemDefinitionId);
                }

                Container source = Game.getWorld()
                                .getContainer(
                                                sourceContainerId.value());

                if (source == null) {
                        return new InventoryMutationResult(
                                        quantity,
                                        0,
                                        "Source container was not found");
                }

                Container target = Game.getWorld()
                                .getContainer(
                                                targetContainerId.value());

                if (target == null) {
                        return new InventoryMutationResult(
                                        quantity,
                                        0,
                                        "Target container was not found");
                }

                int moved = 0;

                for (int i = 0; i < quantity; i++) {

                        Item candidate = findTransferCandidate(
                                        source,
                                        legacyId);

                        if (candidate == null) {
                                break;
                        }

                        /*
                         * Capacity/type rules are checked before
                         * removing anything from the source.
                         */
                        if (!target.itemAllowed(
                                        candidate)) {
                                break;
                        }

                        Item transferred = source.removeItem(
                                        candidate.getID());

                        if (transferred == null) {
                                break;
                        }

                        target.addItem(
                                        transferred);

                        moved++;
                }

                String message = null;

                if (moved < quantity) {
                        if (moved == 0) {
                                message = "No transferable matching items were available";
                        } else {
                                message = "Only part of the requested quantity could be transferred";
                        }
                }

                return new InventoryMutationResult(
                                quantity,
                                moved,
                                message);
        }

        private static Item findItem(
                        Container container,
                        int itemId) {
                for (Item item : container.getItemsInside()) {

                        if (item.getID() == itemId) {

                                return item;
                        }
                }

                return null;
        }

        private static Item findTransferCandidate(
                        Container container,
                        String legacyId) {
                for (Item item : container.getItemsInside()) {

                        if (!legacyId.equals(
                                        item.getIniHeader())) {
                                continue;
                        }

                        /*
                         * Ignore anything already being used by an
                         * existing Towns custom action.
                         */
                        if (isItemInUse(
                                        item.getID())) {
                                continue;
                        }

                        return item;
                }

                return null;
        }

        private static boolean isItemInUse(
                        int itemId) {
                List<Integer> itemsInUse = Item.searchItemInUse(
                                -1);

                return itemsInUse != null
                                && itemsInUse.contains(
                                                itemId);
        }
}