# Pruebas E2E manuales con Postman (Fase 5)

Pruebas manuales de extremo a extremo de la API PulsePass (controladores + seguridad JWT/RBAC). **Sin scripts ni automatización**: ni Pre-request, ni Tests, ni Runner, ni Newman. Cada request se envía con **Send** y el resultado se anota a mano.

## Contenido

| Archivo | Propósito |
|---|---|
| `postman/PulsePass-Manual-E2E.postman_collection.json` | Colección v2.1: 148 requests en 9 carpetas por módulo |
| `postman/PulsePass-local.postman_environment.json` | Plantilla de entorno (secretos y tokens vacíos) |
| `fixtures/seed-e2e.sql`, `fixtures/README.md` | Datos E2E y cómo cargarlos |
| `checklist-e2e-manual.md` | Lista de verificación por caso |
| `registro-ejecucion-manual.md` | Plantilla para registrar resultados |
| `matriz-cobertura.md` | Cobertura por endpoint, rol y tema de seguridad |

## Preparación

1. Cree la base **`pulsepass_e2e`** (distinta de dev/QA/producción).
2. Arranque la app contra esa base con su `.env` local (Flyway aplica V1–V4). Local: `http://localhost:8080` (HTTP solo se acepta en localhost).
3. Cargue la semilla (ver `fixtures/README.md`).
4. En Postman importe la colección y el entorno. **Duplique el entorno**, complételo localmente y no lo exporte ni lo suba: guarda tokens y contraseñas. El `.gitignore` de esta carpeta excluye `*.local.postman_environment.json`.
5. Defina `runId` (p. ej. `e2e-20261019-1`), `futureDate` (fecha futura) y las contraseñas.

## Ejecución

Las carpetas están agrupadas por módulo: **Venues, Artists, Auth, Usuarios, Events, Tickets, Seguridad, Validaciones y Flujos**. La colección es manual: no use Runner; siga el orden de casos del checklist y ejecute inmediatamente las lecturas `TC-STATE-*` que verifican efectos o ausencia de efectos:

- Ajuste `userAEmail`/`userAUsername`, `userBEmail`/`userBUsername` y `userCEmail`/`userCUsername` para que compartan el prefijo elegido en `runId`; no se derivan automáticamente.
- Desde PUB-04 copie el `id` y `stageName` reales a `artistId` y `artistStageName`. Elija `missingArtistId` que no aparezca en esa respuesta. No suponga IDs de artistas.
- Configure `expectedIssuer` y `expectedTokenTtlSeconds` según la configuración local (por defecto `pulsepass-api` y `900`); mantenga `expectedForeignResourceStatus=403`, que es el resultado esperado del contrato.
- Tras AUTH-09/11/12/13/17 copie `accessToken` a `userAToken`, `userBToken`, `staffToken`, `adminToken` y `userCToken`.
- **AUTH-22 / TC-AUTH-013**: active temporalmente `e2e-inactive`, guarde el token de AUTH-22 como `inactivePriorToken` y desactive la cuenta antes de TC-AUTH-013. Consulte el SQL en `fixtures/README.md`.
- **TC-SEC-017**: después de guardar `staffToken`, quite temporalmente el rol `STAFF` a `e2e-staff` y restáurelo inmediatamente después.
- **Flujo C / TC-BIZ-003/004/005**: use el mismo `runId` para crear y consultar el evento de EVT-04, publicarlo en EVT-19, asociar el artista en EVT-25 y verificarlo en los listados.
- Tras TKT-04/05 copie `ticketCode` a `ticketT1`/`ticketT2`. Ejecute TC-STATE-TKT-06 después de TKT-06, TC-STATE-TRN-01 después de TRN-01, y TC-STATE-TRN-06/12 inmediatamente tras cada transición exitosa; TC-STATE-EVT-02 va después de EVT-02 y antes de EVT-04.
- T1 y T2 se usan por separado: T1 se cancela y T2 se marca usado; ambas transiciones son irreversibles.
- Casos de token (carpeta Seguridad):
  - `tamperedToken`: `userAToken` con un carácter cambiado a mano.
  - `expiredToken`: use un TTL corto solo en el entorno local de pruebas, ajuste `expectedTokenTtlSeconds` al mismo valor, haga login y espere a que expire. Restaure la configuración y el valor esperado después.
  - `wrongKeyToken`: firme un JWT HS256 offline con una clave desechable; nunca use la clave real.

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
- El caso ERR-04 (ruta inexistente) puede devolver 403 o 404 según la configuración; anótelo.
- No se crean endpoints ni se altera el contrato de la API para acomodar la colección.

## Seguridad

No guardar contraseñas, tokens ni hashes reales en el repositorio, capturas o exportaciones. Datos de prueba solo con dominio `example.test`. Nunca apuntar estas pruebas a dev/QA/producción.
