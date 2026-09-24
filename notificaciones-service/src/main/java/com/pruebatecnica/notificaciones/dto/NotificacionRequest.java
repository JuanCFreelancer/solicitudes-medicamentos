package com.pruebatecnica.notificaciones.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contrato de entrada del servicio. Es deliberadamente genérico (destinatario + asunto + mensaje):
 * cualquier proceso de negocio puede reutilizarlo sin que este servicio conozca solicitudes ni medicamentos.
 */
public record NotificacionRequest(
        @NotBlank(message = "El destinatario es obligatorio")
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "El destinatario debe ser un correo válido")
        @Size(max = 254, message = "El destinatario no puede superar 254 caracteres")
        String destinatario,

        @NotBlank(message = "El asunto es obligatorio")
        @Size(max = 150, message = "El asunto no puede superar 150 caracteres")
        String asunto,

        @NotBlank(message = "El mensaje es obligatorio")
        @Size(max = 1000, message = "El mensaje no puede superar 1000 caracteres")
        String mensaje,

        @Size(max = 100, message = "La referencia no puede superar 100 caracteres")
        String referencia) {
}
