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

Resultado: `PASS` (coincide), `FAIL` (difiere), `BLOCKED` (no se pudo ejecutar por un bloqueo documentado) o `NOT RUN` (todavía no ejecutado).

Por cada request registre fecha y hora ISO-8601, actor/rol, código HTTP observado y un resumen del JSON o estado observado. No incluya contraseñas, JWT ni hashes.

| Caso | Descripción | Actor | Esperado (HTTP) | Fecha/hora (ISO-8601) | HTTP observado | JSON/estado observado | Resultado | Notas / evidencia |
|---|---|---|---:|---|---:|---|---|---|
| PUB-01 | Listar venues activos |  | 200 |  |  |  | NOT RUN |  |
| PUB-02 | Venue por código |  | 200 |  |  |  | NOT RUN |  |
| PUB-03 | Venue inexistente |  | 404 |  |  |  | NOT RUN |  |
| PUB-04 | Listar artistas activos |  | 200 |  |  |  | NOT RUN |  |
| PUB-05 | Artista por id |  | 200 |  |  |  | NOT RUN |  |
| PUB-06 | Artista por nombre artístico |  | 200 |  |  |  | NOT RUN |  |
| PUB-07 | Artista inexistente |  | 404 |  |  |  | NOT RUN |  |
| PUB-08 | Eventos publicados |  | 200 |  |  |  | NOT RUN |  |
| PUB-09 | Evento PUBLISHED por código (anónimo) |  | 200 |  |  |  | NOT RUN |  |
| PUB-10 | Evento DRAFT por código (anónimo) |  | 403 |  |  |  | NOT RUN |  |
| PUB-11 | Eventos por artista |  | 200 |  |  |  | NOT RUN |  |
| TC-AUTH-015 | Bearer inválido en endpoint público | Token inválido (anónimo) | 401 | | | | NOT RUN | |
| AUTH-01 | Registrar usuario A |  | 201 |  |  |  | NOT RUN |  |
| AUTH-02 | Registrar usuario B |  | 201 |  |  |  | NOT RUN |  |
| AUTH-03 | Registro duplicado (mismo email) |  | 409 |  |  |  | NOT RUN |  |
| AUTH-04 | Registro con password débil (<12) |  | 400 |  |  |  | NOT RUN |  |
| AUTH-05 | Registro sin campos obligatorios |  | 400 |  |  |  | NOT RUN |  |
| AUTH-06 | Registro con username con @ |  | 400 |  |  |  | NOT RUN |  |
| AUTH-07 | Registro con fecha de nacimiento futura |  | 409 |  |  |  | NOT RUN |  |
| AUTH-08 | Inyección de rol en registro público |  | 201 |  |  |  | NOT RUN |  |
| AUTH-09 | Login A por username |  | 200 |  |  |  | NOT RUN |  |
| AUTH-10 | Login A por email |  | 200 |  |  |  | NOT RUN |  |
| AUTH-11 | Login B |  | 200 |  |  |  | NOT RUN |  |
| AUTH-12 | Login STAFF |  | 200 |  |  |  | NOT RUN |  |
| AUTH-13 | Login ADMIN |  | 200 |  |  |  | NOT RUN |  |
| AUTH-14 | Login con contraseña incorrecta |  | 401 |  |  |  | NOT RUN |  |
| AUTH-15 | Login con usuario inexistente |  | 401 |  |  |  | NOT RUN |  |
| AUTH-16 | Login de usuario inactivo |  | 401 |  |  |  | NOT RUN |  |
| AUTH-22 | Preparar TC-AUTH-013: obtener token antes de desactivar | e2e-inactive temporalmente activa | 200 | | | | NOT RUN | |
| AUTH-17 | Login usuario C (verificación de roles) |  | 200 |  |  |  | NOT RUN |  |
| AUTH-18 | Perfil actual usuario C (roles) |  | 200 |  |  |  | NOT RUN |  |
| AUTH-19 | Perfil actual A |  | 200 |  |  |  | NOT RUN |  |
| AUTH-20 | Perfil actual STAFF |  | 200 |  |  |  | NOT RUN |  |
| AUTH-21 | Perfil actual ADMIN |  | 200 |  |  |  | NOT RUN |  |
| JWT-01 | /me sin token |  | 401 |  |  |  | NOT RUN |  |
| JWT-02 | /me con token malformado |  | 401 |  |  |  | NOT RUN |  |
| JWT-03 | /me con token alterado |  | 401 |  |  |  | NOT RUN |  |
| JWT-04 | /me con token expirado |  | 401 |  |  |  | NOT RUN |  |
| JWT-05 | /me con token firmado con otra clave |  | 401 |  |  |  | NOT RUN |  |
| JWT-06 | Esquema Authorization distinto de Bearer |  | 401 |  |  |  | NOT RUN |  |
| JWT-07 | Token en query string (no soportado) |  | 401 |  |  |  | NOT RUN |  |
| JWT-08 | Token válido sobre endpoint protegido de otro rol |  | 403 |  |  |  | NOT RUN |  |
| TC-AUTH-013 | Usuario desactivado con JWT previo | Usuario inactivo con token previo | 401 | | | | NOT RUN | |
| TC-SEC-017 | Rol STAFF revocado con JWT previo | STAFF con token previo | 403 | | | | NOT RUN | |
| EVT-01 | Crear evento sin token |  | 401 |  |  |  | NOT RUN |  |
| EVT-02 | Crear evento como USER |  | 403 |  |  |  | NOT RUN |  |
| TC-STATE-EVT-02 | Confirmar que EVT-02 no creó el evento | ADMIN | 404 |  |  |  | NOT RUN | Ejecutar antes de EVT-04 |
| EVT-03 | Crear evento como STAFF |  | 403 |  |  |  | NOT RUN |  |
| EVT-04 | Crear evento como ADMIN |  | 201 |  |  |  | NOT RUN |  |
| TC-BIZ-003 | Leer el evento recién creado | ADMIN | 200 | | | | NOT RUN | Estado esperado: DRAFT |
| EVT-05 | Crear evento con código duplicado |  | 409 |  |  |  | NOT RUN |  |
| EVT-06 | Crear evento con body inválido |  | 400 |  |  |  | NOT RUN |  |
| EVT-07 | Crear evento con fecha pasada |  | 409 |  |  |  | NOT RUN |  |
| EVT-08 | Crear evento en venue inactivo |  | 409 |  |  |  | NOT RUN |  |
| EVT-09 | Crear evento en venue inexistente |  | 404 |  |  |  | NOT RUN |  |
| EVT-10 | Categoría inválida |  | 400 |  |  |  | NOT RUN |  |
| EVT-11 | Leer evento DRAFT como USER |  | 403 |  |  |  | NOT RUN |  |
| EVT-12 | Leer evento DRAFT como STAFF |  | 200 |  |  |  | NOT RUN |  |
| EVT-13 | Leer evento DRAFT como ADMIN |  | 200 |  |  |  | NOT RUN |  |
| EVT-14 | Leer evento inexistente como USER |  | 403 |  |  |  | NOT RUN |  |
| EVT-15 | Leer evento inexistente como STAFF |  | 404 |  |  |  | NOT RUN |  |
| EVT-16 | Publicar como USER |  | 403 |  |  |  | NOT RUN |  |
| EVT-17 | Publicar como STAFF |  | 403 |  |  |  | NOT RUN |  |
| EVT-18 | Publicar sin token |  | 401 |  |  |  | NOT RUN |  |
| EVT-19 | Publicar el evento creado en EVT-04 como ADMIN | ADMIN | 200 |  |  |  | NOT RUN |  |
| TC-BIZ-004 | Verificar evento creado en eventos publicados | Anónimo | 200 | | | | NOT RUN | Debe incluir el código de evento de EVT-04 |
| EVT-20 | Publicar de nuevo el evento de EVT-04 (ya PUBLISHED) | ADMIN | 409 |  |  |  | NOT RUN |  |
| EVT-21 | Publicar evento con fecha pasada |  | 409 |  |  |  | NOT RUN |  |
| EVT-22 | Publicar evento inexistente |  | 404 |  |  |  | NOT RUN |  |
| EVT-23 | Asociar artista como USER |  | 403 |  |  |  | NOT RUN |  |
| EVT-24 | Asociar artista como STAFF |  | 403 |  |  |  | NOT RUN |  |
| EVT-25 | Asociar artista al evento creado y publicado | ADMIN | 200 |  |  |  | NOT RUN |  |
| EVT-26 | Asociar el mismo artista otra vez |  | 409 |  |  |  | NOT RUN |  |
| EVT-27 | Asociar artista inexistente |  | 404 |  |  |  | NOT RUN |  |
| EVT-28 | Asociar artista a evento inexistente |  | 404 |  |  |  | NOT RUN |  |
| TC-BIZ-005 (EVT-29) | Evento creado por artista tras asociación | Anónimo | 200 |  |  |  | NOT RUN | Debe incluir el código de evento de EVT-04 |
| USR-01 | Mi usuario por email |  | 200 |  |  |  | NOT RUN |  |
| USR-02 | Mi usuario por username |  | 200 |  |  |  | NOT RUN |  |
| USR-03 | Usuario ajeno por email (IDOR) |  | 403 |  |  |  | NOT RUN |  |
| USR-04 | Usuario ajeno por username (IDOR) |  | 403 |  |  |  | NOT RUN |  |
| USR-05 | Email inexistente como USER |  | 403 |  |  |  | NOT RUN |  |
| USR-06 | Usuario ajeno como STAFF |  | 403 |  |  |  | NOT RUN |  |
| USR-07 | Usuario ajeno como ADMIN sin propósito |  | 403 |  |  |  | NOT RUN |  |
| USR-08 | Usuario ajeno como ADMIN con propósito CUSTOMER_SUPPORT |  | 200 |  |  |  | NOT RUN |  |
| USR-09 | ADMIN con propósito incorrecto (TICKET_SUPPORT) |  | 403 |  |  |  | NOT RUN |  |
| USR-10 | USER intenta usar X-Admin-Purpose |  | 403 |  |  |  | NOT RUN |  |
| USR-11 | ADMIN con propósito, usuario inexistente |  | 404 |  |  |  | NOT RUN |  |
| USR-12 | Consulta de usuario sin token |  | 401 |  |  |  | NOT RUN |  |
| USR-13 | Respuesta no expone credenciales |  | 200 |  |  |  | NOT RUN |  |
| TKT-01 | Comprar sin token |  | 401 |  |  |  | NOT RUN |  |
| TKT-02 | Comprar como STAFF |  | 403 |  |  |  | NOT RUN |  |
| TKT-03 | Comprar como ADMIN |  | 403 |  |  |  | NOT RUN |  |
| TKT-04 | A compra ticket T1 para sí |  | 201 |  |  |  | NOT RUN |  |
| TKT-05 | A compra ticket T2 para sí |  | 201 |  |  |  | NOT RUN |  |
| TKT-06 | A compra a nombre de B (IDOR) |  | 403 |  |  |  | NOT RUN |  |
| TC-STATE-TKT-06 | Confirmar que TKT-06 no creó ticket para B | USER (B) | 200 |  |  |  | NOT RUN | Respuesta esperada: [] |
| TKT-07 | A compra a nombre de email inexistente |  | 403 |  |  |  | NOT RUN |  |
| TKT-08 | Comprar con body inválido |  | 400 |  |  |  | NOT RUN |  |
| TKT-09 | Comprar tipo inválido |  | 400 |  |  |  | NOT RUN |  |
| TKT-10 | Comprar evento DRAFT |  | 409 |  |  |  | NOT RUN |  |
| TKT-11 | Comprar evento con fecha pasada |  | 409 |  |  |  | NOT RUN |  |
| TKT-12 | Comprar evento con edad mínima no cumplida |  | 409 |  |  |  | NOT RUN |  |
| TKT-13 | Comprar evento inexistente |  | 404 |  |  |  | NOT RUN |  |
| TKT-14 | Aforo: A compra la última entrada |  | 201 |  |  |  | NOT RUN |  |
| TKT-15 | Aforo: B intenta comprar sin cupo |  | 409 |  |  |  | NOT RUN |  |
| TKT-16 | A lee su ticket |  | 200 |  |  |  | NOT RUN |  |
| TKT-17 | B lee ticket de A (IDOR) |  | 403 |  |  |  | NOT RUN |  |
| TKT-18 | STAFF lee ticket de A |  | 200 |  |  |  | NOT RUN |  |
| TKT-19 | ADMIN lee ticket de A sin propósito |  | 403 |  |  |  | NOT RUN |  |
| TKT-20 | ADMIN lee ticket de A con TICKET_SUPPORT |  | 200 |  |  |  | NOT RUN |  |
| TKT-21 | ADMIN con propósito incorrecto (CUSTOMER_SUPPORT) |  | 403 |  |  |  | NOT RUN |  |
| TKT-22 | Ticket inexistente como USER |  | 403 |  |  |  | NOT RUN |  |
| TKT-23 | Ticket inexistente como STAFF |  | 404 |  |  |  | NOT RUN |  |
| TKT-24 | Leer ticket sin token |  | 401 |  |  |  | NOT RUN |  |
| TKT-25 | A lista sus tickets |  | 200 |  |  |  | NOT RUN |  |
| TKT-26 | B lista tickets de A (IDOR) |  | 403 |  |  |  | NOT RUN |  |
| TKT-27 | STAFF lista tickets de A |  | 403 |  |  |  | NOT RUN |  |
| TKT-28 | ADMIN con TICKET_SUPPORT lista tickets de A |  | 200 |  |  |  | NOT RUN |  |
| TKT-29 | ADMIN sin propósito lista tickets |  | 403 |  |  |  | NOT RUN |  |
| TKT-30 | Listar tickets sin token |  | 401 |  |  |  | NOT RUN |  |
| TRN-01 | Cancelar T1 como B (IDOR) |  | 403 |  |  |  | NOT RUN |  |
| TC-STATE-TRN-01 | Confirmar que TRN-01 no canceló T1 | USER (A) | 200 |  |  |  | NOT RUN | T1 debe seguir PAID |
| TRN-02 | Cancelar T1 como STAFF |  | 403 |  |  |  | NOT RUN |  |
| TRN-03 | Cancelar T1 sin token |  | 401 |  |  |  | NOT RUN |  |
| TRN-04 | Cancelar T1 como ADMIN sin propósito |  | 403 |  |  |  | NOT RUN |  |
| TRN-05 | Cancelar ticket inexistente como USER |  | 403 |  |  |  | NOT RUN |  |
| TRN-06 | A cancela su ticket T1 |  | 200 |  |  |  | NOT RUN |  |
| TC-STATE-TRN-06 | Confirmar cancelación persistida de T1 | USER (A) | 200 |  |  |  | NOT RUN | Estado esperado CANCELLED |
| TRN-07 | Cancelar T1 de nuevo |  | 409 |  |  |  | NOT RUN |  |
| TRN-08 | Usar T1 cancelado |  | 409 |  |  |  | NOT RUN |  |
| TRN-09 | Usar T2 como USER |  | 403 |  |  |  | NOT RUN |  |
| TRN-10 | Usar T2 sin token |  | 401 |  |  |  | NOT RUN |  |
| TRN-11 | Usar ticket inexistente como STAFF |  | 404 |  |  |  | NOT RUN |  |
| TRN-12 | STAFF marca T2 como usado |  | 200 |  |  |  | NOT RUN |  |
| TC-STATE-TRN-12 | Confirmar estado persistido de T2 | USER (A) | 200 |  |  |  | NOT RUN | Estado esperado USED |
| TRN-13 | Usar T2 de nuevo |  | 409 |  |  |  | NOT RUN |  |
| TRN-14 | Cancelar T2 ya usado |  | 409 |  |  |  | NOT RUN |  |
| TRN-15 | ADMIN marca ticket como usado (ya USED) |  | 409 |  |  |  | NOT RUN |  |
| PAID-01 | Listar PAID como USER |  | 403 |  |  |  | NOT RUN |  |
| PAID-02 | Listar PAID sin token |  | 401 |  |  |  | NOT RUN |  |
| PAID-03 | Listar PAID como STAFF |  | 200 |  |  |  | NOT RUN |  |
| PAID-04 | Listar PAID como ADMIN |  | 200 |  |  |  | NOT RUN |  |
| PAID-05 | Listar PAID de evento inexistente como STAFF |  | 404 |  |  |  | NOT RUN |  |
| ERR-01 | Método no permitido |  | 405 |  |  |  | NOT RUN |  |
| ERR-02 | Content-Type no soportado |  | 415 |  |  |  | NOT RUN |  |
| ERR-03 | JSON malformado |  | 400 |  |  |  | NOT RUN |  |
| ERR-04 | Ruta inexistente autenticada |  | 404 |  |  |  | NOT RUN |  |
| ERR-05 | Errores sin trazas internas |  | 404 |  |  |  | NOT RUN |  |

## Defectos encontrados

| # | Caso | Severidad | Descripción | Estado |
| | | | | | | | | |

## Conclusión

- Casos ejecutados / totales:
- Aprobados / fallidos:
- Decisión:
