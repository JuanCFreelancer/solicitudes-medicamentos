# Diagramas

Todos los diagramas están en Mermaid (texto versionable; GitHub los renderiza). Las imágenes exportadas (SVG y PNG) están en [`docs/img/`](img/).

**Índice**
1. [Vista SOA por capas](#1-vista-soa-por-capas)
2. [Modelo de dominio (entre servicios)](#2-modelo-de-dominio-entre-servicios)
3. [Clases de `auth-service`](#3-clases-de-auth-service)
4. [Clases de `solicitudes-service` — capas y regla NO POS](#4-clases-de-solicitudes-service--capas-y-regla-no-pos)
5. [Clases de `solicitudes-service` — composición "Radicar solicitud"](#5-clases-de-solicitudes-service--composición-radicar-solicitud)
6. [Clases de `notificaciones-service`](#6-clases-de-notificaciones-service)
7. [Clases del frontend Angular](#7-clases-del-frontend-angular)
8. [Secuencia: autenticación y uso del token](#8-secuencia-autenticación-y-uso-del-token)
9. [Secuencia: Radicar solicitud (camino feliz y degradación)](#9-secuencia-radicar-solicitud-camino-feliz-y-degradación)

---

## 1. Vista SOA por capas

Las tres fases de SOA — **exposición, composición y consumo** — aplicadas al proyecto.

```mermaid
flowchart TB
    subgraph CONSUMO["CONSUMO · quién usa los servicios"]
        FE["Frontend Angular<br/>aplicación compuesta"]
        OTROS["Swagger UI · Postman · scripts<br/>otros consumidores"]
    end

    subgraph COMPOSICION["COMPOSICIÓN · proceso de negocio (grano grueso)"]
        RAD["Radicar solicitud<br/>RadicacionService<br/>crear + notificar"]
    end

    subgraph EXPOSICION["EXPOSICIÓN · servicios autónomos con contrato REST / OpenAPI (grano fino)"]
        AUTH["auth-service :8081<br/>register · login"]
        SOL["solicitudes-service :8082<br/>SolicitudService · catálogo · listado"]
        NOT["notificaciones-service :8083<br/>enviar · consultar"]
    end

    subgraph ADAPT["ADAPTADORES Y DATOS"]
        CH["NotificationChannel<br/>correo simulado, reemplazable por SMTP"]
        DB[("PostgreSQL<br/>esquemas auth · solicitudes · notificaciones")]
    end

    FE -->|"POST /auth/login"| AUTH
    FE -->|"GET /medicamentos · GET /solicitudes"| SOL
    FE -->|"POST /solicitudes"| RAD
    OTROS --> RAD
    RAD -->|"1. crear solicitud"| SOL
    RAD -->|"2. notificar"| NOT
    AUTH -.-> SOL
    AUTH -.-> NOT
    NOT --> CH
    AUTH --> DB
    SOL --> DB
    NOT --> DB
```

> Líneas punteadas: el JWT que firma `auth-service` (secreto compartido) es validado por los otros dos servicios sin llamarlo. La notificación (paso 2) se hace con *timeout* y degradación elegante.
>
> `RadicacionService` vive dentro de `solicitudes-service` (es un servicio compuesto, no un cuarto despliegue): compone el servicio fino `SolicitudService` con el remoto `notificaciones-service`.

---

## 2. Modelo de dominio (entre servicios)

Cada servicio es dueño de sus entidades. Línea continua = clave foránea real; línea punteada = **referencia lógica** (sin FK, para poder separar las bases de datos).

```mermaid
classDiagram
    direction LR

    class Usuario {
        +Long id
        +String nombre
        +String email
        -String passwordHash
        +Instant createdAt
    }
    class Medicamento {
        +Long id
        +String nombre
        +boolean esPos
        +boolean activo
    }
    class Solicitud {
        +Long id
        +Long usuarioId
        +String numeroOrden
        +String direccion
        +String telefono
        +String correoContacto
        +Instant createdAt
        +paraMedicamentoPos(usuarioId, medicamento)$ Solicitud
        +paraMedicamentoNoPos(usuarioId, medicamento, numeroOrden, direccion, telefono, correoContacto)$ Solicitud
    }
    class Notificacion {
        +Long id
        +Long usuarioId
        +String destinatario
        +String asunto
        +String mensaje
        +CanalNotificacion canal
        +EstadoEnvio estado
        +String referencia
        +Instant createdAt
    }
    class CanalNotificacion {
        <<enumeration>>
        EMAIL
    }
    class EstadoEnvio {
        <<enumeration>>
        ENVIADA
        FALLIDA
    }

    Usuario "1" --> "0..*" Solicitud : crea (FK usuario_id)
    Medicamento "1" --> "0..*" Solicitud : es solicitado en (FK)
    Usuario "1" ..> "0..*" Notificacion : destinatario (referencia lógica)
    Solicitud "1" ..> "0..*" Notificacion : campo referencia (lógica)
    Notificacion --> CanalNotificacion
    Notificacion --> EstadoEnvio
```

| Servicio | Entidades propias | Esquema |
|---|---|---|
| `auth-service` | `Usuario` | `auth` |
| `solicitudes-service` | `Medicamento`, `Solicitud` | `solicitudes` |
| `notificaciones-service` | `Notificacion` | `notificaciones` |

---

## 3. Clases de `auth-service`

```mermaid
classDiagram
    direction TB

    class AuthController {
        +register(RegisterRequest) UsuarioResponse
        +login(LoginRequest) TokenResponse
    }
    class AuthService {
        -String dummyHash
        +register(RegisterRequest) UsuarioResponse
        +login(LoginRequest) TokenResponse
        -normalizeEmail(String)$ String
    }
    class TokenService {
        +issueFor(Usuario) TokenResponse
    }
    class UsuarioRepository {
        <<interface>>
        +findByEmail(String) Optional~Usuario~
        +existsByEmail(String) boolean
    }
    class Usuario {
        +Long id
        +String nombre
        +String email
        -String passwordHash
        +Instant createdAt
    }
    class RegisterRequest {
        <<record>>
        +String nombre
        +String email
        +String password
    }
    class LoginRequest {
        <<record>>
        +String email
        +String password
    }
    class TokenResponse {
        <<record>>
        +String token
        +String tipo
        +long expiraEn
    }
    class UsuarioResponse {
        <<record>>
        +Long id
        +String nombre
        +String email
        +from(Usuario)$ UsuarioResponse
    }
    class GlobalExceptionHandler {
        <<RestControllerAdvice>>
        +handleMethodArgumentNotValid() ResponseEntity
        +handleEmailAlreadyRegistered() ProblemDetail
        +handleInvalidCredentials() ProblemDetail
        +handleUnexpected() ProblemDetail
    }
    class EmailAlreadyRegisteredException
    class InvalidCredentialsException
    class SecurityConfig {
        <<Configuration>>
        +securityFilterChain() SecurityFilterChain
        +passwordEncoder() PasswordEncoder
        +corsConfigurationSource() CorsConfigurationSource
    }
    class JwtConfig {
        <<Configuration>>
        +jwtEncoder() JwtEncoder
    }
    class JwtProperties {
        <<record>>
        +String secret
        +long expirationMinutes
    }
    class CorrelationIdFilter {
        +doFilterInternal()
    }
    class PasswordEncoder {
        <<interface>>
    }
    class JwtEncoder {
        <<interface>>
    }

    AuthController --> AuthService
    AuthController ..> RegisterRequest
    AuthController ..> LoginRequest
    AuthService --> UsuarioRepository
    AuthService --> PasswordEncoder : BCrypt
    AuthService --> TokenService
    AuthService ..> EmailAlreadyRegisteredException : lanza
    AuthService ..> InvalidCredentialsException : lanza
    TokenService --> JwtEncoder
    TokenService --> JwtProperties
    UsuarioRepository ..> Usuario
    UsuarioResponse ..> Usuario
    AuthService ..> UsuarioResponse
    AuthService ..> TokenResponse
    JwtConfig --> JwtProperties
    JwtConfig ..> JwtEncoder : crea
    GlobalExceptionHandler ..> EmailAlreadyRegisteredException : 409
    GlobalExceptionHandler ..> InvalidCredentialsException : 401
```

---

## 4. Clases de `solicitudes-service` — capas y regla NO POS

```mermaid
classDiagram
    direction TB

    class SolicitudController {
        +crear(Jwt, SolicitudRequest) SolicitudResponse
        +listar(Jwt, Integer page, Integer size) PageResponse~SolicitudResponse~
    }
    class MedicamentoController {
        +listar() List~MedicamentoResponse~
    }
    class SolicitudService {
        +crear(Long usuarioId, SolicitudRequest) SolicitudResponse
        +listar(Long usuarioId, Integer page, Integer size) PageResponse~SolicitudResponse~
    }
    class MedicamentoService {
        +listarActivos() List~MedicamentoResponse~
    }
    class SolicitudNoPosValidator {
        <<regla de negocio>>
        +validate(Medicamento, SolicitudRequest)
    }
    class PageRequestFactory {
        +create(Integer page, Integer size) PageRequest
    }
    class SolicitudRepository {
        <<interface>>
        +findByUsuarioId(Long, Pageable) Page~Solicitud~
    }
    class MedicamentoRepository {
        <<interface>>
        +findByActivoTrue(Sort) List~Medicamento~
        +findByIdAndActivoTrue(Long) Optional~Medicamento~
    }
    class Solicitud {
        +Long id
        +Long usuarioId
        +Medicamento medicamento
        +String numeroOrden
        +String direccion
        +String telefono
        +String correoContacto
        +paraMedicamentoPos()$ Solicitud
        +paraMedicamentoNoPos()$ Solicitud
    }
    class Medicamento {
        +Long id
        +String nombre
        +boolean esPos
        +boolean activo
    }
    class SolicitudRequest {
        <<record>>
        +Long medicamentoId
        +String numeroOrden
        +String direccion
        +String telefono
        +String correoContacto
    }
    class SolicitudResponse {
        <<record>>
        +Long id
        +MedicamentoResponse medicamento
        +EstadoNotificacion notificacion
        +from(Solicitud)$ SolicitudResponse
        +conNotificacion(EstadoNotificacion) SolicitudResponse
    }
    class MedicamentoResponse {
        <<record>>
        +Long id
        +String nombre
        +boolean esPos
    }
    class PageResponse~T~ {
        <<record>>
        +List~T~ content
        +int page
        +int size
        +long totalElements
        +int totalPages
    }
    class RequestValidationException {
        +Map errors
    }
    class MedicamentoNoDisponibleException
    class GlobalExceptionHandler {
        <<RestControllerAdvice>>
        +handleRequestValidation() ProblemDetail
        +handleMedicamentoNoDisponible() ProblemDetail
        +handleUnexpected() ProblemDetail
    }
    class ProblemDetailSecurityHandlers {
        +commence() 401
        +handle() 403
    }

    MedicamentoController --> MedicamentoService
    SolicitudController --> SolicitudService : listar
    SolicitudService --> SolicitudRepository
    SolicitudService --> MedicamentoRepository
    SolicitudService --> SolicitudNoPosValidator
    SolicitudService --> PageRequestFactory
    MedicamentoService --> MedicamentoRepository
    SolicitudRepository ..> Solicitud
    MedicamentoRepository ..> Medicamento
    Solicitud "0..*" --> "1" Medicamento
    SolicitudNoPosValidator ..> Medicamento
    SolicitudNoPosValidator ..> SolicitudRequest
    SolicitudNoPosValidator ..> RequestValidationException : lanza
    PageRequestFactory ..> RequestValidationException : lanza
    SolicitudService ..> MedicamentoNoDisponibleException : lanza
    SolicitudService ..> SolicitudResponse
    SolicitudResponse --> MedicamentoResponse
    SolicitudService ..> PageResponse
    GlobalExceptionHandler ..> RequestValidationException : 400
    GlobalExceptionHandler ..> MedicamentoNoDisponibleException : 422
```

---

## 5. Clases de `solicitudes-service` — composición "Radicar solicitud"

El servicio compuesto depende de un **puerto** (`NotificationGateway`), no de HTTP. El adaptador `HttpNotificationGateway` traduce cualquier fallo remoto a `NO_DISPONIBLE`, por lo que la solicitud nunca se pierde.

```mermaid
classDiagram
    direction LR

    class SolicitudController {
        +crear(Jwt, SolicitudRequest) SolicitudResponse
    }
    class AuthenticatedUser {
        <<record>>
        +Long id
        +String nombre
        +String email
        +String bearerToken
    }
    class RadicacionService {
        <<servicio compuesto>>
        +radicar(AuthenticatedUser, SolicitudRequest) SolicitudResponse
        -destinatarioDe(AuthenticatedUser, SolicitudResponse)$ String
        -mensajeDe(AuthenticatedUser, SolicitudResponse)$ String
    }
    class SolicitudService {
        <<servicio fino>>
        +crear(Long usuarioId, SolicitudRequest) SolicitudResponse
    }
    class NotificationGateway {
        <<interface / puerto>>
        +enviar(NotificationCommand) EstadoNotificacion
    }
    class HttpNotificationGateway {
        <<adaptador>>
        +enviar(NotificationCommand) EstadoNotificacion
    }
    class NotificationCommand {
        <<record>>
        +String destinatario
        +String asunto
        +String mensaje
        +String referencia
        +String bearerToken
    }
    class EstadoNotificacion {
        <<enumeration>>
        ENVIADA
        FALLIDA
        NO_DISPONIBLE
    }
    class NotificationsProperties {
        <<record>>
        +boolean enabled
        +String baseUrl
        +Duration timeout
    }
    class NotificationClientConfig {
        <<Configuration>>
        +notificationsRestClient() RestClient
    }
    class CorrelationIdFilter {
        +HEADER = X-Correlation-Id
    }
    class SolicitudResponse {
        <<record>>
        +EstadoNotificacion notificacion
        +conNotificacion(EstadoNotificacion) SolicitudResponse
    }
    class RestClient {
        <<Spring>>
    }
    class NotificacionesService {
        <<servicio remoto :8083>>
        POST /notificaciones
    }

    SolicitudController --> RadicacionService : POST /solicitudes
    SolicitudController ..> AuthenticatedUser : construye desde el JWT
    RadicacionService --> SolicitudService : 1. crear (transacción propia)
    RadicacionService --> NotificationGateway : 2. notificar (fuera de la transacción)
    RadicacionService --> NotificationsProperties : enabled
    RadicacionService ..> SolicitudResponse : conNotificacion(estado)
    NotificationGateway <|.. HttpNotificationGateway
    HttpNotificationGateway --> RestClient
    HttpNotificationGateway ..> NotificationCommand
    HttpNotificationGateway ..> EstadoNotificacion : nunca lanza excepción
    HttpNotificationGateway ..> CorrelationIdFilter : reenvía X-Correlation-Id
    NotificationClientConfig ..> RestClient : timeouts
    NotificationClientConfig --> NotificationsProperties
    RestClient ..> NotificacionesService : HTTP + JWT del usuario
    SolicitudResponse --> EstadoNotificacion
```

---

## 6. Clases de `notificaciones-service`

`NotificationChannel` es el **adaptador**: permite sustituir el correo simulado por un SMTP real, o añadir SMS, sin tocar el servicio ni el contrato.

```mermaid
classDiagram
    direction TB

    class NotificacionController {
        +enviar(Jwt, NotificacionRequest) NotificacionResponse
        +listar(Jwt, Integer page, Integer size) PageResponse~NotificacionResponse~
    }
    class NotificacionService {
        +enviar(Long usuarioId, NotificacionRequest) NotificacionResponse
        +listar(Long usuarioId, Integer page, Integer size) PageResponse~NotificacionResponse~
    }
    class NotificationChannel {
        <<interface / puerto>>
        +send(NotificationMessage)
    }
    class SimulatedEmailChannel {
        <<adaptador>>
        +send(NotificationMessage)
        ~maskEmail(String)$ String
    }
    class SmtpEmailChannel {
        <<futuro>>
        +send(NotificationMessage)
    }
    class NotificationMessage {
        <<record>>
        +String destinatario
        +String asunto
        +String mensaje
    }
    class NotificationDeliveryException
    class NotificacionRepository {
        <<interface>>
        +findByUsuarioId(Long, Pageable) Page~Notificacion~
    }
    class Notificacion {
        +Long id
        +Long usuarioId
        +String destinatario
        +String referencia
        +CanalNotificacion canal
        +EstadoEnvio estado
    }
    class NotificacionRequest {
        <<record>>
        +String destinatario
        +String asunto
        +String mensaje
        +String referencia
    }
    class NotificacionResponse {
        <<record>>
        +Long id
        +EstadoEnvio estado
        +from(Notificacion)$ NotificacionResponse
    }
    class EstadoEnvio {
        <<enumeration>>
        ENVIADA
        FALLIDA
    }
    class CanalNotificacion {
        <<enumeration>>
        EMAIL
    }
    class PageRequestFactory {
        +create(Integer, Integer) PageRequest
    }

    NotificacionController --> NotificacionService
    NotificacionService --> NotificationChannel
    NotificacionService --> NotificacionRepository
    NotificacionService --> PageRequestFactory
    NotificationChannel <|.. SimulatedEmailChannel
    NotificationChannel <|.. SmtpEmailChannel : reemplazo futuro
    NotificationChannel ..> NotificationMessage
    NotificationChannel ..> NotificationDeliveryException : lanza
    NotificacionService ..> NotificationDeliveryException : la captura y registra FALLIDA
    NotificacionRepository ..> Notificacion
    Notificacion --> EstadoEnvio
    Notificacion --> CanalNotificacion
    NotificacionResponse ..> Notificacion
    NotificacionController ..> NotificacionRequest
```

---

## 7. Clases del frontend Angular

Estructura `core` (singleton de la aplicación) → `shared` (reutilizable) → `features` (pantallas). Las funciones (`authInterceptor`, guards) se muestran como clases para ilustrar sus dependencias.

```mermaid
classDiagram
    direction TB

    class AuthService {
        +session Signal~Session|null~
        +register(RegisterRequest) Observable~Usuario~
        +login(LoginRequest) Observable~TokenResponse~
        +logout()
        +isAuthenticated() boolean
        +token() string|null
    }
    class authInterceptor {
        <<HttpInterceptorFn>>
        adjunta Bearer solo a solicitudesApiUrl
        ante 401: logout y redirige a login
    }
    class authGuard {
        <<CanActivateFn>>
    }
    class guestGuard {
        <<CanActivateFn>>
    }
    class SolicitudService {
        +create(SolicitudRequest) Observable~Solicitud~
        +list(page, size) Observable~Page~Solicitud~~
    }
    class MedicamentoService {
        +list() Observable~Medicamento[]~
    }
    class LoginComponent {
        -form FormGroup
        +submit()
    }
    class RegisterComponent {
        -form FormGroup
        +submit()
    }
    class SolicitudFormComponent {
        -form FormGroup
        -selected Signal~Medicamento|null~
        -isNoPos Signal~boolean~
        +submit()
        -configureNoPosFields(required)
        -buildRequest() SolicitudRequest
    }
    class SolicitudListComponent {
        -page Signal~number~
        -size Signal~number~
        -data Signal~Page~
        +goToPage(page)
        +changeSize(size)
        +retry()
    }
    class PaginationComponent {
        +page input
        +totalPages input
        +pageChange output
        +sizeChange output
    }
    class FieldErrorComponent {
        +control input
    }
    class AlertComponent {
        +type input
    }
    class HeaderComponent {
        +logout()
    }
    class formValidators {
        <<funciones>>
        notBlank
        emailFormat
        phoneFormat
        passwordStrength
        fieldsMatch
    }
    class toApiError {
        <<función>>
        HttpErrorResponse a ApiError
    }
    class Session {
        <<interface>>
        +token
        +nombre
        +email
        +expiresAt
    }
    class Solicitud {
        <<interface>>
        +id
        +medicamento
        +notificacion EstadoNotificacion
    }

    LoginComponent --> AuthService
    RegisterComponent --> AuthService
    HeaderComponent --> AuthService
    authInterceptor --> AuthService
    authGuard --> AuthService
    guestGuard --> AuthService
    AuthService --> Session
    SolicitudFormComponent --> SolicitudService
    SolicitudFormComponent --> MedicamentoService
    SolicitudListComponent --> SolicitudService
    SolicitudListComponent --> PaginationComponent
    SolicitudFormComponent --> FieldErrorComponent
    LoginComponent --> FieldErrorComponent
    RegisterComponent --> FieldErrorComponent
    SolicitudFormComponent --> AlertComponent
    SolicitudFormComponent ..> formValidators
    RegisterComponent ..> formValidators
    SolicitudFormComponent ..> toApiError
    SolicitudService --> Solicitud
```

---

## 8. Secuencia: autenticación y uso del token

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant FE as Frontend
    participant A as auth-service
    participant S as solicitudes-service
    participant DB as PostgreSQL

    U->>FE: correo + contraseña
    FE->>A: POST /auth/login
    A->>DB: busca el usuario por correo
    A->>A: compara con BCrypt (hash de relleno si no existe)
    A-->>FE: 200 { token (JWT firmado HS256), tipo, expiraEn }
    Note over FE: guarda la sesión en sessionStorage
    FE->>S: GET /solicitudes  (Authorization: Bearer token)
    S->>S: valida firma y expiración (sin llamar a auth-service)
    S->>DB: solicitudes WHERE usuario_id = sub del token
    S-->>FE: 200 página de solicitudes
    Note over FE,S: si el token venció → 401 → el interceptor cierra sesión y redirige al login
```

---

## 9. Secuencia: Radicar solicitud (camino feliz y degradación)

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant FE as Frontend
    participant C as SolicitudController
    participant R as RadicacionService
    participant SS as SolicitudService
    participant DB as PostgreSQL
    participant G as HttpNotificationGateway
    participant N as notificaciones-service

    U->>FE: elige medicamento (NO POS + datos) y envía
    FE->>C: POST /solicitudes + Bearer + X-Correlation-Id
    C->>R: radicar(AuthenticatedUser, request)
    R->>SS: crear(usuarioId, request)
    SS->>SS: SolicitudNoPosValidator: campos obligatorios si es NO POS
    SS->>DB: INSERT solicitud (transacción confirmada aquí)
    SS-->>R: SolicitudResponse (id)
    Note over R,G: la notificación ocurre FUERA de la transacción
    R->>G: enviar(destinatario, asunto, mensaje, "solicitud:id", token)
    G->>N: POST /notificaciones (JWT + X-Correlation-Id, timeout 2 s)

    alt notificaciones responde
        N->>N: NotificationChannel.send(...)
        N-->>G: 201 { estado: ENVIADA | FALLIDA }
        G-->>R: ENVIADA o FALLIDA
    else caído, lento o con error
        N--xG: timeout / conexión rechazada / 5xx
        G-->>R: NO_DISPONIBLE (sin lanzar excepción)
    end

    R-->>C: SolicitudResponse.conNotificacion(estado)
    C-->>FE: 201 (la solicitud existe siempre)
    FE-->>U: "Solicitud registrada" + aviso del correo según el estado
```
