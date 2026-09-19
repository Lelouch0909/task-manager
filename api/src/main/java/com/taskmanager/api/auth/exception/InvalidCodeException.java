package com.taskmanager.api.auth.exception;
import com.taskmanager.api.common.exception.ApiException;
public class InvalidCodeException extends ApiException {
    public InvalidCodeException() {
        super(400, "invalid_code", "Code invalide, expiré, déjà utilisé ou nombre d’essais dépassé.");
    }
}
