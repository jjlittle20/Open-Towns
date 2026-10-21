package xaos.internal.api.items;

import java.util.ArrayList;
import java.util.List;

import xaos.api.definitions.DefinitionId;
import xaos.api.entity.EntityId;
import xaos.api.items.ItemLocationType;
import xaos.api.items.ItemQueryApi;
import xaos.api.items.ItemReference;

import xaos.main.Game;
import xaos.main.World;
import xaos.tiles.entities.items.Container;
import xaos.tiles.entities.items.Item;

public final class DefaultItemQueryApi
        implements ItemQueryApi {

    @Override
    public List<ItemReference> find(
            DefinitionId definitionId
    ) {
        if (definitionId == null
                || !definitionId.isDefaultNamespace()) {

            return List.of();
        }

        List<ItemReference> result =
                new ArrayList<>();

        /*
         * Loose / placed items currently in the world.
         */
        for (Item item
                : World.getItems().values()) {

            if (!definitionId.path()
                    .equals(item.getIniHeader())) {
                continue;
            }

            result.add(
                    new ItemReference(
                            new EntityId(item.getID()),
                            definitionId,
                            ItemLocationType.WORLD,
                            null
                    )
            );
        }

        /*
         * Items stored inside containers are not present
         * in World.getItems(), so inspect them separately.
         */
        for (Container container
                : Game.getWorld().getContainers()) {

            for (Item item
                    : container.getItemsInside()) {

                if (!definitionId.path()
                        .equals(item.getIniHeader())) {
                    continue;
                }

                result.add(
                        new ItemReference(
                                new EntityId(item.getID()),
                                definitionId,
                                ItemLocationType.CONTAINER,
                                new EntityId(
                                        container.getItemID()
                                )
                        )
                );
            }
        }

        return List.copyOf(result);
    }
}