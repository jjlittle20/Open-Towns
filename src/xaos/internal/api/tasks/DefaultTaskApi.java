package xaos.internal.api.tasks;

import xaos.api.GameApi;
import xaos.api.definitions.DefinitionId;
import xaos.api.tasks.TaskApi;
import xaos.api.tasks.TaskResult;
import xaos.api.world.WorldPosition;
import xaos.main.Game;
import xaos.tasks.Task;
import xaos.tasks.TaskTypes;
import xaos.utils.Point3D;

public final class DefaultTaskApi implements TaskApi {

    @Override
    public TaskResult dig(WorldPosition position) {
        if (position == null) {
            return TaskResult.rejected(
                    "Position cannot be null");
        }

        if (!GameApi.get()
                .world()
                .isValidPosition(position)) {

            return TaskResult.rejected(
                    "Position is outside the world");
        }

        Task task = new Task(TaskTypes.TASK_DIG);

        task.setPointIni(
                new Point3D(
                        position.x(),
                        position.y(),
                        position.z()));

        /*
         * This is the same preparation used by the normal
         * Task.finishCreation() pathway.
         *
         * It creates the task's hot points and applies the
         * normal Towns validation rules.
         */
        task.prepareForQueue();

        /*
         * No hot points means Towns found nothing valid to dig.
         *
         * Avoid adding a dead/finished task to TaskManager.
         */
        if (task.getHotPoints().isEmpty()) {
            return TaskResult.rejected(
                    "No valid dig target at this position");
        }

        /*
         * Match normal God Mode behaviour.
         */
        if (Game.isGodMode()
                && task.canExecuteImmediately()
                && task.executeImmediately()) {

            return TaskResult.executed();
        }

        /*
         * Normal game behaviour.
         *
         * Game.taskCreated() also performs the existing
         * DIG -> MINE conversion before adding the task
         * to TaskManager.
         */
        Game.taskCreated(task);

        return TaskResult.queued();
    }

    @Override
    public TaskResult destroy(WorldPosition position) {
        return TaskResult.rejected(
                "Destroy task API is not implemented yet");
    }

    @Override
    public TaskResult build(
            DefinitionId buildingId,
            WorldPosition position) {
        return TaskResult.rejected(
                "Build task API is not implemented yet");
    }

    @Override
    public TaskResult mine(WorldPosition position) {
        return mineArea(position, position);
    }

    @Override
    public TaskResult mineArea(
            WorldPosition first,
            WorldPosition second) {
        if (first == null || second == null) {
            return TaskResult.rejected(
                    "Mining positions cannot be null");
        }

        if (!GameApi.get()
                .world()
                .isValidPosition(first)
                || !GameApi.get()
                        .world()
                        .isValidPosition(second)) {

            return TaskResult.rejected(
                    "Mining area is outside the world");
        }

        /*
         * Towns mining areas operate on one Z level.
         */
        if (first.z() != second.z()) {
            return TaskResult.rejected(
                    "Mining area must be on one Z level");
        }

        Task task = new Task(TaskTypes.TASK_MINE);

        task.setPointIni(
                new Point3D(
                        first.x(),
                        first.y(),
                        first.z()));

        task.setPointEnd(
                new Point3D(
                        second.x(),
                        second.y(),
                        second.z()));

        task.prepareForQueue();

        if (task.getHotPoints().isEmpty()) {
            return TaskResult.rejected(
                    "No valid mining targets in this area");
        }

        if (Game.isGodMode()
                && task.canExecuteImmediately()
                && task.executeImmediately()) {

            return TaskResult.executed();
        }

        Game.taskCreated(task);

        return TaskResult.queued();
    }
}