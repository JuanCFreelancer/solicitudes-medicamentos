package com.pruebatecnica.notificaciones.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Referencia lógica al usuario del auth-service (sin FK entre servicios). */
    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 254)
    private String destinatario;

    @Column(nullable = false, length = 150)
    private String asunto;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacion canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEnvio estado;

    /** Identifica el proceso que originó la notificación, p. ej. "solicitud:12". Referencia lógica, sin FK. */
    @Column(length = 100)
    private String referencia;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notificacion() {
        // requerido por JPA
    }

    public Notificacion(Long usuarioId, String destinatario, String asunto, String mensaje,
                        CanalNotificacion canal, EstadoEnvio estado, String referencia) {
        this.usuarioId = usuarioId;
        this.destinatario = destinatario;
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.canal = canal;
        this.estado = estado;
        this.referencia = referencia;
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

    public String getDestinatario() {
        return destinatario;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public CanalNotificacion getCanal() {
        return canal;
    }

    public EstadoEnvio getEstado() {
        return estado;
    }

    public String getReferencia() {
        return referencia;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
