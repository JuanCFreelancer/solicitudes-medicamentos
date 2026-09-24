package com.pruebatecnica.solicitudes.dto;

import com.pruebatecnica.solicitudes.entity.Solicitud;
import java.time.Instant;

public record SolicitudResponse(
        Long id,
        MedicamentoResponse medicamento,
        String numeroOrden,
        String direccion,
        String telefono,
        String correoContacto,
        Instant createdAt) {

    public static SolicitudResponse from(Solicitud solicitud) {
        return new SolicitudResponse(
                solicitud.getId(),
                MedicamentoResponse.from(solicitud.getMedicamento()),
                solicitud.getNumeroOrden(),
                solicitud.getDireccion(),
                solicitud.getTelefono(),
                solicitud.getCorreoContacto(),
                solicitud.getCreatedAt());
    }
}
