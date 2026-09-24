package com.pruebatecnica.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pruebatecnica.notificaciones.channel.NotificationChannel;
import com.pruebatecnica.notificaciones.channel.NotificationDeliveryException;
import com.pruebatecnica.notificaciones.channel.NotificationMessage;
import com.pruebatecnica.notificaciones.config.PaginationProperties;
import com.pruebatecnica.notificaciones.dto.NotificacionRequest;
import com.pruebatecnica.notificaciones.dto.NotificacionResponse;
import com.pruebatecnica.notificaciones.dto.PageResponse;
import com.pruebatecnica.notificaciones.entity.CanalNotificacion;
import com.pruebatecnica.notificaciones.entity.EstadoEnvio;
import com.pruebatecnica.notificaciones.entity.Notificacion;
import com.pruebatecnica.notificaciones.repository.NotificacionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    private static final long USUARIO_ID = 7L;
    private static final NotificacionRequest REQUEST =
            new NotificacionRequest("ana@correo.com", "Solicitud #12", "Recibimos tu solicitud", "solicitud:12");

    @Mock
    private NotificacionRepository repository;
    @Mock
    private NotificationChannel channel;

    private NotificacionService service;

    @BeforeEach
    void setUp() {
        service = new NotificacionService(repository, channel, new PageRequestFactory(new PaginationProperties(10, 50)));
    }

    @Test
    void enviar_conCanalDisponible_guardaComoEnviadaConLaReferencia() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificacionResponse response = service.enviar(USUARIO_ID, REQUEST);

        verify(channel).send(new NotificationMessage("ana@correo.com", "Solicitud #12", "Recibimos tu solicitud"));
        ArgumentCaptor<Notificacion> saved = ArgumentCaptor.forClass(Notificacion.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(saved.getValue().getReferencia()).isEqualTo("solicitud:12");
        assertThat(saved.getValue().getCanal()).isEqualTo(CanalNotificacion.EMAIL);
        assertThat(response.estado()).isEqualTo(EstadoEnvio.ENVIADA);
    }

    @Test
    void enviar_siElCanalFalla_noPropagaLaExcepcionYRegistraFallida() {
        doThrow(new NotificationDeliveryException("SMTP caído", new RuntimeException())).when(channel).send(any());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificacionResponse response = service.enviar(USUARIO_ID, REQUEST);

        assertThat(response.estado()).isEqualTo(EstadoEnvio.FALLIDA);
        verify(repository).save(any()); // queda constancia auditable del intento
    }

    @Test
    void listar_filtraPorUsuarioYMapeaLaPagina() {
        Notificacion notificacion = new Notificacion(USUARIO_ID, "ana@correo.com", "Asunto", "Mensaje",
                CanalNotificacion.EMAIL, EstadoEnvio.ENVIADA, "solicitud:1");
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findByUsuarioId(any(), any())).thenReturn(new PageImpl<>(List.of(notificacion), pageable, 1));

        PageResponse<NotificacionResponse> response = service.listar(USUARIO_ID, null, null);

        ArgumentCaptor<Long> user = ArgumentCaptor.forClass(Long.class);
        verify(repository).findByUsuarioId(user.capture(), any());
        assertThat(user.getValue()).isEqualTo(USUARIO_ID);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).extracting(NotificacionResponse::referencia).containsExactly("solicitud:1");
    }
}
