package xaos.internal.api.stockpiles;

import java.util.ArrayList;
import java.util.List;

import xaos.api.definitions.DefinitionId;
import xaos.api.entity.EntityId;
import xaos.api.stockpiles.StockpileApi;
import xaos.api.stockpiles.StockpileId;
import xaos.api.stockpiles.StockpileView;
import xaos.api.world.WorldPosition;

import xaos.main.Game;
import xaos.main.World;
import xaos.stockpiles.Stockpile;
import xaos.tiles.entities.items.Item;
import xaos.utils.Point3DShort;

public final class DefaultStockpileApi
        implements StockpileApi {

    @Override
    public StockpileView get(
            StockpileId id
    ) {
        if (id == null) {
            return null;
        }

        Stockpile stockpile =
                Stockpile.getStockpile(
                        id.value()
                );

        return map(stockpile);
    }

    @Override
    public StockpileView at(
            WorldPosition position
    ) {
        if (position == null) {
            return null;
        }

        Stockpile stockpile =
                Stockpile.getStockpile(
                        position.x(),
                        position.y(),
                        position.z()
                );

        return map(stockpile);
    }

    @Override
    public List<StockpileView> all() {
        List<StockpileView> result =
                new ArrayList<>();

        for (Stockpile stockpile
                : Game.getWorld()
                        .getStockpiles()) {

            StockpileView view =
                    map(stockpile);

            if (view != null) {
                result.add(view);
            }
        }

        return List.copyOf(result);
    }

    @Override
    public List<StockpileView> accepting(
            DefinitionId itemDefinitionId
    ) {
        if (itemDefinitionId == null
                || !itemDefinitionId
                        .isDefaultNamespace()) {

            return List.of();
        }

        List<StockpileView> result =
                new ArrayList<>();

        for (Stockpile stockpile
                : Game.getWorld()
                        .getStockpiles()) {

            if (!stockpile.itemAllowed(
                    itemDefinitionId.path())) {
                continue;
            }

            StockpileView view =
                    map(stockpile);

            if (view != null) {
                result.add(view);
            }
        }

        return List.copyOf(result);
    }

    private static StockpileView map(
            Stockpile stockpile
    ) {
        if (stockpile == null) {
            return null;
        }

        List<WorldPosition> positions =
                new ArrayList<>();

        List<EntityId> itemIds =
                new ArrayList<>();

        for (Point3DShort point
                : stockpile.getPoints()) {

            positions.add(
                    new WorldPosition(
                            point.x,
                            point.y,
                            point.z
                    )
            );

            Item item =
                    World.getCell(point)
                            .getItem();

            if (item != null) {
                itemIds.add(
                        new EntityId(
                                item.getID()
                        )
                );
            }
        }

        List<DefinitionId> allowedItems =
                new ArrayList<>();

        for (String itemId
                : stockpile.getType()
                        .getElements()) {

            if (itemId != null
                    && !itemId.isBlank()) {

                allowedItems.add(
                        DefinitionId.towns(
                                itemId
                        )
                );
            }
        }

        return new StockpileView(
                new StockpileId(
                        stockpile.getID()
                ),
                stockpile.getType().getID(),
                positions,
                itemIds,
                allowedItems,
                stockpile.getFilledPoints(),
                stockpile.isLockedToCopy()
        );
    }
}