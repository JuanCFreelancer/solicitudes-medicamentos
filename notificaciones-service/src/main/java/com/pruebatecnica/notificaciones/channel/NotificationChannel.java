package com.pruebatecnica.notificaciones.channel;

/**
 * Puerto de salida hacia el sistema que realmente entrega el mensaje (SMTP, SMS, push...).
 * Actúa como adaptador (patrón puerto/adaptador): el servicio de negocio no conoce el sistema
 * concreto, así que integrar uno real es escribir otra implementación sin tocar el resto.
 */
public interface NotificationChannel {

    /** @throws NotificationDeliveryException si el mensaje no pudo entregarse */
    void send(NotificationMessage message);
}
