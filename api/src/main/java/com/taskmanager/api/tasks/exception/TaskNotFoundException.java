package com.taskmanager.api.tasks.exception;
import com.taskmanager.api.common.exception.ApiException;
public class TaskNotFoundException extends ApiException {
    public TaskNotFoundException() { super(404, "task_not_found", "Tâche introuvable."); }
}
