package xaos.api.world;

import xaos.main.World;
import xaos.api.definitions.DefinitionId;
import xaos.tiles.Cell;
import xaos.tiles.terrain.TerrainManager;
import xaos.tiles.terrain.TerrainManagerItem;

/**
 * Default implementation of the public World API using the
 * existing Towns world implementation.
 */
public final class DefaultWorldApi implements WorldApi {

    @Override
    public int width() {
        return World.MAP_WIDTH;
    }

    @Override
    public int height() {
        return World.MAP_HEIGHT;
    }

    @Override
    public int depth() {
        return World.MAP_DEPTH;
    }

    @Override
    public boolean isValidPosition(WorldPosition position) {
        if (position == null) {
            return false;
        }

        return position.x() >= 0
                && position.x() < width()
                && position.y() >= 0
                && position.y() < height()
                && position.z() >= 0
                && position.z() < depth();
    }

    @Override
    public CellView cell(WorldPosition position) {
        if (!isValidPosition(position)) {
            return null;
        }

        Cell cell = World.getCell(
                position.x(),
                position.y(),
                position.z());

        if (cell == null) {
            return null;
        }

        return mapCell(position, cell);
    }

    private static CellView mapCell(WorldPosition position, Cell cell) {
        TerrainManagerItem terrain = TerrainManager.getItemByID(
                cell.getTerrain().getTerrainID());

        DefinitionId terrainId = terrain == null
                ? null
                : DefinitionId.towns(terrain.getIniHeader());

        int livingCount = cell.getLivings() == null
                ? 0
                : cell.getLivings().size();

        return new CellView(
                position,
                terrainId,
                cell.isDiscovered(),
                cell.isCave(),
                cell.isDiggable(),
                cell.isEmpty(),
                cell.hasEntity(),
                cell.hasItem(),
                cell.hasBuilding(),
                cell.hasStockPile(),
                cell.getStockPileID(),
                cell.hasZone(),
                cell.getZoneID(),
                livingCount);
    }
}