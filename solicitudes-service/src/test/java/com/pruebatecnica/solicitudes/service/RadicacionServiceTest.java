package com.pruebatecnica.solicitudes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pruebatecnica.solicitudes.dto.MedicamentoResponse;
import com.pruebatecnica.solicitudes.dto.SolicitudRequest;
import com.pruebatecnica.solicitudes.dto.SolicitudResponse;
import com.pruebatecnica.solicitudes.exception.RequestValidationException;
import com.pruebatecnica.solicitudes.notification.EstadoNotificacion;
import com.pruebatecnica.solicitudes.notification.NotificationCommand;
import com.pruebatecnica.solicitudes.notification.NotificationGateway;
import com.pruebatecnica.solicitudes.notification.NotificationsProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RadicacionServiceTest {

    private static final AuthenticatedUser ANA = new AuthenticatedUser(7L, "Ana", "ana@cuenta.com", "jwt-de-ana");
    private static final SolicitudRequest REQUEST = new SolicitudRequest(1L, null, null, null, null);

    @Mock
    private SolicitudService solicitudService;
    @Mock
    private NotificationGateway gateway;

    private RadicacionService service;

    @BeforeEach
    void setUp() {
        service = new RadicacionService(solicitudService, gateway,
                new NotificationsProperties(true, "http://notificaciones", Duration.ofSeconds(2)));
    }

    private static SolicitudResponse solicitud(long id, boolean esPos, String correoContacto) {
        return new SolicitudResponse(id, new MedicamentoResponse(esPos ? 1L : 2L, esPos ? "Acetaminofén" : "Adalimumab", esPos),
                null, null, null, correoContacto, Instant.parse("2026-01-01T10:00:00Z"), null);
    }

    @Test
    void medicamentoPos_notificaAlCorreoDeLaCuentaYReenviaElToken() {
        when(solicitudService.crear(7L, REQUEST)).thenReturn(solicitud(12, true, null));
        when(gateway.enviar(any())).thenReturn(EstadoNotificacion.ENVIADA);

        SolicitudResponse response = service.radicar(ANA, REQUEST);

        ArgumentCaptor<NotificationCommand> command = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(gateway).enviar(command.capture());
        assertThat(command.getValue().destinatario()).isEqualTo("ana@cuenta.com");
        assertThat(command.getValue().referencia()).isEqualTo("solicitud:12");
        assertThat(command.getValue().asunto()).isEqualTo("Solicitud #12 registrada");
        assertThat(command.getValue().mensaje()).contains("Hola Ana").contains("#12").contains("Acetaminofén");
        assertThat(command.getValue().bearerToken()).isEqualTo("jwt-de-ana");
        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.notificacion()).isEqualTo(EstadoNotificacion.ENVIADA);
    }

    @Test
    void medicamentoNoPos_notificaAlCorreoDeContactoDeLaSolicitud() {
        when(solicitudService.crear(7L, REQUEST)).thenReturn(solicitud(13, false, "contacto@paciente.com"));
        when(gateway.enviar(any())).thenReturn(EstadoNotificacion.ENVIADA);

        service.radicar(ANA, REQUEST);

        ArgumentCaptor<NotificationCommand> command = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(gateway).enviar(command.capture());
        assertThat(command.getValue().destinatario()).isEqualTo("contacto@paciente.com");
    }

    @Test
    void siNotificarFalla_laSolicitudSeConservaYLaRespuestaLoInforma() {
        when(solicitudService.crear(7L, REQUEST)).thenReturn(solicitud(14, true, null));
        when(gateway.enviar(any())).thenReturn(EstadoNotificacion.NO_DISPONIBLE);

        SolicitudResponse response = service.radicar(ANA, REQUEST);

        assertThat(response.id()).isEqualTo(14L);                                        // la solicitud existe
        assertThat(response.notificacion()).isEqualTo(EstadoNotificacion.NO_DISPONIBLE); // y se informa el problema
    }

    @Test
    void siLaSolicitudEsInvalida_noSeNotificaNada() {
        when(solicitudService.crear(7L, REQUEST)).thenThrow(new RequestValidationException(Map.of("numeroOrden", "obligatorio")));

        assertThatThrownBy(() -> service.radicar(ANA, REQUEST)).isInstanceOf(RequestValidationException.class);
        verifyNoInteractions(gateway);
    }

    @Test
    void conLaIntegracionDeshabilitada_noContactaAlServicioYNoInformaEstado() {
        service = new RadicacionService(solicitudService, gateway,
                new NotificationsProperties(false, "http://notificaciones", Duration.ofSeconds(2)));
        when(solicitudService.crear(7L, REQUEST)).thenReturn(solicitud(15, true, null));

        SolicitudResponse response = service.radicar(ANA, REQUEST);

        verifyNoInteractions(gateway);
        assertThat(response.notificacion()).isNull();
    }

    @Test
    void sinCorreoDestinatario_omiteLaNotificacion() {
        AuthenticatedUser sinCorreo = new AuthenticatedUser(7L, "Ana", null, "jwt");
        when(solicitudService.crear(7L, REQUEST)).thenReturn(solicitud(16, true, null));

        SolicitudResponse response = service.radicar(sinCorreo, REQUEST);

        verify(gateway, never()).enviar(any());
        assertThat(response.notificacion()).isNull();
    }
}
