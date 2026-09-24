package com.pruebatecnica.solicitudes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pruebatecnica.solicitudes.entity.Solicitud;
import com.pruebatecnica.solicitudes.notification.EstadoNotificacion;
import java.time.Instant;

/**
 * @param notificacion resultado de notificar al usuario. Solo aparece al radicar (POST); en los listados se omite.
 *                     Es un campo <b>opcional agregado</b>: los consumidores existentes no se rompen (evolución
 *                     compatible del contrato).
 */
public record SolicitudResponse(
        Long id,
        MedicamentoResponse medicamento,
        String numeroOrden,
        String direccion,
        String telefono,
        String correoContacto,
        Instant createdAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) EstadoNotificacion notificacion) {

    public static SolicitudResponse from(Solicitud solicitud) {
        return new SolicitudResponse(
                solicitud.getId(),
                MedicamentoResponse.from(solicitud.getMedicamento()),
                solicitud.getNumeroOrden(),
                solicitud.getDireccion(),
                solicitud.getTelefono(),
                solicitud.getCorreoContacto(),
                solicitud.getCreatedAt(),
                null);
    }

    public SolicitudResponse conNotificacion(EstadoNotificacion estado) {
        return new SolicitudResponse(id, medicamento, numeroOrden, direccion, telefono, correoContacto, createdAt, estado);
    }
}
