
package xaos.ui.integration;

import java.util.Objects;

/**
 * Describes whether a gameplay UI action
 * can currently be executed.
 *
 * Does not describe task progress or
 * the availability of construction materials.
 */
public record UiActionAvailability(
        Status status,
        String reason) {

    public enum Status {
        AVAILABLE,
        UNAVAILABLE,
        UNKNOWN
    }

    public UiActionAvailability {

        Objects.requireNonNull(status, "status");

        reason = reason == null
                ? ""
                : reason.trim();

        if (status == Status.AVAILABLE
                && !reason.isEmpty()) {

            throw new IllegalArgumentException(
                    "Available actions cannot have "
                            + "a blocking reason");
        }

        if (status != Status.AVAILABLE
                && reason.isEmpty()) {

            throw new IllegalArgumentException(
                    "Unavailable or unknown actions "
                            + "must include a reason");
        }
    }

    public boolean canExecute() {
        return status == Status.AVAILABLE;
    }

    public static UiActionAvailability allowed() {

        return new UiActionAvailability(
                Status.AVAILABLE, "");
    }

    public static UiActionAvailability blocked(
            String reason) {

        return new UiActionAvailability(
                Status.UNAVAILABLE, reason);
    }

    public static UiActionAvailability unknown(
            String reason) {

        return new UiActionAvailability(
                Status.UNKNOWN, reason);
    }
}
