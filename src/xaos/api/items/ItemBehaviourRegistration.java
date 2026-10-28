package xaos.api.items;

import xaos.api.definitions.DefinitionId;

/**
 * Handle returned when registering custom item behaviour.
 */
public interface ItemBehaviourRegistration {

    /**
     * Removes this behaviour registration.
     */
    void unregister();

    /**
     * Whether this registration is still active.
     */
    boolean isActive();

    /**
     * Item definition this behaviour targets.
     */
    DefinitionId definitionId();

    /**
     * Number of simulation ticks between executions.
     */
    int intervalTicks();
}
