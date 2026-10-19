# Registro de ejecución manual

Copiar esta plantilla por cada ejecución. **No pegar tokens, contraseñas ni hashes.**

| Campo | Valor |
|---|---|
| Fecha | |
| Ejecutor | |
| `runId` | |
| Commit / versión | |
| Base de datos | pulsepass_e2e |
| Versión de Postman | |

## Resultados

Resultado: `OK` (coincide), `FAIL` (difiere), `N/A` (no ejecutado, con motivo).

| Caso | Descripción | Esperado | Obtenido | Resultado | Notas / evidencia |
|---|---|---|---|---|---|
| PUB-01 | Listar venues activos | 200 | | | |
| PUB-02 | Venue por código | 200 | | | |
| PUB-03 | Venue inexistente | 404 | | | |
| PUB-04 | Listar artistas activos | 200 | | | |
| PUB-05 | Artista por id | 200 | | | |
| PUB-06 | Artista por nombre artístico | 200 | | | |
| PUB-07 | Artista inexistente | 404 | | | |
| PUB-08 | Eventos publicados | 200 | | | |
| PUB-09 | Evento PUBLISHED por código (anónimo) | 200 | | | |
| PUB-10 | Evento DRAFT por código (anónimo) | 403 | | | |
| PUB-11 | Eventos por artista | 200 | | | |
| AUTH-01 | Registrar usuario A | 201 | | | |
| AUTH-02 | Registrar usuario B | 201 | | | |
| AUTH-03 | Registro duplicado (mismo email) | 409 | | | |
| AUTH-04 | Registro con password débil (<12) | 400 | | | |
| AUTH-05 | Registro sin campos obligatorios | 400 | | | |
| AUTH-06 | Registro con username con @ | 400 | | | |
| AUTH-07 | Registro con fecha de nacimiento futura | 409 | | | |
| AUTH-08 | Inyección de rol en registro público | 201 | | | |
| AUTH-09 | Login A por username | 200 | | | |
| AUTH-10 | Login A por email | 200 | | | |
| AUTH-11 | Login B | 200 | | | |
| AUTH-12 | Login STAFF | 200 | | | |
| AUTH-13 | Login ADMIN | 200 | | | |
| AUTH-14 | Login con contraseña incorrecta | 401 | | | |
| AUTH-15 | Login con usuario inexistente | 401 | | | |
| AUTH-16 | Login de usuario inactivo | 401 | | | |
| AUTH-17 | Login usuario C (verificación de roles) | 200 | | | |
| AUTH-18 | Perfil actual usuario C (roles) | 200 | | | |
| AUTH-19 | Perfil actual A | 200 | | | |
| AUTH-20 | Perfil actual STAFF | 200 | | | |
| AUTH-21 | Perfil actual ADMIN | 200 | | | |
| JWT-01 | /me sin token | 401 | | | |
| JWT-02 | /me con token malformado | 401 | | | |
| JWT-03 | /me con token alterado | 401 | | | |
| JWT-04 | /me con token expirado | 401 | | | |
| JWT-05 | /me con token firmado con otra clave | 401 | | | |
| JWT-06 | Esquema Authorization distinto de Bearer | 401 | | | |
| JWT-07 | Token en query string (no soportado) | 401 | | | |
| JWT-08 | Token válido sobre endpoint protegido de otro rol | 403 | | | |
| EVT-01 | Crear evento sin token | 401 | | | |
| EVT-02 | Crear evento como USER | 403 | | | |
| EVT-03 | Crear evento como STAFF | 403 | | | |
| EVT-04 | Crear evento como ADMIN | 201 | | | |
| EVT-05 | Crear evento con código duplicado | 409 | | | |
| EVT-06 | Crear evento con body inválido | 400 | | | |
| EVT-07 | Crear evento con fecha pasada | 409 | | | |
| EVT-08 | Crear evento en venue inactivo | 409 | | | |
| EVT-09 | Crear evento en venue inexistente | 404 | | | |
| EVT-10 | Categoría inválida | 400 | | | |
| EVT-11 | Leer evento DRAFT como USER | 403 | | | |
| EVT-12 | Leer evento DRAFT como STAFF | 200 | | | |
| EVT-13 | Leer evento DRAFT como ADMIN | 200 | | | |
| EVT-14 | Leer evento inexistente como USER | 403 | | | |
| EVT-15 | Leer evento inexistente como STAFF | 404 | | | |
| EVT-16 | Publicar como USER | 403 | | | |
| EVT-17 | Publicar como STAFF | 403 | | | |
| EVT-18 | Publicar sin token | 401 | | | |
| EVT-19 | Publicar como ADMIN | 200 | | | |
| EVT-20 | Publicar de nuevo (ya PUBLISHED) | 409 | | | |
| EVT-21 | Publicar evento con fecha pasada | 409 | | | |
| EVT-22 | Publicar evento inexistente | 404 | | | |
| EVT-23 | Asociar artista como USER | 403 | | | |
| EVT-24 | Asociar artista como STAFF | 403 | | | |
| EVT-25 | Asociar artista como ADMIN | 200 | | | |
| EVT-26 | Asociar el mismo artista otra vez | 409 | | | |
| EVT-27 | Asociar artista inexistente | 404 | | | |
| EVT-28 | Asociar artista a evento inexistente | 404 | | | |
| EVT-29 | Eventos por artista tras asociación | 200 | | | |
| USR-01 | Mi usuario por email | 200 | | | |
| USR-02 | Mi usuario por username | 200 | | | |
| USR-03 | Usuario ajeno por email (IDOR) | 403 | | | |
| USR-04 | Usuario ajeno por username (IDOR) | 403 | | | |
| USR-05 | Email inexistente como USER | 403 | | | |
| USR-06 | Usuario ajeno como STAFF | 403 | | | |
| USR-07 | Usuario ajeno como ADMIN sin propósito | 403 | | | |
| USR-08 | Usuario ajeno como ADMIN con propósito CUSTOMER_SUPPORT | 200 | | | |
| USR-09 | ADMIN con propósito incorrecto (TICKET_SUPPORT) | 403 | | | |
| USR-10 | USER intenta usar X-Admin-Purpose | 403 | | | |
| USR-11 | ADMIN con propósito, usuario inexistente | 404 | | | |
| USR-12 | Consulta de usuario sin token | 401 | | | |
| USR-13 | Respuesta no expone credenciales | 200 | | | |
| TKT-01 | Comprar sin token | 401 | | | |
| TKT-02 | Comprar como STAFF | 403 | | | |
| TKT-03 | Comprar como ADMIN | 403 | | | |
| TKT-04 | A compra ticket T1 para sí | 201 | | | |
| TKT-05 | A compra ticket T2 para sí | 201 | | | |
| TKT-06 | A compra a nombre de B (IDOR) | 403 | | | |
| TKT-07 | A compra a nombre de email inexistente | 403 | | | |
| TKT-08 | Comprar con body inválido | 400 | | | |
| TKT-09 | Comprar tipo inválido | 400 | | | |
| TKT-10 | Comprar evento DRAFT | 409 | | | |
| TKT-11 | Comprar evento con fecha pasada | 409 | | | |
| TKT-12 | Comprar evento con edad mínima no cumplida | 409 | | | |
| TKT-13 | Comprar evento inexistente | 404 | | | |
| TKT-14 | Aforo: A compra la última entrada | 201 | | | |
| TKT-15 | Aforo: B intenta comprar sin cupo | 409 | | | |
| TKT-16 | A lee su ticket | 200 | | | |
| TKT-17 | B lee ticket de A (IDOR) | 403 | | | |
| TKT-18 | STAFF lee ticket de A | 200 | | | |
| TKT-19 | ADMIN lee ticket de A sin propósito | 403 | | | |
| TKT-20 | ADMIN lee ticket de A con TICKET_SUPPORT | 200 | | | |
| TKT-21 | ADMIN con propósito incorrecto (CUSTOMER_SUPPORT) | 403 | | | |
| TKT-22 | Ticket inexistente como USER | 403 | | | |
| TKT-23 | Ticket inexistente como STAFF | 404 | | | |
| TKT-24 | Leer ticket sin token | 401 | | | |
| TKT-25 | A lista sus tickets | 200 | | | |
| TKT-26 | B lista tickets de A (IDOR) | 403 | | | |
| TKT-27 | STAFF lista tickets de A | 403 | | | |
| TKT-28 | ADMIN con TICKET_SUPPORT lista tickets de A | 200 | | | |
| TKT-29 | ADMIN sin propósito lista tickets | 403 | | | |
| TKT-30 | Listar tickets sin token | 401 | | | |
| TRN-01 | Cancelar T1 como B (IDOR) | 403 | | | |
| TRN-02 | Cancelar T1 como STAFF | 403 | | | |
| TRN-03 | Cancelar T1 sin token | 401 | | | |
| TRN-04 | Cancelar T1 como ADMIN sin propósito | 403 | | | |
| TRN-05 | Cancelar ticket inexistente como USER | 403 | | | |
| TRN-06 | A cancela su ticket T1 | 200 | | | |
| TRN-07 | Cancelar T1 de nuevo | 409 | | | |
| TRN-08 | Usar T1 cancelado | 409 | | | |
| TRN-09 | Usar T2 como USER | 403 | | | |
| TRN-10 | Usar T2 sin token | 401 | | | |
| TRN-11 | Usar ticket inexistente como STAFF | 404 | | | |
| TRN-12 | STAFF marca T2 como usado | 200 | | | |
| TRN-13 | Usar T2 de nuevo | 409 | | | |
| TRN-14 | Cancelar T2 ya usado | 409 | | | |
| TRN-15 | ADMIN marca ticket como usado (ya USED) | 409 | | | |
| PAID-01 | Listar PAID como USER | 403 | | | |
| PAID-02 | Listar PAID sin token | 401 | | | |
| PAID-03 | Listar PAID como STAFF | 200 | | | |
| PAID-04 | Listar PAID como ADMIN | 200 | | | |
| PAID-05 | Listar PAID de evento inexistente como STAFF | 404 | | | |
| ERR-01 | Método no permitido | 405 | | | |
| ERR-02 | Content-Type no soportado | 415 | | | |
| ERR-03 | JSON malformado | 400 | | | |
| ERR-04 | Ruta inexistente autenticada | 404 | | | |
| ERR-05 | Errores sin trazas internas | 404 | | | |

## Defectos encontrados

| # | Caso | Severidad | Descripción | Estado |
|---|---|---|---|---|
| | | | | |

## Conclusión

- Casos ejecutados / totales:
- Aprobados / fallidos:
- Decisión:
