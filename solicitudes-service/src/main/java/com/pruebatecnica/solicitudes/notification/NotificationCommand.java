package com.pruebatecnica.solicitudes.notification;

/**
 * Lo que este servicio necesita pedirle al de notificaciones.
 *
 * @param bearerToken JWT del usuario, reenviado tal cual (propagación de identidad entre servicios)
 */
public record NotificationCommand(String destinatario, String asunto, String mensaje, String referencia,
                                  String bearerToken) {
}
