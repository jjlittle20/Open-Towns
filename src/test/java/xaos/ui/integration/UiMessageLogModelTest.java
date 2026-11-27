
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import xaos.ui.rendering.UiColor;
import xaos.ui.integration.UiMessageLogModel.Channel;
import xaos.ui.integration.UiMessageLogModel.Entry;
import xaos.ui.integration.UiMessageLogModel.WorldPosition;

class UiMessageLogModelTest {

    private static Entry message(String text) {

        return new Entry(
                text,
                UiColor.WHITE,
                null,
                -1);
    }

    @Test
    void startsWithEmptyMessageChannels() {

        UiMessageLogModel model = new UiMessageLogModel();

        for (Channel channel : Channel.values()) {
            assertTrue(model.messages(channel).isEmpty());
        }

        assertEquals(
                Channel.ANNOUNCEMENTS,
                model.selectedChannel());
    }

    @Test
    void channelsMatchLegacyMessageTypes() {

        assertEquals(0, Channel.ANNOUNCEMENTS.legacyType());
        assertEquals(1, Channel.COMBAT.legacyType());
        assertEquals(2, Channel.HEROES.legacyType());
        assertEquals(3, Channel.SYSTEM.legacyType());

        assertEquals(
                Channel.COMBAT,
                Channel.fromLegacyType(1));

        assertThrows(
                IllegalArgumentException.class,
                () -> Channel.fromLegacyType(99));
    }

    @Test
    void changingChannelChangesVisibleMessages() {

        UiMessageLogModel model = new UiMessageLogModel();

        model.replace(
                Channel.ANNOUNCEMENTS,
                List.of(message("A caravan arrived")));

        model.replace(
                Channel.COMBAT,
                List.of(message("A goblin attacks")));

        assertEquals(
                "A caravan arrived",
                model.visibleMessages().get(0).text());

        model.selectChannel(Channel.COMBAT);

        assertEquals(
                "A goblin attacks",
                model.visibleMessages().get(0).text());
    }

    @Test
    void snapshotsPreserveOrderAndCannotBeModified() {

        UiMessageLogModel model = new UiMessageLogModel();

        List<Entry> source = new ArrayList<>();

        source.add(message("First message"));
        source.add(message("Second message"));

        model.replace(Channel.SYSTEM, source);

        // Changes to the source cannot modify the UI snapshot.
        source.clear();

        List<Entry> snapshot = model.messages(Channel.SYSTEM);

        assertEquals(2, snapshot.size());
        assertEquals("First message", snapshot.get(0).text());
        assertEquals("Second message", snapshot.get(1).text());

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.clear());
    }

    @Test
    void messagesRetainNavigationMetadata() {

        Entry locationMessage = new Entry(
                "A citizen is under attack",
                UiColor.rgb(255, 100, 100),
                new WorldPosition(12, 24, 8),
                42);

        assertTrue(locationMessage.hasNavigationTarget());
        assertEquals(42, locationMessage.entityId());
        assertEquals(12, locationMessage.position().x());

        assertFalse(
                message("Plain message").hasNavigationTarget());
    }
}
