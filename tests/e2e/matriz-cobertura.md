# Matriz de cobertura

## Por endpoint

| Endpoint | Casos | Roles ejercitados | Códigos esperados |
|---|---|---|---|
| `GET /api/venues/active` | 1 (PUB-01…) | Anónimo | 200 |
| `GET /api/venues/{code}` | 2 (PUB-02…) | Anónimo | 200, 404 |
| `GET /api/artists/active` | 1 (PUB-04…) | Anónimo | 200 |
| `GET /api/artists/{id}` | 2 (PUB-05…) | Anónimo | 200, 404 |
| `GET /api/artists/by-stage-name` | 1 (PUB-06…) | Anónimo | 200 |
| `GET /api/events/published` | 1 (PUB-08…) | Anónimo | 200 |
| `GET /api/events/{eventCode}` | 7 (PUB-09…) | ADMIN, Anónimo, STAFF, USER (A) | 200, 403, 404 |
| `GET /api/events/by-artist` | 2 (PUB-11…) | Anónimo | 200 |
| `POST /api/users` | 8 (AUTH-01…) | Anónimo | 201, 400, 409 |
| `POST /api/auth/login` | 11 (AUTH-09…) | Público | 200, 400, 401, 415 |
| `GET /api/auth/me` | 11 (AUTH-18…) | ADMIN, Anónimo, STAFF, Token en URL, Token inválido, USER (A), USER (C) | 200, 401 |
| `GET /api/events/{eventCode}/tickets/paid` | 6 (JWT-08…) | ADMIN, Anónimo, STAFF, USER (A) | 200, 401, 403, 404 |
| `POST /api/events` | 10 (EVT-01…) | ADMIN, Anónimo, STAFF, USER (A) | 201, 400, 401, 403, 404, 409 |
| `PATCH /api/events/{eventCode}/publish` | 7 (EVT-16…) | ADMIN, Anónimo, STAFF, USER (A) | 200, 401, 403, 404, 409 |
| `POST /api/events/{eventCode}/artists/{artistId}` | 6 (EVT-23…) | ADMIN, STAFF, USER (A) | 200, 403, 404, 409 |
| `GET /api/users/by-email` | 9 (USR-01…) | ADMIN, ADMIN + X-Admin-Purpose, Anónimo, STAFF, USER (A) | 200, 401, 403 |
| `GET /api/users/by-username` | 4 (USR-02…) | ADMIN + X-Admin-Purpose, ADMIN + propósito inválido, USER (A) | 200, 403, 404 |
| `POST /api/tickets` | 15 (TKT-01…) | ADMIN, Anónimo, STAFF, USER (A), USER (B) | 201, 400, 401, 403, 404, 409 |
| `GET /api/tickets/{ticketCode}` | 9 (TKT-16…) | ADMIN, ADMIN + X-Admin-Purpose, ADMIN + propósito inválido, Anónimo, STAFF, USER (A), USER (B) | 200, 401, 403, 404 |
| `GET /api/tickets/by-user` | 6 (TKT-25…) | ADMIN, ADMIN + X-Admin-Purpose, Anónimo, STAFF, USER (A), USER (B) | 200, 401, 403 |
| `PATCH /api/tickets/{ticketCode}/cancel` | 8 (TRN-01…) | ADMIN, Anónimo, STAFF, USER (A), USER (B) | 200, 401, 403, 409 |
| `PATCH /api/tickets/{ticketCode}/use` | 7 (TRN-08…) | ADMIN, Anónimo, STAFF, USER (A) | 200, 401, 403, 404, 409 |
| `(transversal) método no soportado` | 1 (ERR-01…) | ADMIN | 405 |
| `(transversal) ruta inexistente` | 1 (ERR-04…) | USER (A) | 404 |
| `(transversal) sin fuga de información` | 1 (ERR-05…) | STAFF | 404 |

## Por tema de seguridad

| Tema | Casos |
|---|---|
| Acceso público | PUB-01…PUB-11 |
| Registro sin inyección de rol | AUTH-08, AUTH-17, AUTH-18 |
| Login y anti-enumeración | AUTH-14, AUTH-15, AUTH-16 |
| JWT inválido (ausente, malformado, alterado, expirado, otra clave, esquema, query) | JWT-01…JWT-07 |
| RBAC por rol (USER/STAFF/ADMIN) | EVT-02/03/16/17/23/24, TKT-02/03, TRN-02/09, PAID-01 |
| IDOR / ownership (403 en recursos ajenos) | USR-03/04/06, TKT-06/17/26/27, TRN-01 |
| X-Admin-Purpose | USR-07…USR-10, TKT-19…TKT-21, TKT-28/29 |
| Reglas de negocio (409) | EVT-05/07/08/20/26, TKT-10/11/12/15, TRN-07/13/14 |
| Validación (400) | AUTH-04…06, EVT-06/10, TKT-08/09 |
| Transiciones irreversibles (T1 cancela, T2 usa) | TRN-06, TRN-12 |
| Sin fuga de información | USR-13, ERR-05 |

**Total: 137 casos; 22/22 endpoints de la API cubiertos.**
