package com.taskmanager.api.tasks;

import java.util.UUID;

/** Public domain event, published inside the task transaction. */
public record TaskChanged(UUID ownerId, UUID taskId, String title, Kind kind) {
    public enum Kind { CREATED, UPDATED, STATUS_CHANGED, DELETED }
}
