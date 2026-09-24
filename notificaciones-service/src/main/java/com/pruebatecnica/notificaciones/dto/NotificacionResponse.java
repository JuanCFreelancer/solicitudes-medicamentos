package com.pruebatecnica.notificaciones.dto;

import com.pruebatecnica.notificaciones.entity.CanalNotificacion;
import com.pruebatecnica.notificaciones.entity.EstadoEnvio;
import com.pruebatecnica.notificaciones.entity.Notificacion;
import java.time.Instant;

public record NotificacionResponse(
        Long id,
        String destinatario,
        String asunto,
        String mensaje,
        CanalNotificacion canal,
        EstadoEnvio estado,
        String referencia,
        Instant createdAt) {

    public static NotificacionResponse from(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getDestinatario(),
                notificacion.getAsunto(),
                notificacion.getMensaje(),
                notificacion.getCanal(),
                notificacion.getEstado(),
                notificacion.getReferencia(),
                notificacion.getCreatedAt());
    }
}
