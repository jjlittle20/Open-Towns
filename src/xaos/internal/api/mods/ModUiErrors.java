
package xaos.internal.api.mods;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores mod-related errors for the in-game UI.
 *
 * Safe to report to from any thread.
 * UI components must read the snapshot on the game thread.
 */
public final class ModUiErrors {

    public record Entry(
            long id,
            String modId,
            String stage,
            String message,
            String details) {
    }

    private static final int MAX_ENTRIES = 100;
    private static final int MAX_DETAILS = 12000;

    private static final List<Entry> entries = new ArrayList<>();

    private static long nextId;

    private ModUiErrors() {
    }

    public static synchronized void report(
            String modId,
            String stage,
            String message,
            Throwable cause) {

        String details = "";

        if (cause != null) {
            StringWriter writer = new StringWriter();
            cause.printStackTrace(new PrintWriter(writer));
            details = writer.toString();

            if (details.length() > MAX_DETAILS) {
                details = details.substring(0, MAX_DETAILS)
                        + "\n[Stack trace truncated]";
            }
        }

        entries.add(new Entry(
                ++nextId,
                modId == null ? "unknown" : modId,
                stage == null ? "Mod error" : stage,
                message == null ? "Unknown error" : message,
                details));

        if (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
    }

    public static void report(
            String modId,
            String stage,
            String message) {

        report(modId, stage, message, null);
    }

    public static synchronized List<Entry> snapshot() {
        return List.copyOf(entries);
    }

    public static synchronized void clear() {
        entries.clear();
    }
}
