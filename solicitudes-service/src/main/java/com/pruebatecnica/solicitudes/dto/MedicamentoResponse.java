package com.pruebatecnica.solicitudes.dto;

import com.pruebatecnica.solicitudes.entity.Medicamento;

public record MedicamentoResponse(Long id, String nombre, boolean esPos) {

    public static MedicamentoResponse from(Medicamento medicamento) {
        return new MedicamentoResponse(medicamento.getId(), medicamento.getNombre(), medicamento.isEsPos());
    }
}
