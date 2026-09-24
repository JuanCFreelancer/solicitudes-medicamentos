package com.pruebatecnica.solicitudes.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Verifica el contrato HTTP con el servicio remoto y la degradación ante cada tipo de fallo. */
class HttpNotificationGatewayTest {

    private static final NotificationCommand COMMAND = new NotificationCommand(
            "ana@correo.com", "Solicitud #12 registrada", "Hola Ana", "solicitud:12", "jwt-de-ana");

    private MockRestServiceServer server;
    private HttpNotificationGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://notificaciones");
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new HttpNotificationGateway(builder.build());
    }

    @Test
    void enviaElContratoEsperadoYReenviaElToken() {
        server.expect(requestTo("http://notificaciones/notificaciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer jwt-de-ana"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.destinatario").value("ana@correo.com"))
                .andExpect(jsonPath("$.asunto").value("Solicitud #12 registrada"))
                .andExpect(jsonPath("$.mensaje").value("Hola Ana"))
                .andExpect(jsonPath("$.referencia").value("solicitud:12"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":5,\"estado\":\"ENVIADA\"}"));

        assertThat(gateway.enviar(COMMAND)).isEqualTo(EstadoNotificacion.ENVIADA);
        server.verify();
    }

    @Test
    void toleraCamposNuevosEnLaRespuestaDelProveedor() {
        server.expect(requestTo("http://notificaciones/notificaciones"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":5,\"estado\":\"ENVIADA\",\"campoNuevoDeLaV2\":{\"x\":1}}"));

        assertThat(gateway.enviar(COMMAND)).isEqualTo(EstadoNotificacion.ENVIADA);
    }

    @Test
    void siElServicioRespondePeroNoPudoEntregar_devuelveFallida() {
        server.expect(requestTo("http://notificaciones/notificaciones"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":6,\"estado\":\"FALLIDA\"}"));

        assertThat(gateway.enviar(COMMAND)).isEqualTo(EstadoNotificacion.FALLIDA);
    }

    @Test
    void ante500_devuelveNoDisponibleSinLanzarExcepcion() {
        server.expect(requestTo("http://notificaciones/notificaciones")).andRespond(withServerError());

        assertThat(gateway.enviar(COMMAND)).isEqualTo(EstadoNotificacion.NO_DISPONIBLE);
    }

    @Test
    void ante401_devuelveNoDisponible() {
        server.expect(requestTo("http://notificaciones/notificaciones")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThat(gateway.enviar(COMMAND)).isEqualTo(EstadoNotificacion.NO_DISPONIBLE);
    }

    @Test
    void anteErrorDeConexion_devuelveNoDisponible() {
        server.expect(requestTo("http://notificaciones/notificaciones"))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        assertThat(gateway.enviar(COMMAND)).isEqualTo(EstadoNotificacion.NO_DISPONIBLE);
    }
}
