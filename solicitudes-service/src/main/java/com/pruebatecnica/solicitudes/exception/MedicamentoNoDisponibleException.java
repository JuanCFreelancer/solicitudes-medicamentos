package com.pruebatecnica.solicitudes.exception;

/** El medicamento no existe o fue dado de baja del catálogo. */
public class MedicamentoNoDisponibleException extends RuntimeException {

    public MedicamentoNoDisponibleException(Long medicamentoId) {
        super("El medicamento con id " + medicamentoId + " no existe o no está disponible");
    }
}
