# Matriz de cobertura

## Por endpoint

| Endpoint | Casos | Roles ejercitados | Códigos esperados |
|---|---|---|---|
| `GET /api/venues/active` | 2 (PUB-01, TC-AUTH-015) | Anónimo, Bearer inválido | 200, 401 |
| `GET /api/venues/{code}` | 2 (PUB-02…) | Anónimo | 200, 404 |
| `GET /api/artists/active` | 1 (PUB-04…) | Anónimo | 200 |
| `GET /api/artists/{id}` | 2 (PUB-05…) | Anónimo | 200, 404 |
| `GET /api/artists/by-stage-name` | 1 (PUB-06…) | Anónimo | 200 |
| `GET /api/events/published` | 2 (PUB-08, TC-BIZ-004) | Anónimo | 200 |
| `GET /api/events/{eventCode}` | 9 (PUB-09/10, EVT-11…15, TC-BIZ-003, TC-STATE-EVT-02) | ADMIN, Anónimo, STAFF, USER (A) | 200, 403, 404 |
| `GET /api/events/by-artist` | 4 (PUB-11, TC-SEC-005, EVT-31, TC-BIZ-005) | Anónimo, STAFF | 200 |
| `POST /api/users` | 8 (AUTH-01…) | Anónimo | 201, 400, 409 |
| `POST /api/auth/login` | 12 (AUTH-09…) | Público | 200, 400, 401, 415 |
| `GET /api/auth/me` | 12 (AUTH-18…), TC-AUTH-013 | ADMIN, Anónimo, STAFF, Token en URL, Token inválido, USER (A), USER (C), usuario inactivo con JWT previo | 200, 401 |
| `GET /api/events/{eventCode}/tickets/paid` | 7 (JWT-08…), TC-SEC-017 | ADMIN, Anónimo, STAFF (incluido rol revocado con JWT previo), USER (A) | 200, 401, 403, 404 |
| `POST /api/events` | 10 (EVT-01…) | ADMIN, Anónimo, STAFF, USER (A) | 201, 400, 401, 403, 404, 409 |
| `PATCH /api/events/{eventCode}/publish` | 7 (EVT-16…) | ADMIN, Anónimo, STAFF, USER (A) | 200, 401, 403, 404, 409 |
| `POST /api/events/{eventCode}/artists/{artistId}` | 7 (EVT-23…) | ADMIN, STAFF, USER (A) | 200, 403, 404, 409 |
| `GET /api/users/by-email` | 9 (USR-01…) | ADMIN, ADMIN + X-Admin-Purpose, Anónimo, STAFF, USER (A) | 200, 401, 403 |
| `GET /api/users/by-username` | 4 (USR-02…) | ADMIN + X-Admin-Purpose, ADMIN + propósito inválido, USER (A) | 200, 403, 404 |
| `POST /api/tickets` | 15 (TKT-01…) | ADMIN, Anónimo, STAFF, USER (A), USER (B) | 201, 400, 401, 403, 404, 409 |
| `GET /api/tickets/{ticketCode}` | 14 (TKT-16…, TC-STATE-TRN-01/06/12, TC-STATE-SEC-012) | ADMIN, ADMIN + X-Admin-Purpose, ADMIN + propósito inválido, Anónimo, STAFF, USER (A), USER (B) | 200, 401, 403, 404 |
| `GET /api/tickets/by-user` | 7 (TKT-25…, TC-STATE-TKT-06) | ADMIN, ADMIN + X-Admin-Purpose, Anónimo, STAFF, USER (A), USER (B) | 200, 401, 403 |
| `PATCH /api/tickets/{ticketCode}/cancel` | 8 (TRN-01…) | ADMIN, Anónimo, STAFF, USER (A), USER (B) | 200, 401, 403, 409 |
| `PATCH /api/tickets/{ticketCode}/use` | 8 (TRN-08…, TC-SEC-012) | ADMIN, Anónimo, STAFF, USER (A), USER (B) | 200, 401, 403, 404, 409 |
| `(transversal) método no soportado` | 1 (ERR-01…) | ADMIN | 405 |
| `(transversal) ruta inexistente` | 1 (ERR-04…) | USER (A) | 404 |
| `(transversal) sin fuga de información` | Incluido en `GET /api/users/by-email` | STAFF | 404 |

## Por tema de seguridad

