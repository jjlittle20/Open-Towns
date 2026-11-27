
package xaos.ui.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

import xaos.panels.MessagesPanel;
import xaos.panels.MessagesPanelData;
import xaos.ui.integration.UiMessageLogModel.Channel;
import xaos.ui.integration.UiMessageLogModel.Entry;
import xaos.ui.integration.UiMessageLogModel.WorldPosition;
import xaos.ui.rendering.UiColor;
import xaos.utils.ColorGL;
import xaos.utils.Point3DShort;

/**
 * Read-only bridge between the original Towns
 * message history and the modern UI model.
 *
 * Does not alter saved messages, page positions,
 * blinking indicators or legacy rendering.
 *
 * Read on the game/UI thread.
 */
public final class UiLegacyMessagesReader {

    /**
     * Raw message data supplied by the legacy
     * system or an isolated test.
     */
    record LegacyMessage(
            String text,
            ColorGL color,
            Point3DShort position,
            int entityId) {
    }

    private final IntFunction<List<LegacyMessage>> source;

    // Injectable source for isolated tests.
    UiLegacyMessagesReader(
            IntFunction<List<LegacyMessage>> source) {

        this.source = Objects.requireNonNull(source);
    }

    /**
     * Production reader backed by MessagesPanel.
     */
    public static UiLegacyMessagesReader forGame() {

        return new UiLegacyMessagesReader(
                UiLegacyMessagesReader::readLegacy);
    }

    /**
     * Refresh all four message channels.
     *
     * The selected channel is preserved.
     */
    public void refresh(UiMessageLogModel model) {

        Objects.requireNonNull(model);

        for (Channel channel : Channel.values()) {

            List<LegacyMessage> original = Objects.requireNonNull(
                    source.apply(channel.legacyType()),
                    "Legacy message source returned null");

            List<Entry> snapshot = new ArrayList<>(original.size());

            for (LegacyMessage message : original) {

                if (message == null
                        || message.text() == null) {
                    continue;
                }

                snapshot.add(new Entry(
                        message.text(),
                        convertColor(message.color()),
                        convertPosition(message.position()),
                        message.entityId()));
            }

            model.replace(channel, snapshot);
        }
    }

    /**
     * Read the original, unwrapped message array.
     *
     * Even-numbered slots contain original messages.
     * Odd-numbered slots contain legacy render lines.
     */
    private static List<LegacyMessage> readLegacy(int type) {

        var storage = MessagesPanel.getMessagesDataFull();

        int slot = type * 2;

        if (storage == null
                || slot < 0
                || slot >= storage.size()) {

            return List.of();
        }

        MessagesPanelData[] history = storage.get(slot);

        if (history == null) {
            return List.of();
        }

        List<LegacyMessage> result = new ArrayList<>();

        // Oldest to newest, matching the model.
        for (MessagesPanelData message : history) {

            if (message == null) {
                continue;
            }

            result.add(new LegacyMessage(
                    message.getMessage(),
                    message.getColor(),
                    message.getView(),
                    message.getEntityID()));
        }

        return result;
    }

    /**
     * Convert mutable legacy RGB floats to the
     * modern immutable UI colour.
     *
     * ColorGL uses division by 256 when reading
     * java.awt.Color, so multiply by 256 to
     * recover those original channel values.
     */
    private static UiColor convertColor(ColorGL color) {

        if (color == null) {
            return UiColor.WHITE;
        }

        return UiColor.rgb(
                toByte(color.r),
                toByte(color.g),
                toByte(color.b));
    }

    private static int toByte(float value) {

        return Math.max(
                0,
                Math.min(255, Math.round(value * 256f)));
    }

    /**
     * Copy coordinates instead of exposing the
     * mutable, pooled legacy Point3DShort.
     */
    private static WorldPosition convertPosition(
            Point3DShort position) {

        if (position == null || position.x == -1) {
            return null;
        }

        return new WorldPosition(
                position.x,
                position.y,
                position.z);
    }
}
