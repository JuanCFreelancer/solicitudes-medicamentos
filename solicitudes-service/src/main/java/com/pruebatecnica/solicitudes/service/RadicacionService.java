package com.pruebatecnica.solicitudes.service;

import com.pruebatecnica.solicitudes.dto.SolicitudRequest;
import com.pruebatecnica.solicitudes.dto.SolicitudResponse;
import com.pruebatecnica.solicitudes.notification.EstadoNotificacion;
import com.pruebatecnica.solicitudes.notification.NotificationCommand;
import com.pruebatecnica.solicitudes.notification.NotificationGateway;
import com.pruebatecnica.solicitudes.notification.NotificationsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servicio COMPUESTO (grano grueso) "Radicar solicitud": orquesta dos servicios de grano fino.
 * <ol>
 *   <li>{@link SolicitudService#crear}: valida la regla NO POS y persiste (transacción propia).</li>
 *   <li>{@link NotificationGateway}: avisa al usuario por el servicio de notificaciones.</li>
 * </ol>
 * Reglas del proceso:
 * <ul>
 *   <li>La notificación ocurre <b>después</b> de confirmar la transacción y nunca dentro de ella.</li>
 *   <li>Si notificar falla, la solicitud <b>se conserva</b> y la respuesta lo informa (degradación elegante).</li>
 * </ul>
 * Intencionalmente sin {@code @Transactional}: no hay una transacción distribuida que abarcar.
 */
@Service
public class RadicacionService {

    private static final Logger log = LoggerFactory.getLogger(RadicacionService.class);

    private final SolicitudService solicitudService;
    private final NotificationGateway notificationGateway;
    private final NotificationsProperties notifications;

    public RadicacionService(SolicitudService solicitudService, NotificationGateway notificationGateway,
                             NotificationsProperties notifications) {
        this.solicitudService = solicitudService;
        this.notificationGateway = notificationGateway;
        this.notifications = notifications;
    }

    public SolicitudResponse radicar(AuthenticatedUser user, SolicitudRequest request) {
        SolicitudResponse creada = solicitudService.crear(user.id(), request);

        if (!notifications.enabled()) {
            return creada;
        }
        String destinatario = destinatarioDe(user, creada);
        if (destinatario == null) {
            log.warn("Solicitud id={} sin destinatario para notificar (el token no trae correo)", creada.id());
            return creada;
        }

        EstadoNotificacion estado = notificationGateway.enviar(new NotificationCommand(
                destinatario,
                "Solicitud #" + creada.id() + " registrada",
                mensajeDe(user, creada),
                "solicitud:" + creada.id(),
                user.bearerToken()));
        return creada.conNotificacion(estado);
    }

    /** NO POS: el correo de contacto que el usuario dio para esta solicitud; POS: el correo de su cuenta. */
    private static String destinatarioDe(AuthenticatedUser user, SolicitudResponse solicitud) {
        String correo = solicitud.medicamento().esPos() ? user.email() : solicitud.correoContacto();
        return correo == null || correo.isBlank() ? null : correo;
    }

    private static String mensajeDe(AuthenticatedUser user, SolicitudResponse solicitud) {
        String saludo = user.nombre() == null || user.nombre().isBlank() ? "Hola" : "Hola " + user.nombre();
        return saludo + ", registramos tu solicitud #" + solicitud.id() + " del medicamento "
                + solicitud.medicamento().nombre() + ". Te contactaremos por este medio.";
    }
}
