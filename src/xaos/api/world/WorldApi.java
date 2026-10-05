package xaos.api.world;

/**
 * Public API for querying the game world.
 *
 * This interface deliberately hides the internal World and Cell
 * implementations from mods.
 */
public interface WorldApi {

    /**
     * Returns the width of the current world in cells.
     */
    int width();

    /**
     * Returns the height of the current world in cells.
     */
    int height();

    /**
     * Returns the number of vertical levels in the current world.
     */
    int depth();

    /**
     * Returns true if the supplied position exists within the
     * bounds of the current world.
     */
    boolean isValidPosition(WorldPosition position);
}