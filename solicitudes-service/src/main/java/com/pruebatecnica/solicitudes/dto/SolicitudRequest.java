package com.pruebatecnica.solicitudes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo para crear una solicitud.
 * <p>
 * Aquí solo se validan tipo y formato. La obligatoriedad de los cuatro datos adicionales depende
 * del medicamento (NO POS) y por eso la resuelve {@code SolicitudNoPosValidator}.
 * <p>
 * El constructor compacto normaliza la entrada: textos vacíos pasan a {@code null} y el teléfono
 * se limpia de espacios, guiones y paréntesis.
 */
public record SolicitudRequest(
        @NotNull(message = "El medicamento es obligatorio")
        @Positive(message = "El identificador del medicamento no es válido")
        Long medicamentoId,

        @Size(max = 50, message = "El número de orden no puede superar 50 caracteres")
        String numeroOrden,

        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccion,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                message = "El teléfono debe tener entre 7 y 15 dígitos (puede iniciar con +)")
        String telefono,

        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "El correo electrónico no tiene un formato válido")
        @Size(max = 254, message = "El correo no puede superar 254 caracteres")
        String correoContacto) {

    public SolicitudRequest {
        numeroOrden = blankToNull(numeroOrden);
        direccion = blankToNull(direccion);
        telefono = blankToNull(telefono) == null ? null : telefono.replaceAll("[\\s()-]", "");
        correoContacto = blankToNull(correoContacto);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
