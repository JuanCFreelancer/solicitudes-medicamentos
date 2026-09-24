# Modelo Entidad-Relación

Diagrama en Mermaid (texto plano, versionable; se renderiza en GitHub).

```mermaid
erDiagram
    USUARIOS ||--o{ SOLICITUDES : "crea"
    MEDICAMENTOS ||--o{ SOLICITUDES : "es solicitado en"

    USUARIOS {
        bigint id PK
        varchar nombre
        varchar email UK "minúsculas, único"
        varchar password_hash "BCrypt"
        timestamptz created_at
    }

    MEDICAMENTOS {
        bigint id PK
        varchar nombre UK
        boolean es_pos "false = NO POS"
        boolean activo
    }

    SOLICITUDES {
        bigint id PK
        bigint usuario_id FK
        bigint medicamento_id FK
        varchar numero_orden "solo NO POS"
        varchar direccion "solo NO POS"
        varchar telefono "solo NO POS"
        varchar correo_contacto "solo NO POS"
        timestamptz created_at
    }
```

## Cardinalidades
- Un **usuario** crea 0..N **solicitudes**; cada solicitud pertenece a un único usuario (1:N).
- Un **medicamento** aparece en 0..N **solicitudes**; cada solicitud referencia exactamente un medicamento (1:N).
- Usuarios y medicamentos no se relacionan directamente: la relación N:M entre ambos se resuelve a través de `solicitudes`.

## Reglas de integridad
| Regla | Dónde se garantiza |
|---|---|
| Email único y en minúsculas | `UNIQUE` + `CHECK` en `usuarios` |
| Password nunca en texto plano | Hash BCrypt en `auth-service` |
| Medicamento NO POS ⇒ 4 campos obligatorios | `SolicitudNoPosValidator` (servicio) + validación del formulario |
| Los 4 campos NO POS son "todos o ninguno" | `CHECK ck_solicitudes_datos_no_pos` |
| Solicitud siempre ligada a usuario y medicamento existentes | Claves foráneas |

## Esquemas
- `auth` → tabla `usuarios` (propiedad de `auth-service`).
- `solicitudes` → tablas `medicamentos` y `solicitudes` (propiedad de `solicitudes-service`).

Script completo: [`schema.sql`](schema.sql) · datos de ejemplo: [`seed.sql`](seed.sql).