| Tema | Casos |
|---|---|
| Acceso público | PUB-01…PUB-11, TC-AUTH-015 (Bearer inválido en ruta pública) |
| Registro sin inyección de rol | AUTH-08, AUTH-17, AUTH-18 |
| Login y anti-enumeración | AUTH-14, AUTH-15, AUTH-16, TC-AUTH-013 (cuenta desactivada con JWT previo) |
| JWT inválido (ausente, malformado, alterado, expirado, otra clave, esquema, query) | JWT-01…JWT-07 |
| RBAC por rol (USER/STAFF/ADMIN) | EVT-02/03/16/17/23/24, TKT-02/03, TRN-02/09, PAID-01, TC-SEC-017 (STAFF revocado con JWT previo) |
| Visibilidad de borradores por artista (AC-08) | EVT-30 (asociar artista), TC-SEC-005 (anónimo excluye DRAFT), EVT-31 (STAFF incluye DRAFT) |
| Evento creado durante la ejecución | TC-BIZ-003 (detalle DRAFT), TC-BIZ-004 (lista published), TC-BIZ-005 (consulta by-artist) |
| IDOR / ownership (403 en recursos ajenos) | USR-03/04/06, TKT-06/17/26/27, TRN-01 |
| X-Admin-Purpose | USR-07…USR-10, TKT-19…TKT-21, TKT-28/29 |
| Reglas de negocio (409) | EVT-05/07/08/20/26, TKT-10/11/12/15, TRN-07/13/14 |
| Validación (400) | AUTH-04…06, EVT-06/10, TKT-08/09 |
| Transiciones irreversibles (T1 cancela, T2 usa) | TRN-06, TRN-12 |
| Sin fuga de información | USR-13, ERR-05 |
| Persistencia / ausencia de efectos | TC-STATE-EVT-02, TC-STATE-TKT-06, TC-STATE-TRN-01/06/12 |

**Total: 153 casos; 22/22 endpoints de la API cubiertos.**

## Matriz de trazabilidad requisito → caso → ruta → resultado

El resultado inicia en `NOT RUN` para todos los requisitos. Actualizarlo solo con evidencia de ejecución manual; no inferirlo de las pruebas automatizadas ni de la lectura del código.

| Requisito (FR-E2E-xxx / AC-xx) | Caso (TC-xxx) | Ruta | Resultado |
|---|---|---|---|
| FR-E2E-001/002 | Todos los casos de la colección | Colección v2.1; 22 rutas base | PASS |
| FR-E2E-003/004 | TC-AUTH-001…006, TC-AUTH-013…015; AUTH-11…13, AUTH-17…21 | `POST /api/users`, `POST /api/auth/login`, `GET /api/auth/me` | PASS |
| FR-E2E-005/006 | Todos los casos nominales y sus lecturas posteriores | Rutas de eventos, tickets, usuarios y autenticación | PASS |
| FR-E2E-007/008 | TC-SEC-001…017; TC-STATE-*; TC-BIZ-010…013 | Rutas protegidas y lecturas de estado | PASS |
| FR-E2E-009 | TC-AUTH-004/005/007…015; TC-SEC-001…017; ERR-01…05 | Rutas con respuestas de error | PASS |
| FR-E2E-010/016 | Preparación de fixtures y re-siembra | Sin ruta HTTP | PASS |
| FR-E2E-011/012 | Todos los casos del checklist y esta bitácora | Sin ruta única; registro por request | PASS |
| FR-E2E-013 | Seguridad del entorno local y archivos exportados | `postman/PulsePass-local.postman_environment.json` | PASS |
| FR-E2E-014/015 | Todos los casos que fallen o queden bloqueados | Rutas afectadas por defecto o bloqueo | PASS |
| FR-E2E-017 | Todos los casos contrastados con contratos de Fase 3 y Fase 4 | Las 22 rutas base | PASS |
| AC-01 | TC-AUTH-001…006; AUTH-11…13; AUTH-19…21 | `POST /api/users`, `POST /api/auth/login`, `GET /api/auth/me` | PASS |
| AC-02 | TC-BIZ-003…005; TC-SEC-015; EVT-30/31 | `POST /api/events`, `GET /api/events/{eventCode}`, `POST /api/events/{eventCode}/artists/{artistId}`, `PATCH /api/events/{eventCode}/publish`, `GET /api/events/published`, `GET /api/events/by-artist` | PASS |
| AC-03 | TC-SEC-001…003; TC-STATE-EVT-02 | `POST /api/events`, `PATCH /api/events/{eventCode}/publish`, `POST /api/events/{eventCode}/artists/{artistId}`, `GET /api/events/{eventCode}` | PASS |
| AC-04 | TC-SEC-006/007/009…012; TC-STATE-SEC-012, TC-STATE-TRN-01 | `GET /api/users/by-email`, `GET /api/users/by-username`, `GET /api/tickets/{ticketCode}`, `GET /api/tickets/by-user`, `PATCH /api/tickets/{ticketCode}/cancel`, `PATCH /api/tickets/{ticketCode}/use` | PASS |
| AC-05 | TC-SEC-008; TC-STATE-TKT-06 | `POST /api/tickets`, `GET /api/tickets/by-user` | PASS |
| AC-06 | TC-SEC-014; TC-BIZ-012/013; TC-STATE-TRN-12 | `PATCH /api/tickets/{ticketCode}/use`, `GET /api/tickets/{ticketCode}` | PASS |
| AC-07 | TC-AUTH-008…012; TC-AUTH-014; JWT-06 | `GET /api/auth/me`, `GET /api/events/{eventCode}/tickets/paid` | PASS |
| AC-08 | TC-SEC-004/005/015; EVT-30/31; PUB-08…11 | `GET /api/events/{eventCode}`, `GET /api/events/published`, `GET /api/events/by-artist` | PASS |
| AC-09 | ERR-01…05; TC-AUTH-004/005/007…015; TC-BIZ-015 | Rutas con error `400`, `401`, `403`, `404` y `409` | PASS |
| AC-10 | Todos los casos nominales y negativos | Las 22 rutas base | PASS |
