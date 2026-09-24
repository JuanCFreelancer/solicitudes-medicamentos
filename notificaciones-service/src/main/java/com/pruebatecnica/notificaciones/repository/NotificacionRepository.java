package com.pruebatecnica.notificaciones.repository;

import com.pruebatecnica.notificaciones.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByUsuarioId(Long usuarioId, Pageable pageable);
}
