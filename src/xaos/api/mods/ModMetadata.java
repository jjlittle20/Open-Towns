package xaos.api.mods;

/**
 * Basic descriptive information about a loaded mod.
 */
public record ModMetadata(
        String id,
        String name,
        String version,
        String author
) {

    public ModMetadata {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Mod ID cannot be empty"
            );
        }

        if (name == null || name.isBlank()) {
            name = id;
        }

        if (version == null || version.isBlank()) {
            version = "unknown";
        }

        if (author == null || author.isBlank()) {
            author = "unknown";
        }
    }
}