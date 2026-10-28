package xaos.api.buildings;

import xaos.api.definitions.DefinitionId;

/**
 * Handle returned when registering custom building behaviour.
 */
public interface BuildingBehaviourRegistration {

    /**
     * Removes this behaviour registration.
     */
    void unregister();

    /**
     * Whether this registration is still active.
     */
    boolean isActive();

    /**
     * Building definition this behaviour targets.
     */
    DefinitionId definitionId();

    /**
     * Number of simulation ticks between executions.
     */
    int intervalTicks();
}