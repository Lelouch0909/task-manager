package com.taskmanager.api.auth.exception;
import com.taskmanager.api.common.exception.ApiException;
public class InvalidSessionException extends ApiException {
    public InvalidSessionException() {
        super(401, "invalid_session", "Session invalide ou expirée. Reconnectez-vous.");
    }
}
