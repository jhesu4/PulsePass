# Pruebas E2E manuales con Postman (Fase 5)

Pruebas manuales de extremo a extremo de la API PulsePass (controladores + seguridad JWT/RBAC). **Sin scripts ni automatización**: ni Pre-request, ni Tests, ni Runner, ni Newman. Cada request se envía con **Send** y el resultado se anota a mano.

## Contenido

| Archivo | Propósito |
|---|---|
| `postman/PulsePass-Manual-E2E.postman_collection.json` | Colección v2.1: 137 requests en 9 carpetas (00–08) |
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

Siga el orden de carpetas **00 → 08**; algunas dependen de pasos previos:

- **01**: tras AUTH-09/11/12/13/17 copie manualmente cada `accessToken` a `tokenUserA`, `tokenUserB`, `tokenStaff`, `tokenAdmin`, `tokenUserC`.
- **05**: tras TKT-04 y TKT-05 copie `ticketCode` a `ticketT1` y `ticketT2`.
- **06** usa T1 (se cancela) y T2 (se marca usado) por separado, porque las transiciones son irreversibles.
- Casos de token (carpeta 02):
  - `tokenTampered`: `tokenUserA` con un carácter cambiado a mano.
  - `tokenExpired`: arranque la app con un TTL de JWT corto (según `JwtProperties`/`.env`), haga login y espere a que expire.
  - `tokenWrongKey`: firme un JWT HS256 offline con una clave desechable; nunca use la clave real.

Marque cada caso en `checklist-e2e-manual.md` y complete `registro-ejecucion-manual.md`.

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
- El caso ERR-04 (ruta inexistente) puede devolver 403 o 404 según la configuración; anótelo.
- No se crean endpoints ni se altera el contrato de la API para acomodar la colección.

## Seguridad

No guardar contraseñas, tokens ni hashes reales en el repositorio, capturas o exportaciones. Datos de prueba solo con dominio `example.test`. Nunca apuntar estas pruebas a dev/QA/producción.
