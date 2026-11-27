
package xaos.ui.integration;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

import xaos.ui.rendering.UiColor;

/**
 * UI-independent message log model.
 *
 * Stores immutable snapshots of messages from
 * the game's existing message system.
 *
 * Does not depend on Game, OpenGL or UIPanel.
 */
public final class UiMessageLogModel {

    public enum Channel {

        ANNOUNCEMENTS(0),
        COMBAT(1),
        HEROES(2),
        SYSTEM(3);

        private final int legacyType;

        Channel(int legacyType) {
            this.legacyType = legacyType;
        }

        public int legacyType() {
            return legacyType;
        }

        public static Channel fromLegacyType(int type) {

            for (Channel channel : values()) {
                if (channel.legacyType == type) {
                    return channel;
                }
            }

            throw new IllegalArgumentException(
                    "Unknown message type: " + type);
        }
    }

    /**
     * Immutable world position.
     *
     * The original MessagesPanel stores this as
     * a mutable Point3DShort.
     */
    public record WorldPosition(
            int x,
            int y,
            int z) {
    }

    /**
     * One message ready for display.
     *
     * entityId is -1 when no entity is attached.
     * position may be null.
     */
    public record Entry(
            String text,
            UiColor color,
            WorldPosition position,
            int entityId) {

        public Entry {
            Objects.requireNonNull(text);
            Objects.requireNonNull(color);
        }

        public boolean hasNavigationTarget() {

            return entityId >= 0 || position != null;
        }
    }

    private final EnumMap<Channel, List<Entry>> messages = new EnumMap<>(Channel.class);

    private Channel selectedChannel = Channel.ANNOUNCEMENTS;

    public UiMessageLogModel() {

        for (Channel channel : Channel.values()) {
            messages.put(channel, List.of());
        }
    }

    public Channel selectedChannel() {
        return selectedChannel;
    }

    public void selectChannel(Channel channel) {

        selectedChannel = Objects.requireNonNull(channel);
    }

    /**
     * Replace the displayed snapshot for a channel.
     *
     * Entries are ordered oldest to newest,
     * matching the original message history.
     */
    public void replace(
            Channel channel,
            List<Entry> entries) {

        Objects.requireNonNull(channel);
        Objects.requireNonNull(entries);

        messages.put(channel, List.copyOf(entries));
    }

    public List<Entry> messages(Channel channel) {

        return messages.get(
                Objects.requireNonNull(channel));
    }

    public List<Entry> visibleMessages() {

        return messages(selectedChannel);
    }
}
