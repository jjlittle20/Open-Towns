
package xaos.ui.components;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import xaos.ui.layout.UiRect;
import xaos.ui.layout.UiSize;
import xaos.ui.rendering.UiTextMetrics;

class UiSliderTest {

    private static final UiTextMetrics FONT = text -> new UiSize(text.length() * 7, 16);

    private static UiSlider slider(
            AtomicInteger value,
            AtomicInteger commits) {

        UiSlider slider = new UiSlider(
                "Music Volume",
                FONT,
                1,
                10,
                1,
                value::get,
                value::set,
                commits::incrementAndGet,
                current -> (current * 10) + "%");

        slider.layout(new UiRect(30, 40, 320, 54));

        return slider;
    }

    @Test
    void sliderDisplaysLivePercentage() {

        AtomicInteger value = new AtomicInteger(7);

        UiSlider slider = slider(value, new AtomicInteger());

        assertEquals(
                "Music Volume: 70%",
                slider.text());

        value.set(3);
        slider.refresh();

        assertEquals(
                "Music Volume: 30%",
                slider.text());
    }

    @Test
    void draggingUpdatesValueAndCommitsOnce() {

        AtomicInteger value = new AtomicInteger(5);
        AtomicInteger commits = new AtomicInteger();

        UiSlider slider = slider(value, commits);

        UiRect track = slider.trackBounds();

        slider.beginDrag(track.x());

        assertEquals(1, value.get());
        assertEquals(0, commits.get());

        slider.dragTo(
                track.x() + track.width() - 1);

        assertEquals(10, value.get());
        assertEquals(0, commits.get());

        slider.endDrag(
                track.x() + track.width() - 1);

        assertEquals(10, value.get());
        assertEquals(1, commits.get());
        assertFalse(slider.dragging());
    }

    @Test
    void clickingUnchangedValueDoesNotSave() {

        AtomicInteger value = new AtomicInteger(1);
        AtomicInteger commits = new AtomicInteger();

        UiSlider slider = slider(value, commits);

        int x = slider.trackBounds().x();

        slider.beginDrag(x);
        slider.endDrag(x);

        assertEquals(1, value.get());
        assertEquals(0, commits.get());
    }

    @Test
    void cancelledDragCommitsChangedValue() {

        AtomicInteger value = new AtomicInteger(5);
        AtomicInteger commits = new AtomicInteger();

        UiSlider slider = slider(value, commits);

        slider.beginDrag(slider.trackBounds().x());

        assertEquals(1, value.get());

        slider.cancelDrag();

        assertFalse(slider.dragging());
        assertEquals(1, commits.get());

        // Repeated cancellation does not save again.
        slider.cancelDrag();

        assertEquals(1, commits.get());
    }

    @Test
    void keyboardActivationAdvancesOneStep() {

        AtomicInteger value = new AtomicInteger(7);
        AtomicInteger commits = new AtomicInteger();

        UiSlider slider = slider(value, commits);

        // UiButton.activate() is used by the
        // existing Enter/Space keyboard handling.
        slider.activate();

        assertEquals(8, value.get());
        assertEquals(1, commits.get());

        slider.stepBackward();

        assertEquals(7, value.get());
        assertEquals(2, commits.get());
    }

    @Test
    void valuesAreClampedAtBothEnds() {

        AtomicInteger value = new AtomicInteger(10);
        AtomicInteger commits = new AtomicInteger();

        UiSlider slider = slider(value, commits);

        slider.stepForward();

        assertEquals(10, value.get());
        assertEquals(0, commits.get());

        slider.adjustBySteps(-100);

        assertEquals(1, value.get());
        assertEquals(1, commits.get());
    }
}
