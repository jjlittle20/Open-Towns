
package xaos.ui.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import xaos.ui.integration.UiLegacyMessagesReader.LegacyMessage;
import xaos.ui.integration.UiMessageLogModel.Channel;
import xaos.ui.rendering.UiColor;
import xaos.utils.ColorGL;
import xaos.utils.Point3DShort;

class UiLegacyMessagesReaderTest {

    private static LegacyMessage message(String text) {

        return new LegacyMessage(
                text,
                ColorGL.WHITE,
                null,
                -1);
    }

    @Test
    void readsChannelsAndPreservesMessageOrder() {

        Map<Integer, List<LegacyMessage>> source = new HashMap<>();

        source.put(0, List.of(
                message("First announcement"),
                message("Second announcement")));

        source.put(1, List.of(
                message("Goblin attacks")));

        UiLegacyMessagesReader reader = new UiLegacyMessagesReader(
                type -> source.getOrDefault(
                        type, List.of()));

        UiMessageLogModel model = new UiMessageLogModel();

        model.selectChannel(Channel.COMBAT);

        reader.refresh(model);

        assertEquals(
                Channel.COMBAT,
                model.selectedChannel());

        assertEquals(
                "Goblin attacks",
                model.visibleMessages().get(0).text());

        assertEquals(
                List.of(
                        "First announcement",
                        "Second announcement"),
                model.messages(Channel.ANNOUNCEMENTS)
                        .stream()
                        .map(UiMessageLogModel.Entry::text)
                        .toList());

        assertTrue(
                model.messages(Channel.SYSTEM).isEmpty());
    }

    @Test
    void copiesMutableColoursAndCoordinates() {

        ColorGL color = new ColorGL(
                0.5f, 0.25f, 1.0f);

        Point3DShort position = new Point3DShort(
                (short) 12,
                (short) 24,
                (short) 8);

        UiLegacyMessagesReader reader = new UiLegacyMessagesReader(
                type -> type == 3
                        ? List.of(new LegacyMessage(
                                "Citizen needs help",
                                color,
                                position,
                                42))
                        : List.of());

        UiMessageLogModel model = new UiMessageLogModel();

        reader.refresh(model);

        var entry = model.messages(Channel.SYSTEM).get(0);

        assertEquals(
                UiColor.rgb(128, 64, 255),
                entry.color());

        assertEquals(12, entry.position().x());
        assertEquals(42, entry.entityId());
        assertTrue(entry.hasNavigationTarget());

        // Mutating legacy data cannot change
        // the already captured UI snapshot.
        color.r = 0f;
        position.x = 99;

        assertEquals(128, entry.color().red());
        assertEquals(12, entry.position().x());
    }

    @Test
    void handlesMissingEntriesAndColours() {

        UiLegacyMessagesReader reader = new UiLegacyMessagesReader(
                type -> type == 2
                        ? Arrays.asList(
                                null,
                                new LegacyMessage(
                                        null, null, null, -1),
                                new LegacyMessage(
                                        "Hero returned",
                                        null, null, -1))
                        : List.of());

        UiMessageLogModel model = new UiMessageLogModel();

        reader.refresh(model);

        var heroes = model.messages(Channel.HEROES);

        assertEquals(1, heroes.size());
        assertEquals("Hero returned", heroes.get(0).text());
        assertEquals(UiColor.WHITE, heroes.get(0).color());
        assertFalse(heroes.get(0).hasNavigationTarget());
    }

    @Test
    void refreshPicksUpNewMessages() {

        Map<Integer, List<LegacyMessage>> source = new HashMap<>();

        UiLegacyMessagesReader reader = new UiLegacyMessagesReader(
                type -> source.getOrDefault(
                        type, List.of()));

        UiMessageLogModel model = new UiMessageLogModel();

        model.selectChannel(Channel.SYSTEM);

        source.put(3, List.of(message("First")));
        reader.refresh(model);

        assertEquals(1, model.visibleMessages().size());

        source.put(3, List.of(
                message("First"),
                message("Second")));

        reader.refresh(model);

        assertEquals(2, model.visibleMessages().size());

        assertEquals(
                "Second",
                model.visibleMessages().get(1).text());

        assertEquals(
                Channel.SYSTEM,
                model.selectedChannel());
    }
}
