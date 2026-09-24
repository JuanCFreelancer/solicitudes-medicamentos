package com.pruebatecnica.notificaciones.service;

import com.pruebatecnica.notificaciones.channel.NotificationChannel;
import com.pruebatecnica.notificaciones.channel.NotificationDeliveryException;
import com.pruebatecnica.notificaciones.channel.NotificationMessage;
import com.pruebatecnica.notificaciones.dto.NotificacionRequest;
import com.pruebatecnica.notificaciones.dto.NotificacionResponse;
import com.pruebatecnica.notificaciones.dto.PageResponse;
import com.pruebatecnica.notificaciones.entity.CanalNotificacion;
import com.pruebatecnica.notificaciones.entity.EstadoEnvio;
import com.pruebatecnica.notificaciones.entity.Notificacion;
import com.pruebatecnica.notificaciones.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository repository;
    private final NotificationChannel channel;
    private final PageRequestFactory pageRequestFactory;

    public NotificacionService(NotificacionRepository repository, NotificationChannel channel,
                               PageRequestFactory pageRequestFactory) {
        this.repository = repository;
        this.channel = channel;
        this.pageRequestFactory = pageRequestFactory;
    }

    /**
     * Intenta la entrega y deja constancia del resultado. Un fallo del canal NO es una excepción para el
     * llamador: se registra como FALLIDA (auditable) y la respuesta lo informa.
     * <p>
     * Deliberadamente sin {@code @Transactional}: la llamada al canal es E/S externa y no debe mantener
     * abierta una transacción de base de datos; {@code save} ya es transaccional por sí mismo.
     */
    public NotificacionResponse enviar(Long usuarioId, NotificacionRequest request) {
        EstadoEnvio estado;
        try {
            channel.send(new NotificationMessage(request.destinatario(), request.asunto(), request.mensaje()));
            estado = EstadoEnvio.ENVIADA;
        } catch (NotificationDeliveryException e) {
            log.warn("Falló la entrega de la notificación referencia={}", request.referencia(), e);
            estado = EstadoEnvio.FALLIDA;
        }

        Notificacion guardada = repository.save(new Notificacion(usuarioId, request.destinatario(),
                request.asunto(), request.mensaje(), CanalNotificacion.EMAIL, estado, request.referencia()));
        log.info("Notificación registrada id={} usuarioId={} estado={} referencia={}",
                guardada.getId(), usuarioId, estado, request.referencia());
        return NotificacionResponse.from(guardada);
    }

    /** Notificaciones del usuario autenticado, más recientes primero. */
    @Transactional(readOnly = true)
    public PageResponse<NotificacionResponse> listar(Long usuarioId, Integer page, Integer size) {
        return PageResponse.from(
                repository.findByUsuarioId(usuarioId, pageRequestFactory.create(page, size)),
                NotificacionResponse::from);
    }
}
