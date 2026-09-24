# Especificación de requisitos

Especificación de requisitos del sistema. Formato: historias de usuario con criterios de aceptación verificables.

## 1. Alcance
Aplicación full stack para que usuarios autenticados soliciten medicamentos y consulten sus solicitudes. Los medicamentos **NO POS** (fuera del Plan Obligatorio de Salud) exigen datos adicionales de contacto y entrega.

## 2. Actores
| Actor | Descripción |
|---|---|
| Visitante | Persona sin sesión. Puede registrarse e iniciar sesión. |
| Usuario autenticado | Puede consultar el catálogo, crear solicitudes y listar **sus** solicitudes. |

## 3. Glosario
- **POS / NO POS**: medicamento incluido / no incluido en el Plan Obligatorio de Salud.
- **Solicitud**: petición de un usuario para un medicamento del catálogo.
- **JWT**: token firmado que emite `auth-service` y valida `solicitudes-service`.

## 4. Requisitos funcionales

### RF-1 Registro
*Como visitante quiero crear una cuenta para usar la aplicación.*
- `POST /auth/register` con `nombre`, `email`, `password`.
- **CA1** Responde 201 con `id`, `nombre`, `email` (nunca la contraseña ni su hash).
- **CA2** Correo duplicado (sin distinguir mayúsculas) → 409.
- **CA3** Datos inválidos → 400 con el mensaje por campo. Contraseña: 8–72 caracteres, al menos una letra y un número.
- **CA4** La contraseña se almacena como hash BCrypt.

### RF-2 Inicio de sesión
*Como usuario quiero iniciar sesión para acceder a mis solicitudes.*
- `POST /auth/login` con `email`, `password`.
- **CA1** Credenciales correctas → 200 con `token`, `tipo="Bearer"`, `expiraEn` (segundos).
- **CA2** Credenciales incorrectas → 401 con mensaje genérico (no revela si el correo existe).
- **CA3** El JWT contiene `sub` (id de usuario), `email`, `nombre`, `exp`.

### RF-3 Catálogo de medicamentos
- `GET /medicamentos` (autenticado) devuelve los medicamentos activos, ordenados por nombre, con el indicador `esPos`.

### RF-4 Crear solicitud
*Como usuario quiero solicitar un medicamento.*
- `POST /solicitudes` con `medicamentoId` y, si aplica, `numeroOrden`, `direccion`, `telefono`, `correoContacto`.
- **CA1** Medicamento POS → 201; los datos adicionales no se exigen (si llegan, se descartan).
- **CA2** Medicamento **NO POS** → los cuatro campos son obligatorios; si falta alguno → 400 indicando cuáles.
- **CA3** Teléfono: 7–15 dígitos, `+` inicial opcional (se aceptan espacios, guiones y paréntesis, que se normalizan). Correo con formato válido.
- **CA4** Medicamento inexistente o inactivo → 422.
- **CA5** La solicitud queda asociada al usuario del token, nunca a uno enviado por el cliente.
- **CA6** En la UI, al elegir un NO POS aparece el bloque de datos adicionales; al elegir un POS desaparece y se descarta lo escrito.

### RF-5 Consultar solicitudes
*Como usuario quiero ver mis solicitudes paginadas.*
- `GET /solicitudes?page=&size=` (autenticado).
- **CA1** Solo devuelve las solicitudes del usuario del token.
- **CA2** Orden: más recientes primero. Respuesta: `content`, `page` (base 0), `size`, `totalElements`, `totalPages`.
- **CA3** `size` por defecto 10, máximo 50; valores fuera de rango → 400.
- **CA4** Sin token o token inválido/vencido → 401.
- **CA5** La pantalla muestra estado de carga, vacío y error con reintento.

### RF-6 Notificación al radicar
*Como usuario quiero recibir un aviso cuando mi solicitud queda radicada.*
- `POST /solicitudes` compone dos servicios: crea la solicitud y luego pide a `notificaciones-service` (`POST /notificaciones`) que avise al usuario.
- **CA1** Destinatario: para un NO POS, el correo de contacto de la solicitud; para un POS, el correo de la cuenta (claim `email` del JWT).
- **CA2** La respuesta incluye `notificacion`: `ENVIADA`, `FALLIDA` (el servicio respondió pero no pudo entregar) o `NO_DISPONIBLE` (no se pudo contactar al servicio).
- **CA3** Si notificar falla, la solicitud **se conserva** y el resultado sigue siendo 201.
- **CA4** La notificación ocurre después de confirmar la solicitud, nunca dentro de su transacción de BD.
- **CA5** Una solicitud rechazada por validación (400/422) no genera notificación.
- **CA6** `GET /notificaciones` devuelve solo las notificaciones del usuario autenticado, paginadas.
- **CA7** El campo `notificacion` es opcional en el contrato: los clientes anteriores no se rompen; en los listados no aparece.

