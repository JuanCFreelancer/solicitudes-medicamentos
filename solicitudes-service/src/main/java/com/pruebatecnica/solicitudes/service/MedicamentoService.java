package com.pruebatecnica.solicitudes.service;

import com.pruebatecnica.solicitudes.dto.MedicamentoResponse;
import com.pruebatecnica.solicitudes.repository.MedicamentoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;

    public MedicamentoService(MedicamentoRepository medicamentoRepository) {
        this.medicamentoRepository = medicamentoRepository;
    }

    /** Catálogo de medicamentos vigentes, ordenado alfabéticamente. */
    @Transactional(readOnly = true)
    public List<MedicamentoResponse> listarActivos() {
        return medicamentoRepository.findByActivoTrue(Sort.by("nombre")).stream()
                .map(MedicamentoResponse::from)
                .toList();
    }
}
