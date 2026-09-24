-- =====================================================================
--  Esquema de base de datos - Módulo de Solicitudes de Medicamentos
--  Motor: PostgreSQL 16
--
--  Dos esquemas, uno por servicio:
--    auth         -> propiedad del auth-service         (usuarios)
--    solicitudes  -> propiedad del solicitudes-service  (medicamentos, solicitudes)
--    notificaciones -> propiedad del notificaciones-service (notificaciones)
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS solicitudes;
CREATE SCHEMA IF NOT EXISTS notificaciones;

-- ---------------------------------------------------------------------
-- auth.usuarios
-- password_hash almacena un hash BCrypt (irreversible), nunca la clave.
-- ---------------------------------------------------------------------
CREATE TABLE auth.usuarios (
    id             BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    email          VARCHAR(254) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_email_minusculas CHECK (email = lower(email))
);

-- ---------------------------------------------------------------------
-- solicitudes.medicamentos  (catálogo)
-- es_pos = TRUE  -> incluido en el Plan Obligatorio de Salud
-- es_pos = FALSE -> NO POS: la solicitud exige datos adicionales
-- ---------------------------------------------------------------------
CREATE TABLE solicitudes.medicamentos (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre     VARCHAR(150) NOT NULL,
    es_pos     BOOLEAN      NOT NULL,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_medicamentos_nombre UNIQUE (nombre)
);

-- ---------------------------------------------------------------------
-- solicitudes.solicitudes
-- Los 4 campos NO POS son "todos NULL o todos con valor".
-- La regla "medicamento NO POS => campos obligatorios" cruza tablas, por lo
-- que la aplica el servicio (SolicitudNoPosValidator); el CHECK es la red
-- de seguridad de integridad a nivel de BD.
-- usuario_id referencia auth.usuarios (FK); en producción cada servicio
-- tendría su propia BD y este vínculo sería lógico (ver README).
-- ---------------------------------------------------------------------
CREATE TABLE solicitudes.solicitudes (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL REFERENCES auth.usuarios (id),
    medicamento_id  BIGINT       NOT NULL REFERENCES solicitudes.medicamentos (id),
    numero_orden    VARCHAR(50),
    direccion       VARCHAR(255),
    telefono        VARCHAR(20),
    correo_contacto VARCHAR(254),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_solicitudes_datos_no_pos CHECK (
        (numero_orden IS NULL AND direccion IS NULL AND telefono IS NULL AND correo_contacto IS NULL)
        OR
        (numero_orden IS NOT NULL AND direccion IS NOT NULL AND telefono IS NOT NULL AND correo_contacto IS NOT NULL)
    )
);

-- Listado paginado de un usuario, más recientes primero.
CREATE INDEX ix_solicitudes_usuario_fecha
    ON solicitudes.solicitudes (usuario_id, created_at DESC, id DESC);
CREATE INDEX ix_solicitudes_medicamento
    ON solicitudes.solicitudes (medicamento_id);

-- ---------------------------------------------------------------------
-- notificaciones.notificaciones
-- Servicio fino y reutilizable: no conoce solicitudes ni medicamentos.
-- usuario_id y referencia son referencias LÓGICAS (sin FK): cada servicio es
-- dueño de sus datos y puede tener su propia BD sin cambiar el código.
-- ---------------------------------------------------------------------
CREATE TABLE notificaciones.notificaciones (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id    BIGINT        NOT NULL,
    destinatario  VARCHAR(254)  NOT NULL,
    asunto        VARCHAR(150)  NOT NULL,
    mensaje       VARCHAR(1000) NOT NULL,
    canal         VARCHAR(20)   NOT NULL,
    estado        VARCHAR(20)   NOT NULL,
    referencia    VARCHAR(100),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_notificaciones_canal  CHECK (canal IN ('EMAIL')),
    CONSTRAINT ck_notificaciones_estado CHECK (estado IN ('ENVIADA', 'FALLIDA'))
);

CREATE INDEX ix_notificaciones_usuario_fecha
    ON notificaciones.notificaciones (usuario_id, created_at DESC, id DESC);
