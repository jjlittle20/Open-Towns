
package xaos.ui.input;

/**
 * Detects transitions between the main menu and
 * gameplay, including changes to the active world.
 *
 * This class only detects changes. The caller
 * decides what to do when a transition occurs.
 */
public final class UiScreenTransitionTracker {

    private boolean initialized;
    private boolean mainMenuActive;
    private Object world;

    /**
     * Updates the observed screen state.
     *
     * @return true when the screen or world changed
     */
    public boolean update(
            boolean mainMenuActive,
            Object world) {

        if (!initialized) {

            this.mainMenuActive = mainMenuActive;
            this.world = world;
            initialized = true;

            return false;
        }

        boolean changed =
                this.mainMenuActive != mainMenuActive
                || this.world != world;

        this.mainMenuActive = mainMenuActive;
        this.world = world;

        return changed;
    }
}