## 5. Requisitos no funcionales
| ID | Requisito |
|---|---|
| RNF-1 | Autenticación y solicitudes en **servicios separados**. `auth-service` no es llamado por nadie (validación de JWT *stateless*). La única llamada entre servicios es `solicitudes → notificaciones` (composición), con *timeout* y degradación elegante. |
| RNF-2 | Errores en formato RFC 7807 (`application/problem+json`), sin filtrar trazas ni detalles internos. |
| RNF-3 | Secretos y credenciales solo por variables de entorno; ninguno en el repositorio. |
| RNF-4 | Código en capas (controller → service → repository), DTOs separados de las entidades. |
| RNF-5 | Todo el sistema se levanta con un solo comando (`docker compose up`). |
| RNF-6 | Pruebas automáticas de la lógica de negocio, la API y la UI. |
| RNF-7 | Accesibilidad básica: etiquetas asociadas, `aria-invalid`, mensajes anunciados, foco en el primer error. |
| RNF-8 | Trazabilidad de un proceso entre servicios: cabecera `X-Correlation-Id` (validada) reenviada y presente en cada línea de log. |
| RNF-9 | Una dependencia caída o lenta no debe volver caída ni lenta la API que la usa (*timeout* 2 s, sin propagar el fallo). |

## 6. Puntos de duda y decisión tomada
Puntos abiertos del planteamiento y la decisión tomada en cada uno.

| # | Duda | Decisión | Impacto |
|---|---|---|---|
| 1 | "Listado solo para usuarios autenticados": ¿todas las solicitudes o solo las propias? | **Solo las propias.** Listar las de todos expondría datos personales (dirección, teléfono, correo) de terceros. | Filtro por `usuario_id` del token. |
| 2 | "Encriptado" para el password. | **Hash BCrypt** (irreversible, con salt), no cifrado reversible. | Nadie, ni el sistema, puede recuperar la contraseña. |
| 3 | Dos APIs y un único ER: ¿una o dos bases de datos? | **Una instancia PostgreSQL con dos esquemas** (`auth`, `solicitudes`). En producción, una BD por servicio. | Simplifica el entregable; el FK `usuario_id` sería un vínculo lógico. |
| 4 | ¿Cómo confía `solicitudes-service` en el token? | **JWT HS256 con secreto compartido** por variable de entorno. Alternativa en producción: clave asimétrica (RS256) con JWKS. | Sin acoplamiento en tiempo de ejecución. |
| 5 | Si se envían datos NO POS con un medicamento POS. | Se **descartan** silenciosamente (no aplican). | El CHECK de BD exige "todos o ninguno". |
| 6 | Medicamento dado de baja. | 422 al solicitarlo; no aparece en el catálogo. | El seed incluye uno inactivo para demostrarlo. |
| 7 | ¿Roles/permisos? | Fuera de alcance: un solo rol de usuario. | Ver "Mejoras futuras" en el README. |
| 8 | ¿Por qué existe el servicio de notificaciones? | Los medicamentos NO POS piden correo y teléfono para contactar al paciente; avisarle que su solicitud quedó radicada aprovecha ese dato y es una capacidad reutilizable (exposición → composición → consumo). | Validar con el negocio antes de llevarlo a producción. |
| 9 | ¿Qué pasa si notificar falla tras crear la solicitud? | La solicitud **se conserva** y se informa el estado. Entrega *at-most-once*. | Para garantía de entrega: *outbox* + reintentos o una cola. |

## 7. Trazabilidad requisito → verificación
| Requisito | Pruebas automáticas | Prueba de humo |
|---|---|---|
| RF-1 | `AuthServiceTest`, `AuthControllerTest` | registro válido / duplicado / inválido |
| RF-2 | `AuthServiceTest`, `TokenServiceTest`, `AuthControllerTest`, `auth.service.spec` | login correcto / incorrecto |
| RF-3 | `SolicitudControllerTest` | catálogo con token |
| RF-4 | `SolicitudNoPosValidatorTest`, `SolicitudServiceTest`, `SolicitudRequestTest`, `solicitud-form.component.spec` | POS, NO POS incompleto/completo, medicamento inexistente |
| RF-5 | `PageRequestFactoryTest`, `SolicitudServiceTest`, `pagination.component.spec`, `solicitud-list.component.spec` | paginación y aislamiento entre usuarios |
| RNF-1/2 | `SolicitudControllerTest` (401 en ProblemDetail), `auth.interceptor.spec` | 401 sin token / con token inválido |
| RF-6 | `RadicacionServiceTest`, `HttpNotificationGatewayTest`, `NotificacionServiceTest`, `NotificacionControllerTest`, `solicitud-form.component.spec` | composición, aislamiento de notificaciones, `resilience-test.sh` (notificaciones caído) |
| RNF-8 | `CorrelationIdFilterTest` (×3), `HttpNotificationGatewayTest` | `X-Correlation-Id` devuelto incluso en un 401 |
| RNF-9 | `HttpNotificationGatewayTest` (5xx, 401, conexión), `RadicacionServiceTest` | `resilience-test.sh` |
