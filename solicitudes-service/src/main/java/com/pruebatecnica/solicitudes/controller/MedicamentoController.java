package com.pruebatecnica.solicitudes.controller;

import com.pruebatecnica.solicitudes.dto.MedicamentoResponse;
import com.pruebatecnica.solicitudes.service.MedicamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/medicamentos")
@Tag(name = "Medicamentos")
public class MedicamentoController {

    private final MedicamentoService medicamentoService;

    public MedicamentoController(MedicamentoService medicamentoService) {
        this.medicamentoService = medicamentoService;
    }

    @GetMapping
    @Operation(summary = "Lista los medicamentos disponibles (indica si son POS o NO POS)")
    public List<MedicamentoResponse> listar() {
        return medicamentoService.listarActivos();
    }
}
