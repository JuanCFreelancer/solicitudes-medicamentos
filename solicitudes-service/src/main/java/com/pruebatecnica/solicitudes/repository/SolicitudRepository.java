package com.pruebatecnica.solicitudes.repository;

import com.pruebatecnica.solicitudes.entity.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    /** El EntityGraph trae el medicamento en la misma consulta (evita el problema N+1 al listar). */
    @EntityGraph(attributePaths = "medicamento")
    Page<Solicitud> findByUsuarioId(Long usuarioId, Pageable pageable);
}
