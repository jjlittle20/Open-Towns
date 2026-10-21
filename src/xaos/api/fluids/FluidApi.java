package xaos.api.fluids;

import xaos.api.definitions.DefinitionId;
import xaos.api.world.WorldPosition;

public interface FluidApi {

    FluidView at(
            WorldPosition position);

    FluidMutationResult fill(
            WorldPosition position,
            DefinitionId fluidId);

    FluidMutationResult remove(
            WorldPosition position);
}