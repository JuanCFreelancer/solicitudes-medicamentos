package com.pruebatecnica.solicitudes.notification;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Integración con notificaciones-service (prefijo {@code app.notifications}).
 *
 * @param enabled si es false, el servicio de solicitudes funciona de forma autónoma y no notifica
 * @param timeout tiempo máximo de conexión y de lectura: una dependencia lenta no debe volver lenta a esta API
 */
@Validated
@ConfigurationProperties(prefix = "app.notifications")
public record NotificationsProperties(boolean enabled, @NotBlank String baseUrl, Duration timeout) {

    public NotificationsProperties {
        timeout = timeout == null ? Duration.ofSeconds(2) : timeout;
    }
}
