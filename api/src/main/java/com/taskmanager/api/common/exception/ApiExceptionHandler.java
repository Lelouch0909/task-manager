package com.taskmanager.api.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    public static ProblemDetail problem(int status, String code, String detail, String path) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        p.setProperty("code", code);
        p.setInstance(URI.create(path));
        return p;
    }
    @ExceptionHandler(ApiException.class)
    ProblemDetail business(ApiException ex, HttpServletRequest request) {
        return problem(ex.status(), ex.code(), ex.getMessage(), request.getRequestURI());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var p = problem(400, "validation_failed", "Certains champs sont invalides.", request.getRequestURI());
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        p.setProperty("errors", fields);
        return p;
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        HandlerMethodValidationException.class, ConstraintViolationException.class})
    ProblemDetail invalid(Exception ex, HttpServletRequest request) {
        return problem(400, "invalid_request", "Paramètres ou corps de requête invalides.", request.getRequestURI());
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(HttpServletRequest request) {
        return problem(409, "data_conflict", "Une donnée existe déjà ou entre en conflit.", request.getRequestURI());
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail missing(HttpServletRequest request) {
        return problem(404, "not_found", "Ressource introuvable.", request.getRequestURI());
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail method(HttpServletRequest request) {
        return problem(405, "method_not_allowed", "Méthode non autorisée.", request.getRequestURI());
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail media(HttpServletRequest request) {
        return problem(415, "unsupported_media_type", "Format de contenu non pris en charge.", request.getRequestURI());
    }
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex, HttpServletRequest request) {
        // Never expose request bodies, credentials, provider errors or database details.
        return problem(500, "internal_error", "Une erreur interne est survenue.", request.getRequestURI());
    }
}
