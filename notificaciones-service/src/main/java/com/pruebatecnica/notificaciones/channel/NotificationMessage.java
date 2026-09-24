package com.pruebatecnica.notificaciones.channel;

/** Mensaje listo para entregar, independiente del medio concreto. */
public record NotificationMessage(String destinatario, String asunto, String mensaje) {
}
