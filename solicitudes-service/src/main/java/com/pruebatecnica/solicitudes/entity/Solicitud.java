package com.pruebatecnica.solicitudes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Referencia lógica al usuario del auth-service (id que viene en el "sub" del JWT). */
    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false, updatable = false)
    private Medicamento medicamento;

    @Column(name = "numero_orden", length = 50)
    private String numeroOrden;

    @Column(length = 255)
    private String direccion;

    @Column(length = 20)
    private String telefono;

    @Column(name = "correo_contacto", length = 254)
    private String correoContacto;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Solicitud() {
        // requerido por JPA
    }

    private Solicitud(Long usuarioId, Medicamento medicamento) {
        this.usuarioId = usuarioId;
        this.medicamento = medicamento;
    }

    /** Solicitud de un medicamento POS: no lleva datos adicionales. */
    public static Solicitud paraMedicamentoPos(Long usuarioId, Medicamento medicamento) {
        return new Solicitud(usuarioId, medicamento);
    }

    /** Solicitud de un medicamento NO POS: los cuatro datos adicionales son obligatorios. */
    public static Solicitud paraMedicamentoNoPos(Long usuarioId, Medicamento medicamento, String numeroOrden,
                                                 String direccion, String telefono, String correoContacto) {
        Solicitud solicitud = new Solicitud(usuarioId, medicamento);
        solicitud.numeroOrden = numeroOrden;
        solicitud.direccion = direccion;
        solicitud.telefono = telefono;
        solicitud.correoContacto = correoContacto;
        return solicitud;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public String getNumeroOrden() {
        return numeroOrden;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreoContacto() {
        return correoContacto;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
