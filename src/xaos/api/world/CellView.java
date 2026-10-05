package xaos.api.world;

import xaos.api.definitions.DefinitionId;

/**
 * Read-only snapshot of a cell in the game world.
 *
 * This deliberately exposes cell state without exposing the
 * internal xaos.tiles.Cell object.
 */
public record CellView(
        WorldPosition position,
        DefinitionId terrainId,
        boolean discovered,
        boolean cave,
        boolean diggable,
        boolean empty,
        boolean hasEntity,
        boolean hasItem,
        boolean hasBuilding,
        boolean hasStockpile,
        int stockpileId,
        boolean hasZone,
        int zoneId,
        int livingCount
) {

    /**
     * Returns true if one or more living entities are currently
     * occupying this cell.
     */
    public boolean hasLivings() {
        return livingCount > 0;
    }
}