package xaos.api.definitions;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/** Internal adapter from the legacy static managers to read-only API snapshots. */
final class MappedDefinitionRegistry<S, T extends Definition> implements DefinitionRegistry<T> {

    private final Function<String, S> finder;
    private final Supplier<? extends Map<String, S>> allSupplier;
    private final Function<S, T> mapper;

    MappedDefinitionRegistry(
            Function<String, S> finder,
            Supplier<? extends Map<String, S>> allSupplier,
            Function<S, T> mapper) {
        this.finder = finder;
        this.allSupplier = allSupplier;
        this.mapper = mapper;
    }

    @Override
    public T get(String id) {
        DefinitionId definitionId;
        try {
            definitionId = DefinitionId.parse(id);
        } catch (IllegalArgumentException exception) {
            return null;
        }

        // Current XML managers use unqualified IDs. Non-default namespaces will
        // become resolvable when the loader starts tracking definition sources.
        if (!definitionId.isDefaultNamespace()) {
            return null;
        }

        S source = finder.apply(definitionId.path());
        return source == null ? null : mapper.apply(source);
    }

    @Override
    public List<T> all() {
        return allSupplier.get().values().stream()
                .map(mapper)
                .toList();
    }

    @Override
    public Set<DefinitionId> ids() {
        LinkedHashSet<DefinitionId> ids = new LinkedHashSet<>();
        for (String legacyId : allSupplier.get().keySet()) {
            ids.add(DefinitionId.towns(legacyId));
        }
        return Set.copyOf(ids);
    }
}
