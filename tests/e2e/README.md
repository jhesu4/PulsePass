# Pruebas E2E manuales con Postman (Fase 5)

Pruebas manuales de extremo a extremo de la API PulsePass (controladores + seguridad JWT/RBAC). **Sin scripts ni automatización**: ni Pre-request, ni Tests, ni Runner, ni Newman. Cada request se envía con **Send** y el resultado se anota a mano.

## Contenido

| Archivo | Propósito |
|---|---|
| `postman/PulsePass-Manual-E2E.postman_collection.json` | Colección v2.1: 153 requests en 10 carpetas por módulo y flujo |
| `postman/PulsePass-local.postman_environment.json` | Plantilla de entorno (secretos y tokens vacíos) |
| `fixtures/seed-e2e.sql`, `fixtures/README.md` | Datos E2E y cómo cargarlos |
| `checklist-e2e-manual.md` | Lista de verificación por caso |
| `registro-ejecucion-manual.md` | Plantilla para registrar resultados |
| `matriz-cobertura.md` | Cobertura por endpoint, rol y tema de seguridad |

## Preparación

1. Cree la base **`pulsepass_e2e`** (distinta de dev/QA/producción).
2. Arranque la app contra esa base con su `.env` local (Flyway aplica V1–V4). Local: `http://localhost:8080` (HTTP solo se acepta en localhost).
3. Cargue la semilla (ver `fixtures/README.md`).
4. En Postman importe la colección y el entorno. **Duplique el entorno**, complételo localmente y no lo exporte ni lo suba: guarda tokens y contraseñas. El `.gitignore` excluye `*.postman_environment.json`, excepto la plantilla versionada.
5. Defina `runId` (p. ej. `e2e-20261019-1`), `futureDate` (fecha futura) y las contraseñas.

## Ejecución por flujos

Las carpetas de Postman siguen la sección 10.1: **00 Preparación / conectividad**, **01 Venues**, **02 Artists**, **03 Auth / usuarios**, **04 Usuarios / consulta y propiedad**, **05 Events / administración y visibilidad**, **06 Tickets / compra, consulta y ciclo de vida**, **07 Seguridad / escenarios negativos**, **08 Validaciones y errores HTTP** y **09 Flujos de negocio / secuencia recomendada**. No use Runner; envíe cada request con **Send** y anote los resultados manuales.

### Flujo A — Descubrimiento público

Ejecute 00, 01 y 02; luego PUB-08 a PUB-11 para comprobar publicados, borradores y eventos por artista. `venueActiveCode`, `artistId` y `artistStageName` deben confirmarse con respuestas reales. PUB-11 debe ejecutarse sobre la semilla limpia y devolver `[]`.

### Flujo B — Registro y JWT

Configure `userAEmail`/`userAUsername`, `userBEmail`/`userBUsername`, `userAPassword` y `userBPassword`; los datos deben tener prefijos únicos. Configure también `staffIdentifier`, `adminIdentifier`, `staffPassword` y `adminPassword`. Ejecute registro, login y `/me` para USER_A, USER_B, STAFF y ADMIN; copie los JWT solo a `userAToken`, `userBToken`, `staffToken` y `adminToken`. Para AUTH-22 / TC-AUTH-013, active temporalmente `e2e-inactive`, copie el token a `inactivePriorToken` y desactive la cuenta antes de enviar el caso. Para TC-SEC-017, revoque y restaure el rol STAFF como se indica en `fixtures/README.md`.

### Flujo C — Gestión del catálogo (ADMIN)

Con el mismo `runId`, cree el evento, léalo como ADMIN, asocie el artista, publíquelo y compruebe su aparición en `/api/events/published` y `/api/events/by-artist`. Ejecute las verificaciones de borrador: PUB-11 antes de asociar artistas; después EVT-30 asocia un artista al borrador sembrado, TC-SEC-005 comprueba que el anónimo no lo recibe y EVT-31 que STAFF sí puede verlo.

### Flujo D — Compra, propiedad, cancelación y uso

Compre dos tickets distintos y copie sus códigos a `ticketCancelableCode` (T1) y `ticketUsableCode` (T2). Verifique lecturas y propiedad antes de cualquier transición; después de cada denegación sobre un recurso mutable, ejecute la lectura de estado indicada. Cancele T1 y marque T2 como usado; no reutilice el mismo ticket para ambas transiciones.

### Flujo E — Regresión de errores

Ejecute los escenarios de validación 400, 401, 403, 404 y 409 y compruebe `ErrorResponse`, sin datos internos. Las pruebas de JWT usan `tamperedToken`, `expiredToken` y `wrongKeyToken`; el último debe firmarse offline con una clave desechable, nunca con el secreto real.

### Preparación de variables y registro

- Ajuste los emails/usernames de A y B con el prefijo `runId`; si se ejecutan los casos adicionales de USER_C, configure también `userCEmail`, `userCUsername` y `userCPassword`.
- Configure `expectedIssuer` y `expectedTokenTtlSeconds` según la API (por defecto `pulsepass-api` y `900`), y mantenga `expectedForeignResourceStatus=403`.
- Copie los identificadores de respuestas a `venueActiveCode`, `artistId`, `artistStageName`, `publishedEventCode`, `draftEventCode`, `ticketCancelableCode` y `ticketUsableCode`; no suponga IDs de artista.
- Ejecute las lecturas `TC-STATE-*` inmediatamente después de la operación relacionada.

Cada request incluye `Accept: application/json`. Revise el JSON esperado en su descripción. En login compruebe `tokenType=Bearer`, `expiresIn` y los claims `sub`, `iss`, `iat`, `exp` y `jti`; decodifique localmente y nunca registre el token.

Marque cada caso en `checklist-e2e-manual.md` y complete `registro-ejecucion-manual.md`.

Al registrar resultados, use únicamente `PASS`, `FAIL`, `BLOCKED` o `NOT RUN`. Para cada request anote fecha/hora, actor, HTTP observado y un resumen del JSON/estado, sin incluir credenciales ni tokens.

## Política de respuestas verificada contra el código

| Situación | Resultado |
|---|---|
| Sin token / token inválido en ruta protegida por URL | 401 |
| Rol insuficiente o recurso ajeno | 403 |
| Recurso inexistente consultado por un no autorizado (ticket, usuario, evento) | 403 (anti-enumeración); STAFF/ADMIN con propósito reciben 404 |
| Anónimo sobre evento DRAFT (ruta pública, regla de método) | 403 |
| Violación de regla de negocio | 409 |
| Validación de body | 400 |
| `X-Admin-Purpose` | Solo con rol ADMIN: `CUSTOMER_SUPPORT` (usuarios) y `TICKET_SUPPORT` (tickets) |

## Limitaciones

- Los códigos esperados salen de la lectura del código y de la validación de la semilla contra el esquema Flyway real (V1–V4) en PostgreSQL 16. **La colección no se ha ejecutado contra la app en marcha**; cualquier diferencia observada debe anotarse como desviación y revisarse (código o expectativa).
- Los casos TC-AUTH-013 y TC-SEC-017 requieren cambios temporales en la base dedicada; aplique las instrucciones de activación, revocación y restauración de `fixtures/README.md`.
- No se crean endpoints ni se altera el contrato de la API para acomodar la colección.

## Seguridad

No guardar contraseñas, tokens ni hashes reales en el repositorio, capturas o exportaciones. Datos de prueba solo con dominio `example.test`. Nunca apuntar estas pruebas a dev/QA/producción.
