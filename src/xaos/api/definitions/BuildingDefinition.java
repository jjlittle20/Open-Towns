package xaos.api.definitions;

import java.util.List;

/** Read-only snapshot of a building definition. */
public final class BuildingDefinition extends AbstractDefinition {

    private final String type;
    private final short width;
    private final short height;
    private final boolean automatic;
    private final List<String> descriptions;

    BuildingDefinition(
            String legacyId,
            String name,
            String type,
            short width,
            short height,
            boolean automatic,
            List<String> descriptions) {
        super(legacyId, name);
        this.type = type;
        this.width = width;
        this.height = height;
        this.automatic = automatic;
        this.descriptions = descriptions == null ? List.of() : List.copyOf(descriptions);
    }

    public String type() { return type; }
    public short width() { return width; }
    public short height() { return height; }
    public boolean automatic() { return automatic; }
    public List<String> descriptions() { return descriptions; }
}
