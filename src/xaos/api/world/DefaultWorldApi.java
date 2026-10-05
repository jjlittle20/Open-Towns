package xaos.api.world;

import xaos.main.World;

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
}