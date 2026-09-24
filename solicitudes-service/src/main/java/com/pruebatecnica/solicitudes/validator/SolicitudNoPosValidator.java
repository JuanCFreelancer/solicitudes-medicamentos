package com.pruebatecnica.solicitudes.validator;

import com.pruebatecnica.solicitudes.dto.SolicitudRequest;
import com.pruebatecnica.solicitudes.entity.Medicamento;
import com.pruebatecnica.solicitudes.exception.RequestValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Regla de negocio: si el medicamento es NO POS, número de orden, dirección, teléfono y correo
 * son obligatorios. Vive aislada para poder probarla sin HTTP ni base de datos.
 * <p>
 * El formato (email, teléfono, longitudes) ya lo validó Bean Validation en {@link SolicitudRequest}.
 */
@Component
public class SolicitudNoPosValidator {

    public void validate(Medicamento medicamento, SolicitudRequest request) {
        if (medicamento.isEsPos()) {
            return;
        }

        Map<String, String> errors = new LinkedHashMap<>();
        requireText(errors, "numeroOrden", request.numeroOrden(), "El número de orden es obligatorio para medicamentos NO POS");
        requireText(errors, "direccion", request.direccion(), "La dirección es obligatoria para medicamentos NO POS");
        requireText(errors, "telefono", request.telefono(), "El teléfono es obligatorio para medicamentos NO POS");
        requireText(errors, "correoContacto", request.correoContacto(), "El correo electrónico es obligatorio para medicamentos NO POS");

        if (!errors.isEmpty()) {
            throw new RequestValidationException(errors);
        }
    }

    private static void requireText(Map<String, String> errors, String field, String value, String message) {
        if (value == null || value.isBlank()) {
            errors.put(field, message);
        }
    }
}
