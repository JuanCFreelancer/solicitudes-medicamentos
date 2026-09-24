package com.pruebatecnica.solicitudes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "medicamentos")
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    /** true = incluido en el POS; false = NO POS (la solicitud exige datos adicionales). */
    @Column(name = "es_pos", nullable = false)
    private boolean esPos;

    @Column(nullable = false)
    private boolean activo;

    protected Medicamento() {
        // requerido por JPA
    }

    public Medicamento(String nombre, boolean esPos, boolean activo) {
        this.nombre = nombre;
        this.esPos = esPos;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isEsPos() {
        return esPos;
    }

    public boolean isActivo() {
        return activo;
    }
}
