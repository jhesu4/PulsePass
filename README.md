# PulsePass

PulsePass es una API REST para consultar eventos y artistas, registrar usuarios, autenticar solicitudes y gestionar entradas. Está desarrollada en Java con Spring Boot; PostgreSQL almacena los datos y Flyway administra el esquema.

## Contenido

1. [Tecnologías y requisitos](#tecnologías-y-requisitos)
2. [Estructura](#estructura)
3. [Dominio y persistencia](#dominio-y-persistencia)
4. [API REST](#api-rest)
5. [Autenticación y autorización](#autenticación-y-autorización)
6. [Configuración local](#configuración-local)
7. [Pruebas](#pruebas)
8. [Decisiones y límites conocidos](#decisiones-y-límites-conocidos)

## Tecnologías y requisitos

| Área | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1, Spring MVC, Spring Security y Spring Data JPA |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway |
| JWT | JJWT 0.13.0, firma HS256 |
| Mapeo | MapStruct 1.6.3 |
| Pruebas | JUnit 5, Mockito, AssertJ, MockMvc y Testcontainers |
| Build | Maven Wrapper (`mvnw` / `mvnw.cmd`) |

Se requiere Java 21. La suite completa de integración requiere Docker en ejecución para iniciar PostgreSQL mediante Testcontainers. No se usa H2. Hibernate valida el esquema con `ddl-auto: validate`; Flyway es responsable de crearlo.

## Estructura

La estructura concreta mantiene el paquete base `com.pulse.pass` y organiza el código por responsabilidad:

```text
src/
├── main/
│   ├── java/com/pulse/pass/
│   │   ├── controller/       AuthController, ArtistController, EventController,
│   │   │                     TicketController, UserController, VenueController
│   │   ├── service/          AuthService, AuthorizationService y contratos de dominio
│   │   │   └── impl/          Servicios, reglas de negocio y TicketPriceCalculator
│   │   ├── dto/
│   │   │   ├── request/       LoginRequest, RegisterUserRequest y requests de dominio
│   │   │   └── response/      AuthResponse, AuthenticatedUserResponse y responses
│   │   ├── security/
│   │   │   ├── SecurityConfig.java, JwtProperties.java, JwtService.java
│   │   │   ├── JwtAuthenticationFilter.java, CustomUserDetailsService.java
│   │   │   ├── SecurityPrincipal.java, RestAuthenticationEntryPoint.java
│   │   │   ├── RestAccessDeniedHandler.java, RequestIdFilter.java
│   │   │   └── CorsProperties.java, SecurityAuditLogger.java,
│   │   │       RestSecurityErrorResponseWriter.java
│   │   ├── domain/            Entidades, credenciales y roles
│   │   ├── repository/        Repositories JPA
│   │   ├── exception/         Excepciones y GlobalExceptionHandler
│   │   └── mapper/            Mappers MapStruct
│   └── resources/
│       ├── application.yaml
│       └── db/migration/      Migraciones V1 a V4
└── test/java/com/pulse/pass/
    ├── controller/            Pruebas MockMvc
    ├── security/              Pruebas unitarias de seguridad
    ├── integration/           Flujos HTTP y persistencia con Testcontainers
    ├── repository/            Pruebas JPA
    └── service/               Pruebas unitarias de servicios
```

La configuración, los filtros y los handlers de seguridad se mantienen en el paquete previsto por el PRD. CORS, auditoría, `X-Request-ID` y la escritura de errores son clases auxiliares dentro de ese mismo paquete; no requieren una estructura de producción adicional.

## Dominio y persistencia

El dominio comprende venues, eventos, artistas, usuarios, perfiles, credenciales, roles y entradas. `Ticket` es una entidad propia porque conserva precio, tipo, estado, código y fecha de compra.

Relaciones principales:

- `Venue` a `Event`: uno a muchos.
- `Event` a `Artist`: muchos a muchos mediante `event_artists`.
- `User` a `UserProfile`: uno a uno.
- `User` a `UserCredential`: uno a uno mediante clave primaria compartida.
- `User` a `UserRole`: uno a muchos, con clave compuesta `(user_id, role)`.
- `User` y `Event` a `Ticket`: uno a muchos.

Las migraciones de `src/main/resources/db/migration/` se aplican en orden:

| Migración | Contenido |
|---|---|
| `V1__create_schema.sql` | Esquema del dominio, claves, restricciones e índices. |
| `V2__insert_initial.artists.sql` | Catálogo inicial de artistas. |
| `V3__add_streaming_url_to_event.sql` | Campo opcional `streaming_url` para eventos. |
| `V4__security_credentials_roles.sql` | Tablas de credenciales y roles. |

Una migración aplicada no se edita; todo cambio estructural futuro debe agregarse en una nueva versión. Las claves foráneas, unicidad y rangos se protegen también en PostgreSQL. Los enums se persisten por nombre, no por ordinal.

## API REST

La API expone 22 rutas:

| Recurso | Método y rutas |
|---|---|
| Autenticación | `POST /api/auth/login`, `GET /api/auth/me` |
| Usuarios | `POST /api/users`, `GET /api/users/by-email`, `GET /api/users/by-username` |
| Venues | `GET /api/venues/{code}`, `GET /api/venues/active` |
| Artistas | `GET /api/artists/{id}`, `GET /api/artists/by-stage-name`, `GET /api/artists/active` |
| Eventos | `POST /api/events`, `GET /api/events/{eventCode}`, `GET /api/events/published`, `PATCH /api/events/{eventCode}/publish`, `POST /api/events/{eventCode}/artists/{artistId}`, `GET /api/events/{eventCode}/tickets/paid`, `GET /api/events/by-artist` |
| Entradas | `POST /api/tickets`, `GET /api/tickets/{ticketCode}`, `GET /api/tickets/by-user`, `PATCH /api/tickets/{ticketCode}/cancel`, `PATCH /api/tickets/{ticketCode}/use` |

Los controladores reciben y devuelven DTOs; no exponen entidades JPA. Jakarta Bean Validation verifica los requests. `GlobalExceptionHandler` normaliza los errores: validación o JSON inválido devuelve 400, recurso inexistente 404, conflicto de negocio 409, falta de autenticación 401 y falta de autorización 403.

## Autenticación y autorización

La API es stateless. El registro crea usuario, perfil, hash BCrypt y rol inicial `USER` en una transacción. El login acepta username o email y devuelve un JWT de acceso con tipo `Bearer` y duración en segundos. Usuarios inactivos y usuarios legados sin credencial no pueden autenticarse. `GET /api/auth/me` devuelve ID, username y roles del principal autenticado.

La contraseña debe tener al menos 12 caracteres y no superar 72 bytes UTF-8, límite aplicado por BCrypt. El username no puede contener `@`, para distinguirlo del identificador de email. La contraseña en texto plano nunca se persiste ni se devuelve.

Los JWT se firman con HS256 y se validan por firma, algoritmo, tipo `JWT`, issuer, subject, fechas de emisión y expiración e identificador del token. La clave debe ser Base64 válida y decodificar a por lo menos 32 bytes.

La autorización combina reglas HTTP en `SecurityConfig` con verificaciones de rol, ownership y propósito de soporte en la capa de servicio:

| Operación | Regla efectiva |
|---|---|
| Login y registro | Públicos. El registro solo concede `USER`. |
| Venues y artistas | Lectura pública. |
| Eventos publicados y búsqueda por artista | Lectura pública limitada a eventos publicados para usuarios normales. `STAFF` y `ADMIN` también pueden consultar eventos no publicados. |
| Detalle de evento | Público para eventos publicados; `STAFF` y `ADMIN` pueden consultar otros estados. |
| Crear/publicar evento y asociar artista | `ADMIN`. |
| Consultar entradas pagadas por evento | `STAFF` o `ADMIN`. |
| Comprar entrada | `USER` y el email debe pertenecer al usuario autenticado. Solo se compra para eventos publicados, futuros y con capacidad disponible; el precio se calcula en el servidor. |
| Consultar una entrada | Propietario, `STAFF` o `ADMIN` con propósito `TICKET_SUPPORT` para datos ajenos. |
| Listar entradas por email | Propietario o `ADMIN` con propósito `TICKET_SUPPORT`. |
| Cancelar entrada | Propietario o `ADMIN` con propósito `TICKET_SUPPORT`; solo una entrada pagada antes del evento. |
| Marcar entrada como usada | `STAFF` o `ADMIN`, solo desde estado pagado. |
| Consultar usuario por email o username | El mismo usuario o `ADMIN` con propósito `CUSTOMER_SUPPORT`. |

Para operaciones de soporte sobre datos ajenos, el administrador envía `X-Admin-Purpose` con el propósito requerido (`CUSTOMER_SUPPORT` o `TICKET_SUPPORT`). El servicio valida el rol, el valor y el recurso; el header no sustituye la autenticación.

`RequestIdFilter` devuelve `X-Request-ID`, genera uno si falta o es inválido y registra eventos de seguridad sin incluir contraseñas, tokens ni hashes. HTTP se rechaza fuera de loopback cuando `PULSEPASS_REQUIRE_HTTPS=true`; también se admite TLS terminado en un proxy configurado para reenviar encabezados de forma confiable. CORS restringe los orígenes configurados, no usa credenciales y expone `X-Request-ID`.

### Provisionamiento local de roles

No hay cuentas privilegiadas ni contraseñas de demostración creadas por defecto. El registro público siempre asigna `USER`. Para asignar un rol privilegiado en desarrollo:

1. Inicia con el perfil `local` y el bootstrap desactivado; registra normalmente la cuenta.
2. Reinicia con `PULSEPASS_LOCAL_ROLE_BOOTSTRAP_ENABLED=true`, `PULSEPASS_LOCAL_ROLE_BOOTSTRAP_EMAIL` igual al email registrado y `PULSEPASS_LOCAL_ROLE_BOOTSTRAP_ROLE=STAFF` o `ADMIN`.

El bootstrap es opt-in, solo funciona con el perfil `local`, asigna el rol a un usuario existente y no admite `USER`. No se debe habilitar en producción.

## Configuración local

Spring lee variables de entorno. `.env.example` sirve como plantilla; Spring no la carga automáticamente.

| Variable | Valor o requisito |
|---|---|
| `DB_URL` | Opcional; por defecto `jdbc:postgresql://localhost:5432/pulsepass`. |
| `DB_USER` | Opcional; por defecto `postgres`. |
| `DB_PASSWORD` | Obligatoria; no tiene contraseña predeterminada. |
| `PULSEPASS_JWT_ISSUER` | Opcional; por defecto `pulsepass-api`. |
| `PULSEPASS_JWT_SECRET_BASE64` | Obligatoria; Base64 de al menos 32 bytes aleatorios. |
| `PULSEPASS_JWT_ACCESS_TOKEN_TTL` | Opcional; por defecto `PT15M`. |
| `PULSEPASS_CORS_ALLOWED_ORIGINS` | Opcional; por defecto `http://localhost:3000`; lista separada por comas. |
| `PULSEPASS_REQUIRE_HTTPS` | Opcional; `true` por defecto. Loopback se permite para desarrollo. |
| `PULSEPASS_LOCAL_ROLE_BOOTSTRAP_ENABLED` | Opcional; `false` por defecto y requiere perfil `local`. |
| `PULSEPASS_LOCAL_ROLE_BOOTSTRAP_EMAIL` | Email del usuario existente al que se asigna el rol local. |
| `PULSEPASS_LOCAL_ROLE_BOOTSTRAP_ROLE` | `STAFF` o `ADMIN`. |

### Windows (PowerShell)

Define la contraseña local de PostgreSQL y genera una clave JWT aleatoria para la sesión actual:

```powershell
$env:DB_PASSWORD = "<contraseña-local-de-postgres>"
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:PULSEPASS_JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Inicia PostgreSQL 16 si aún no está disponible:

```powershell
docker run --name pulsepass-db -e POSTGRES_DB=pulsepass -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=$env:DB_PASSWORD -p 5432:5432 -d postgres:16
```

Inicia la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway aplica las migraciones al iniciar. No se incluyen claves JWT ni contraseñas de base de datos en el repositorio.

Para probar HTTP en un laboratorio aislado, configura `PULSEPASS_REQUIRE_HTTPS=false`. No uses esa opción en redes compartidas ni en producción. Con TLS terminado en proxy, el proxy debe sanear los encabezados reenviados y el backend no debe ser accesible directamente desde redes no confiables.

## Pruebas

Ejecuta toda la suite desde la raíz del proyecto:

```powershell
.\mvnw.cmd clean test
```

En Linux o macOS:

```bash
./mvnw clean test
```

La suite completa incluye pruebas de integración que requieren Docker. Los reportes de Maven quedan en `target/surefire-reports/`.

Cobertura principal:

- `controller/`: contratos HTTP, validación y errores; las pruebas MVC declaran identidades de prueba.
- `security/`: BCrypt, política de contraseñas, JWT, filtro Bearer, request ID, HTTPS configurable, carga de principal y bootstrap local.
- `AuthServiceIntegrationTest`: registro, login, roles y autenticación con PostgreSQL.
- `SecurityRouteMatrixIntegrationTest`: matriz de rutas y roles; simula servicios para aislar las reglas de autorización HTTP.
- `SecurityAuthorizationIntegrationTest`: ownership, propósitos de administrador, CORS, request ID y validación JWT por HTTP con servicios y persistencia reales.
- `ScenarioFinalIntegrationTest`: demostración de registro/login, evento, compra, uso/cancelación de entradas y rechazos de seguridad.
- `SecurityMigrationUpgradeTest` y pruebas de `repository/`: migraciones y persistencia sobre PostgreSQL/Testcontainers.

## Decisiones y límites conocidos

- MapStruct transforma entidades a DTOs; los controladores delegan la lógica de negocio a los servicios.
- Las escrituras se ejecutan en transacciones. El precio de las entradas se calcula en servidor con `BigDecimal`.
- `open-in-view` está desactivado y Hibernate valida el esquema en lugar de modificarlo.
- No hay limitación distribuida de intentos de login. Producción requiere una estrategia de rate limiting y gestión/rotación de secretos.
- `STAFF` tiene acceso operativo a las consultas previstas por el modelo actual; limitarlo a eventos asignados requiere incorporar esa relación al dominio.