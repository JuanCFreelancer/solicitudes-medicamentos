package com.pruebatecnica.notificaciones.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Canal de correo simulado: registra el envío en el log en lugar de contactar un servidor SMTP.
 * Se reemplaza por una implementación real (p. ej. JavaMailSender) sin modificar el servicio.
 */
@Component
public class SimulatedEmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SimulatedEmailChannel.class);

    @Override
    public void send(NotificationMessage message) {
        log.info("[correo simulado] para={} asunto='{}'", maskEmail(message.destinatario()), message.asunto());
    }

    /** Enmascara el correo para no dejar datos personales completos en los logs: ana@correo.com -> a**@correo.com */
    static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.charAt(0) + "*".repeat(at - 1) + email.substring(at);
    }
}
