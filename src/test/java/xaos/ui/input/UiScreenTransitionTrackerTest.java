
package xaos.ui.input;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UiScreenTransitionTrackerTest {

    @Test
    void initialStateDoesNotCountAsTransition() {

        UiScreenTransitionTracker tracker =
                new UiScreenTransitionTracker();

        assertFalse(tracker.update(true, null));
        assertFalse(tracker.update(true, null));
    }

    @Test
    void enteringAndLeavingGameAreDetected() {

        UiScreenTransitionTracker tracker =
                new UiScreenTransitionTracker();

        Object world = new Object();

        tracker.update(true, null);

        // Start a game.
        assertTrue(tracker.update(false, world));

        // No change.
        assertFalse(tracker.update(false, world));

        // Return to the main menu.
        assertTrue(tracker.update(true, null));

        assertFalse(tracker.update(true, null));
    }

    @Test
    void replacingActiveWorldCountsAsTransition() {

        UiScreenTransitionTracker tracker =
                new UiScreenTransitionTracker();

        Object firstWorld = new Object();
        Object secondWorld = new Object();

        tracker.update(false, firstWorld);

        // Load another world while already in gameplay.
        assertTrue(tracker.update(false, secondWorld));

        assertFalse(tracker.update(false, secondWorld));
    }

    @Test
    void worldIdentityIsUsedInsteadOfEquality() {

        UiScreenTransitionTracker tracker =
                new UiScreenTransitionTracker();

        // These are equal strings but distinct objects.
        Object first = new String("world");
        Object second = new String("world");

        tracker.update(false, first);

        assertTrue(tracker.update(false, second));
    }
}
