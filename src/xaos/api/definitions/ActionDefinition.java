package xaos.api.definitions;

/** Read-only snapshot of an action definition. */
public final class ActionDefinition extends AbstractDefinition {

    private final String priorityId;
    private final int turns;
    private final String effect;
    private final String generatedItem;
    private final boolean killsSource;
    private final boolean inverted;

    public ActionDefinition(
            String legacyId,
            String name,
            String priorityId,
            int turns,
            String effect,
            String generatedItem,
            boolean killsSource,
            boolean inverted) {
        super(legacyId, name);
        this.priorityId = priorityId;
        this.turns = turns;
        this.effect = effect;
        this.generatedItem = generatedItem;
        this.killsSource = killsSource;
        this.inverted = inverted;
    }

    public String priorityId() { return priorityId; }
    public int turns() { return turns; }
    public String effect() { return effect; }
    public String generatedItem() { return generatedItem; }
    public boolean killsSource() { return killsSource; }
    public boolean inverted() { return inverted; }
}
