package com.pruebatecnica.solicitudes.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Adaptador HTTP hacia notificaciones-service (POST /notificaciones). */
@Component
public class HttpNotificationGateway implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpNotificationGateway.class);

    private final RestClient restClient;

    public HttpNotificationGateway(RestClient notificationsRestClient) {
        this.restClient = notificationsRestClient;
    }

    @Override
    public EstadoNotificacion enviar(NotificationCommand command) {
        try {
            Reply reply = restClient.post()
                    .uri("/notificaciones")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + command.bearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Body(command.destinatario(), command.asunto(), command.mensaje(), command.referencia()))
                    .retrieve()
                    .body(Reply.class);
            return reply != null && "ENVIADA".equals(reply.estado()) ? EstadoNotificacion.ENVIADA : EstadoNotificacion.FALLIDA;
        } catch (RestClientException e) {
            // Timeout, conexión rechazada, 4xx/5xx...: la solicitud ya está creada, no se propaga el fallo.
            log.warn("No se pudo contactar al servicio de notificaciones referencia={}: {}",
                    command.referencia(), e.getMessage());
            return EstadoNotificacion.NO_DISPONIBLE;
        }
    }

    private record Body(String destinatario, String asunto, String mensaje, String referencia) {
    }

    /** Lector tolerante: solo lee lo que necesita e ignora campos nuevos que el proveedor agregue al contrato. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Reply(String estado) {
    }
}
