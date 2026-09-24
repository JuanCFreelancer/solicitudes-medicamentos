package com.pruebatecnica.solicitudes.service;

import com.pruebatecnica.solicitudes.dto.PageResponse;
import com.pruebatecnica.solicitudes.dto.SolicitudRequest;
import com.pruebatecnica.solicitudes.dto.SolicitudResponse;
import com.pruebatecnica.solicitudes.entity.Medicamento;
import com.pruebatecnica.solicitudes.entity.Solicitud;
import com.pruebatecnica.solicitudes.exception.MedicamentoNoDisponibleException;
import com.pruebatecnica.solicitudes.repository.MedicamentoRepository;
import com.pruebatecnica.solicitudes.repository.SolicitudRepository;
import com.pruebatecnica.solicitudes.validator.SolicitudNoPosValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitudService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudService.class);

    private final SolicitudRepository solicitudRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final SolicitudNoPosValidator noPosValidator;
    private final PageRequestFactory pageRequestFactory;

    public SolicitudService(SolicitudRepository solicitudRepository, MedicamentoRepository medicamentoRepository,
                            SolicitudNoPosValidator noPosValidator, PageRequestFactory pageRequestFactory) {
        this.solicitudRepository = solicitudRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.noPosValidator = noPosValidator;
        this.pageRequestFactory = pageRequestFactory;
    }

    @Transactional
    public SolicitudResponse crear(Long usuarioId, SolicitudRequest request) {
        Medicamento medicamento = medicamentoRepository.findByIdAndActivoTrue(request.medicamentoId())
                .orElseThrow(() -> new MedicamentoNoDisponibleException(request.medicamentoId()));

        noPosValidator.validate(medicamento, request);

        // Para un medicamento POS los datos adicionales se descartan: no aplican y la BD exige "todos o ninguno".
        Solicitud solicitud = medicamento.isEsPos()
                ? Solicitud.paraMedicamentoPos(usuarioId, medicamento)
                : Solicitud.paraMedicamentoNoPos(usuarioId, medicamento, request.numeroOrden(),
                        request.direccion(), request.telefono(), request.correoContacto());

        Solicitud guardada = solicitudRepository.save(solicitud);
        log.info("Solicitud creada id={} usuarioId={} medicamentoId={} noPos={}",
                guardada.getId(), usuarioId, medicamento.getId(), !medicamento.isEsPos());
        return SolicitudResponse.from(guardada);
    }

    /** Solicitudes del usuario autenticado, más recientes primero. */
    @Transactional(readOnly = true)
    public PageResponse<SolicitudResponse> listar(Long usuarioId, Integer page, Integer size) {
        return PageResponse.from(
                solicitudRepository.findByUsuarioId(usuarioId, pageRequestFactory.create(page, size)),
                SolicitudResponse::from);
    }
}
