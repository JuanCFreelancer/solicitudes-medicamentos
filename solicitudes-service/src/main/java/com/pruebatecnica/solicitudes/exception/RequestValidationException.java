package com.pruebatecnica.solicitudes.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Error de validación de reglas de negocio; transporta el mensaje por campo (respetando el orden). */
public class RequestValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public RequestValidationException(Map<String, String> errors) {
        super("La solicitud contiene datos inválidos");
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
