package com.pruebatecnica.solicitudes.repository;

import com.pruebatecnica.solicitudes.entity.Medicamento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    List<Medicamento> findByActivoTrue(Sort sort);

    Optional<Medicamento> findByIdAndActivoTrue(Long id);
}
