package xaos.api.definitions;

abstract class AbstractDefinition implements Definition {

    private final DefinitionId id;
    private final String legacyId;
    private final String name;

    AbstractDefinition(String legacyId, String name) {
        this.id = DefinitionId.towns(legacyId);
        this.legacyId = legacyId;
        this.name = name;
    }

    @Override
    public final DefinitionId id() {
        return id;
    }

    @Override
    public final String legacyId() {
        return legacyId;
    }

    @Override
    public final String name() {
        return name;
    }
}
