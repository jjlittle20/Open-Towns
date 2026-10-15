package xaos.api.tasks;

import xaos.api.definitions.DefinitionId;
import xaos.api.world.WorldPosition;

public interface TaskApi {

    TaskResult dig(
            WorldPosition position);

    TaskResult destroy(
            WorldPosition position);

    TaskResult build(
            DefinitionId buildingId,
            WorldPosition position);

    TaskResult mine(
            WorldPosition position);

    TaskResult mineArea(
            WorldPosition first,
            WorldPosition second);
}