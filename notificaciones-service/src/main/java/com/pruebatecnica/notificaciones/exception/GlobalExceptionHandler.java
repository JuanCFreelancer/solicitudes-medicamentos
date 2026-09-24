package com.pruebatecnica.notificaciones.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce cualquier error a una respuesta {@link ProblemDetail} (RFC 7807).
 * Los errores de validación incluyen la propiedad {@code errors} con un mensaje por campo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError -> errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
    }

    @ExceptionHandler(RequestValidationException.class)
    ProblemDetail handleRequestValidation(RequestValidationException ex) {
        return validationProblem(ex.getErrors());
    }

    /** Último recurso: se registra el detalle en el log y al cliente solo llega un mensaje genérico. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta nuevamente más tarde.");
        problem.setTitle("Error interno");
        return problem;
    }

    private static ProblemDetail validationProblem(Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos");
        problem.setTitle("Error de validación");
        problem.setProperty("errors", errors);
        return problem;
    }
}
