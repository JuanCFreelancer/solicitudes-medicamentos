package com.pruebatecnica.solicitudes.notification;

/** Resultado de intentar notificar al usuario. Forma parte del contrato de la respuesta al radicar. */
public enum EstadoNotificacion {
    /** El servicio de notificaciones entregó el mensaje. */
    ENVIADA,
    /** El servicio respondió, pero no pudo entregar el mensaje. */
    FALLIDA,
    /** No se pudo contactar al servicio de notificaciones (caído, lento o con error). */
    NO_DISPONIBLE
}
