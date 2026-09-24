package com.pruebatecnica.notificaciones.channel;

/** El canal no pudo entregar el mensaje (servidor SMTP caído, destinatario rechazado, etc.). */
public class NotificationDeliveryException extends RuntimeException {

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
