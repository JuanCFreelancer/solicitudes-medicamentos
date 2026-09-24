package com.pruebatecnica.solicitudes.notification;

/**
 * Puerto hacia el servicio de notificaciones. El proceso de negocio depende de esta interfaz y no de
 * HTTP: se puede sustituir por una cola de mensajes sin tocar la lógica de composición.
 */
public interface NotificationGateway {

    /**
     * Contrato: <b>nunca lanza excepción</b>. Cualquier problema con el servicio remoto se traduce en
     * {@link EstadoNotificacion#NO_DISPONIBLE} para que el llamador pueda degradar con elegancia.
     */
    EstadoNotificacion enviar(NotificationCommand command);
}
