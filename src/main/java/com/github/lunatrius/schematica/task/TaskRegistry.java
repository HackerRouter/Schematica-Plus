package com.github.lunatrius.schematica.task;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TaskRegistry {
    public static final TaskRegistry INSTANCE = new TaskRegistry();

    public enum Kind {
        SAVE("litematica.gui.label.task_name.save_schematic"),
        PASTE("litematica.gui.label.task_name.paste"),
        FILL("litematica.gui.label.task_name.fill"),
        DELETE("litematica.gui.label.task_name.delete"),
        REPLACE("litematica.tool_mode.name.replace_block"),
        MOVE("litematica.tool_mode.name.move"),
        VERIFIER("litematica.gui.label.task_name.verifier"),
        REBUILD("litematica.tool_mode.name.rebuild");

        public final String key;
        Kind(String key) { this.key = key; }

        /** The upstream task completion message for world edits, or null when upstream shows none. */
        public String finishedKey(boolean success) {
            switch (this) {
                case PASTE: return success ? "litematica.message.schematic_pasted" : "litematica.message.error.schematic_paste_failed";
                case FILL: case REPLACE: return success ? "litematica.message.area_filled" : "litematica.message.area_fill_fail";
                case DELETE: return success ? "litematica.message.area_cleared" : "litematica.message.area_clear_fail";
                default: return null;
            }
        }
    }

    public enum Backend {
        CLIENT("schematica.ui.task.backend.client"),
        SERVER("schematica.ui.task.backend.server"),
        COMMANDS("schematica.ui.task.backend.commands"),
        ANALYSIS("schematica.ui.task.backend.analysis"),
        MEMORY("schematica.ui.task.backend.memory");

        public final String key;
        Backend(String key) { this.key = key; }
    }

    public enum Stage {
        QUEUED("schematica.ui.task.stage.queued"),
        CAPTURE("schematica.ui.task.stage.capture"),
        WRITE("schematica.ui.task.stage.write"),
        STRUCTURE("schematica.ui.task.stage.structure"),
        DECORATIONS("schematica.ui.task.stage.decorations"),
        UPDATES("schematica.ui.task.stage.updates"),
        ENTITIES("schematica.ui.task.stage.entities"),
        COMMANDS("schematica.ui.task.stage.commands"),
        VERIFY("schematica.ui.task.stage.verify"),
        EDIT("schematica.ui.task.stage.edit"),
        UPLOAD("schematica.ui.task.stage.upload");

        public final String key;
        Stage(String key) { this.key = key; }
    }

    public static final class Progress {
        public final Stage stage;
        public final long completed, total, affected, entities;
        public final boolean cancelling, cancellable, finished;

        private Progress(Stage stage, long completed, long total, long affected, long entities,
            boolean cancelling, boolean cancellable, boolean finished) {
            this.stage = stage;
            this.total = Math.max(0, total);
            this.completed = Math.max(0, Math.min(completed, this.total));
            this.affected = affected;
            this.entities = entities;
            this.cancelling = cancelling;
            this.cancellable = cancellable;
            this.finished = finished;
        }
    }

    public final class Task {
        public final UUID owner;
        public final int dimension;
        public final Kind kind;
        public final Backend backend;
        public final String detail;
        private volatile Progress progress = new Progress(Stage.QUEUED, 0, 0, 0, 0, false, true, false);

        private Task(UUID owner, int dimension, Kind kind, Backend backend, String detail) {
            this.owner = owner;
            this.dimension = dimension;
            this.kind = kind;
            this.backend = backend;
            this.detail = detail;
        }

        public Progress progress() { return progress; }

        public synchronized boolean cancel(UUID player) {
            Progress old = progress;
            if (!owner.equals(player) || !old.cancellable) return false;
            progress = new Progress(old.stage, old.completed, old.total, old.affected, old.entities, true, false, false);
            return true;
        }

        public synchronized void update(Stage stage, long completed, long total, long affected, long entities) {
            Progress old = progress;
            if (!old.finished) progress = new Progress(stage, completed, total, affected, entities,
                old.cancelling, old.cancellable, false);
        }

        public synchronized boolean beginWrite() {
            Progress old = progress;
            if (old.finished || old.cancelling || !old.cancellable) return false;
            progress = new Progress(Stage.WRITE, 0, 0, old.affected, old.entities, false, false, false);
            return true;
        }

        public synchronized void finish() {
            Progress old = progress;
            progress = new Progress(old.stage, old.completed, old.total, old.affected, old.entities, old.cancelling, false, true);
            remove(this);
        }
    }

    private final List<Task> tasks = new ArrayList<>();

    public synchronized Task start(UUID owner, int dimension, Kind kind, Backend backend, String detail) {
        Task task = new Task(owner, dimension, kind, backend, detail);
        tasks.add(task);
        return task;
    }

    public synchronized List<Task> tasks(UUID owner, int dimension) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) if (task.owner.equals(owner) && task.dimension == dimension) result.add(task);
        return result;
    }

    private synchronized void remove(Task task) { tasks.remove(task); }
}
