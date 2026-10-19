# Checklist E2E manual

Marcar `[x]` al ejecutar cada caso con **Send** en Postman. Resultado esperado = código HTTP indicado. Anotar desviaciones en `registro-ejecucion-manual.md`.

## Pre-ejecución

- [ ] Base `pulsepass_e2e` creada y distinta de dev/QA/producción
- [ ] App arrancada contra `pulsepass_e2e` (Flyway V1–V4 aplicado)
- [ ] Semilla `fixtures/seed-e2e.sql` ejecutada con hashes BCrypt locales
- [ ] Entorno Postman local importado, `runId` único y contraseñas definidas
- [ ] Colección importada; confirmado que no contiene scripts

## 00 Público

- [ ] **PUB-01** Listar venues activos — `GET /api/venues/active` · Anónimo · esperado **200**
- [ ] **PUB-02** Venue por código — `GET /api/venues/{code}` · Anónimo · esperado **200**
- [ ] **PUB-03** Venue inexistente — `GET /api/venues/{code}` · Anónimo · esperado **404**
- [ ] **PUB-04** Listar artistas activos — `GET /api/artists/active` · Anónimo · esperado **200**
- [ ] **PUB-05** Artista por id — `GET /api/artists/{id}` · Anónimo · esperado **200**
- [ ] **PUB-06** Artista por nombre artístico — `GET /api/artists/by-stage-name` · Anónimo · esperado **200**
- [ ] **PUB-07** Artista inexistente — `GET /api/artists/{id}` · Anónimo · esperado **404**
- [ ] **PUB-08** Eventos publicados — `GET /api/events/published` · Anónimo · esperado **200**
- [ ] **PUB-09** Evento PUBLISHED por código (anónimo) — `GET /api/events/{eventCode}` · Anónimo · esperado **200**
- [ ] **PUB-10** Evento DRAFT por código (anónimo) — `GET /api/events/{eventCode}` · Anónimo · esperado **403**
- [ ] **PUB-11** Eventos por artista — `GET /api/events/by-artist` · Anónimo · esperado **200**
- [ ] **TC-AUTH-015** Bearer inválido en endpoint público — `GET /api/venues/active` · Bearer inválido · esperado **401**

## 01 Registro y autenticación

- [ ] **AUTH-01** Registrar usuario A — `POST /api/users` · Anónimo · esperado **201**
- [ ] **AUTH-02** Registrar usuario B — `POST /api/users` · Anónimo · esperado **201**
- [ ] **AUTH-03** Registro duplicado (mismo email) — `POST /api/users` · Anónimo · esperado **409**
- [ ] **AUTH-04** Registro con password débil (<12) — `POST /api/users` · Anónimo · esperado **400**
- [ ] **AUTH-05** Registro sin campos obligatorios — `POST /api/users` · Anónimo · esperado **400**
- [ ] **AUTH-06** Registro con username con @ — `POST /api/users` · Anónimo · esperado **400**
- [ ] **AUTH-07** Registro con fecha de nacimiento futura — `POST /api/users` · Anónimo · esperado **409**
- [ ] **AUTH-08** Inyección de rol en registro público — `POST /api/users` · Anónimo · esperado **201**
- [ ] **AUTH-09** Login A por username — `POST /api/auth/login` · Público · esperado **200**
- [ ] **AUTH-10** Login A por email — `POST /api/auth/login` · Público · esperado **200**
- [ ] **AUTH-11** Login B — `POST /api/auth/login` · Público · esperado **200**
- [ ] **AUTH-12** Login STAFF — `POST /api/auth/login` · Público · esperado **200**
- [ ] **AUTH-13** Login ADMIN — `POST /api/auth/login` · Público · esperado **200**
- [ ] **AUTH-14** Login con contraseña incorrecta — `POST /api/auth/login` · Público · esperado **401**
- [ ] **AUTH-15** Login con usuario inexistente — `POST /api/auth/login` · Público · esperado **401**
- [ ] **AUTH-16** Login de usuario inactivo — `POST /api/auth/login` · Público · esperado **401**
- [ ] **AUTH-22** Preparar TC-AUTH-013: obtener token antes de desactivar — `POST /api/auth/login` · Cuenta E2E temporalmente activa · esperado **200**
- [ ] **AUTH-17** Login usuario C (verificación de roles) — `POST /api/auth/login` · Público · esperado **200**
- [ ] **AUTH-18** Perfil actual usuario C (roles) — `GET /api/auth/me` · USER (C) · esperado **200**
- [ ] **AUTH-19** Perfil actual A — `GET /api/auth/me` · USER (A) · esperado **200**
- [ ] **AUTH-20** Perfil actual STAFF — `GET /api/auth/me` · STAFF · esperado **200**
- [ ] **AUTH-21** Perfil actual ADMIN — `GET /api/auth/me` · ADMIN · esperado **200**

## 02 JWT inválidos

- [ ] **JWT-01** /me sin token — `GET /api/auth/me` · Anónimo · esperado **401**
- [ ] **JWT-02** /me con token malformado — `GET /api/auth/me` · Token inválido · esperado **401**
- [ ] **JWT-03** /me con token alterado — `GET /api/auth/me` · Token inválido · esperado **401**
- [ ] **JWT-04** /me con token expirado — `GET /api/auth/me` · Token inválido · esperado **401**
- [ ] **JWT-05** /me con token firmado con otra clave — `GET /api/auth/me` · Token inválido · esperado **401**
- [ ] **JWT-06** Esquema Authorization distinto de Bearer — `GET /api/auth/me` · Token inválido · esperado **401**
- [ ] **JWT-07** Token en query string (no soportado) — `GET /api/auth/me` · Token en URL · esperado **401**
- [ ] **JWT-08** Token válido sobre endpoint protegido de otro rol — `GET /api/events/{eventCode}/tickets/paid` · USER (A) · esperado **403**
- [ ] **TC-AUTH-013** Usuario desactivado con JWT previo — `GET /api/auth/me` · Token emitido antes de desactivar la cuenta · esperado **401**
- [ ] **TC-SEC-017** Rol STAFF revocado con JWT previo — `GET /api/events/{eventCode}/tickets/paid` · JWT emitido antes de revocar STAFF · esperado **403**

## 03 Eventos y RBAC

- [ ] **EVT-01** Crear evento sin token — `POST /api/events` · Anónimo · esperado **401**
- [ ] **EVT-02** Crear evento como USER — `POST /api/events` · USER (A) · esperado **403**
- [ ] **EVT-03** Crear evento como STAFF — `POST /api/events` · STAFF · esperado **403**
- [ ] **EVT-04** Crear evento como ADMIN — `POST /api/events` · ADMIN · esperado **201**
- [ ] **TC-BIZ-003** Leer el evento recién creado — `GET /api/events/{eventCode}` · ADMIN · esperado **200** (estado `DRAFT`)
- [ ] **EVT-05** Crear evento con código duplicado — `POST /api/events` · ADMIN · esperado **409**
- [ ] **EVT-06** Crear evento con body inválido — `POST /api/events` · ADMIN · esperado **400**
- [ ] **EVT-07** Crear evento con fecha pasada — `POST /api/events` · ADMIN · esperado **409**
- [ ] **EVT-08** Crear evento en venue inactivo — `POST /api/events` · ADMIN · esperado **409**
- [ ] **EVT-09** Crear evento en venue inexistente — `POST /api/events` · ADMIN · esperado **404**
- [ ] **EVT-10** Categoría inválida — `POST /api/events` · ADMIN · esperado **400**
- [ ] **EVT-11** Leer evento DRAFT como USER — `GET /api/events/{eventCode}` · USER (A) · esperado **403**
- [ ] **EVT-12** Leer evento DRAFT como STAFF — `GET /api/events/{eventCode}` · STAFF · esperado **200**
- [ ] **EVT-13** Leer evento DRAFT como ADMIN — `GET /api/events/{eventCode}` · ADMIN · esperado **200**
- [ ] **EVT-14** Leer evento inexistente como USER — `GET /api/events/{eventCode}` · USER (A) · esperado **403**
- [ ] **EVT-15** Leer evento inexistente como STAFF — `GET /api/events/{eventCode}` · STAFF · esperado **404**
- [ ] **EVT-16** Publicar como USER — `PATCH /api/events/{eventCode}/publish` · USER (A) · esperado **403**
- [ ] **EVT-17** Publicar como STAFF — `PATCH /api/events/{eventCode}/publish` · STAFF · esperado **403**
- [ ] **EVT-18** Publicar sin token — `PATCH /api/events/{eventCode}/publish` · Anónimo · esperado **401**
- [ ] **EVT-19** Publicar como ADMIN el evento creado en EVT-04 — `PATCH /api/events/{eventCode}/publish` · ADMIN · esperado **200**
- [ ] **TC-BIZ-004** Verificar evento creado en eventos publicados — `GET /api/events/published` · Anónimo · esperado **200** e incluye el evento creado
- [ ] **EVT-20** Publicar de nuevo (ya PUBLISHED) — `PATCH /api/events/{eventCode}/publish` · ADMIN · esperado **409**
- [ ] **EVT-21** Publicar evento con fecha pasada — `PATCH /api/events/{eventCode}/publish` · ADMIN · esperado **409**
- [ ] **EVT-22** Publicar evento inexistente — `PATCH /api/events/{eventCode}/publish` · ADMIN · esperado **404**
- [ ] **EVT-23** Asociar artista como USER — `POST /api/events/{eventCode}/artists/{artistId}` · USER (A) · esperado **403**
- [ ] **EVT-24** Asociar artista como STAFF — `POST /api/events/{eventCode}/artists/{artistId}` · STAFF · esperado **403**
- [ ] **EVT-25** Asociar artista al evento creado y publicado — `POST /api/events/{eventCode}/artists/{artistId}` · ADMIN · esperado **200**
- [ ] **EVT-26** Asociar el mismo artista otra vez — `POST /api/events/{eventCode}/artists/{artistId}` · ADMIN · esperado **409**
- [ ] **EVT-27** Asociar artista inexistente — `POST /api/events/{eventCode}/artists/{artistId}` · ADMIN · esperado **404**
- [ ] **EVT-28** Asociar artista a evento inexistente — `POST /api/events/{eventCode}/artists/{artistId}` · ADMIN · esperado **404**
- [ ] **TC-BIZ-005 (EVT-29)** Verificar evento creado en eventos por artista — `GET /api/events/by-artist` · Anónimo · esperado **200** e incluye el evento creado

## 04 Usuarios (ownership)

- [ ] **USR-01** Mi usuario por email — `GET /api/users/by-email` · USER (A) · esperado **200**
- [ ] **USR-02** Mi usuario por username — `GET /api/users/by-username` · USER (A) · esperado **200**
- [ ] **USR-03** Usuario ajeno por email (IDOR) — `GET /api/users/by-email` · USER (A) · esperado **403**
- [ ] **USR-04** Usuario ajeno por username (IDOR) — `GET /api/users/by-username` · USER (A) · esperado **403**
- [ ] **USR-05** Email inexistente como USER — `GET /api/users/by-email` · USER (A) · esperado **403**
- [ ] **USR-06** Usuario ajeno como STAFF — `GET /api/users/by-email` · STAFF · esperado **403**
- [ ] **USR-07** Usuario ajeno como ADMIN sin propósito — `GET /api/users/by-email` · ADMIN · esperado **403**
- [ ] **USR-08** Usuario ajeno como ADMIN con propósito CUSTOMER_SUPPORT — `GET /api/users/by-email` · ADMIN + X-Admin-Purpose · esperado **200**
- [ ] **USR-09** ADMIN con propósito incorrecto (TICKET_SUPPORT) — `GET /api/users/by-username` · ADMIN + propósito inválido · esperado **403**
- [ ] **USR-10** USER intenta usar X-Admin-Purpose — `GET /api/users/by-email` · USER (A) · esperado **403**
- [ ] **USR-11** ADMIN con propósito, usuario inexistente — `GET /api/users/by-username` · ADMIN + X-Admin-Purpose · esperado **404**
- [ ] **USR-12** Consulta de usuario sin token — `GET /api/users/by-email` · Anónimo · esperado **401**
- [ ] **USR-13** Respuesta no expone credenciales — `GET /api/users/by-email` · USER (A) · esperado **200**

## 05 Tickets

- [ ] **TKT-01** Comprar sin token — `POST /api/tickets` · Anónimo · esperado **401**
- [ ] **TKT-02** Comprar como STAFF — `POST /api/tickets` · STAFF · esperado **403**
- [ ] **TKT-03** Comprar como ADMIN — `POST /api/tickets` · ADMIN · esperado **403**
- [ ] **TKT-04** A compra ticket T1 para sí — `POST /api/tickets` · USER (A) · esperado **201**
- [ ] **TKT-05** A compra ticket T2 para sí — `POST /api/tickets` · USER (A) · esperado **201**
- [ ] **TKT-06** A compra a nombre de B (IDOR) — `POST /api/tickets` · USER (A) · esperado **403**
- [ ] **TKT-07** A compra a nombre de email inexistente — `POST /api/tickets` · USER (A) · esperado **403**
- [ ] **TKT-08** Comprar con body inválido — `POST /api/tickets` · USER (A) · esperado **400**
- [ ] **TKT-09** Comprar tipo inválido — `POST /api/tickets` · USER (A) · esperado **400**
- [ ] **TKT-10** Comprar evento DRAFT — `POST /api/tickets` · USER (A) · esperado **409**
- [ ] **TKT-11** Comprar evento con fecha pasada — `POST /api/tickets` · USER (A) · esperado **409**
- [ ] **TKT-12** Comprar evento con edad mínima no cumplida — `POST /api/tickets` · USER (A) · esperado **409**
- [ ] **TKT-13** Comprar evento inexistente — `POST /api/tickets` · USER (A) · esperado **404**
- [ ] **TKT-14** Aforo: A compra la última entrada — `POST /api/tickets` · USER (A) · esperado **201**
- [ ] **TKT-15** Aforo: B intenta comprar sin cupo — `POST /api/tickets` · USER (B) · esperado **409**
- [ ] **TKT-16** A lee su ticket — `GET /api/tickets/{ticketCode}` · USER (A) · esperado **200**
- [ ] **TKT-17** B lee ticket de A (IDOR) — `GET /api/tickets/{ticketCode}` · USER (B) · esperado **403**
- [ ] **TKT-18** STAFF lee ticket de A — `GET /api/tickets/{ticketCode}` · STAFF · esperado **200**
- [ ] **TKT-19** ADMIN lee ticket de A sin propósito — `GET /api/tickets/{ticketCode}` · ADMIN · esperado **403**
- [ ] **TKT-20** ADMIN lee ticket de A con TICKET_SUPPORT — `GET /api/tickets/{ticketCode}` · ADMIN + X-Admin-Purpose · esperado **200**
- [ ] **TKT-21** ADMIN con propósito incorrecto (CUSTOMER_SUPPORT) — `GET /api/tickets/{ticketCode}` · ADMIN + propósito inválido · esperado **403**
- [ ] **TKT-22** Ticket inexistente como USER — `GET /api/tickets/{ticketCode}` · USER (A) · esperado **403**
- [ ] **TKT-23** Ticket inexistente como STAFF — `GET /api/tickets/{ticketCode}` · STAFF · esperado **404**
- [ ] **TKT-24** Leer ticket sin token — `GET /api/tickets/{ticketCode}` · Anónimo · esperado **401**
- [ ] **TKT-25** A lista sus tickets — `GET /api/tickets/by-user` · USER (A) · esperado **200**
- [ ] **TKT-26** B lista tickets de A (IDOR) — `GET /api/tickets/by-user` · USER (B) · esperado **403**
- [ ] **TKT-27** STAFF lista tickets de A — `GET /api/tickets/by-user` · STAFF · esperado **403**
- [ ] **TKT-28** ADMIN con TICKET_SUPPORT lista tickets de A — `GET /api/tickets/by-user` · ADMIN + X-Admin-Purpose · esperado **200**
- [ ] **TKT-29** ADMIN sin propósito lista tickets — `GET /api/tickets/by-user` · ADMIN · esperado **403**
- [ ] **TKT-30** Listar tickets sin token — `GET /api/tickets/by-user` · Anónimo · esperado **401**

## 06 Transiciones

- [ ] **TRN-01** Cancelar T1 como B (IDOR) — `PATCH /api/tickets/{ticketCode}/cancel` · USER (B) · esperado **403**
- [ ] **TRN-02** Cancelar T1 como STAFF — `PATCH /api/tickets/{ticketCode}/cancel` · STAFF · esperado **403**
- [ ] **TRN-03** Cancelar T1 sin token — `PATCH /api/tickets/{ticketCode}/cancel` · Anónimo · esperado **401**
- [ ] **TRN-04** Cancelar T1 como ADMIN sin propósito — `PATCH /api/tickets/{ticketCode}/cancel` · ADMIN · esperado **403**
- [ ] **TRN-05** Cancelar ticket inexistente como USER — `PATCH /api/tickets/{ticketCode}/cancel` · USER (A) · esperado **403**
- [ ] **TRN-06** A cancela su ticket T1 — `PATCH /api/tickets/{ticketCode}/cancel` · USER (A) · esperado **200**
- [ ] **TRN-07** Cancelar T1 de nuevo — `PATCH /api/tickets/{ticketCode}/cancel` · USER (A) · esperado **409**
- [ ] **TRN-08** Usar T1 cancelado — `PATCH /api/tickets/{ticketCode}/use` · STAFF · esperado **409**
- [ ] **TRN-09** Usar T2 como USER — `PATCH /api/tickets/{ticketCode}/use` · USER (A) · esperado **403**
- [ ] **TRN-10** Usar T2 sin token — `PATCH /api/tickets/{ticketCode}/use` · Anónimo · esperado **401**
- [ ] **TRN-11** Usar ticket inexistente como STAFF — `PATCH /api/tickets/{ticketCode}/use` · STAFF · esperado **404**
- [ ] **TRN-12** STAFF marca T2 como usado — `PATCH /api/tickets/{ticketCode}/use` · STAFF · esperado **200**
- [ ] **TRN-13** Usar T2 de nuevo — `PATCH /api/tickets/{ticketCode}/use` · STAFF · esperado **409**
- [ ] **TRN-14** Cancelar T2 ya usado — `PATCH /api/tickets/{ticketCode}/cancel` · USER (A) · esperado **409**
- [ ] **TRN-15** ADMIN marca ticket como usado (ya USED) — `PATCH /api/tickets/{ticketCode}/use` · ADMIN · esperado **409**

## 07 Tickets pagados

- [ ] **PAID-01** Listar PAID como USER — `GET /api/events/{eventCode}/tickets/paid` · USER (A) · esperado **403**
- [ ] **PAID-02** Listar PAID sin token — `GET /api/events/{eventCode}/tickets/paid` · Anónimo · esperado **401**
- [ ] **PAID-03** Listar PAID como STAFF — `GET /api/events/{eventCode}/tickets/paid` · STAFF · esperado **200**
- [ ] **PAID-04** Listar PAID como ADMIN — `GET /api/events/{eventCode}/tickets/paid` · ADMIN · esperado **200**
- [ ] **PAID-05** Listar PAID de evento inexistente como STAFF — `GET /api/events/{eventCode}/tickets/paid` · STAFF · esperado **404**

## 08 Robustez

- [ ] **ERR-01** Método no permitido — `DELETE método no soportado` · ADMIN · esperado **405**
- [ ] **ERR-02** Content-Type no soportado — `POST /api/auth/login` · Público · esperado **415**
- [ ] **ERR-03** JSON malformado — `POST /api/auth/login` · Público · esperado **400**
- [ ] **ERR-04** Ruta inexistente autenticada — `GET ruta inexistente` · USER (A) · esperado **404**
- [ ] **ERR-05** Errores sin trazas internas — `GET sin fuga de información` · STAFF · esperado **404**

## Post-ejecución

- [ ] Ningún token/contraseña/hash guardado en repo ni en capturas
- [ ] Entorno local con secretos exportado/compartido: NO
- [ ] Registro de ejecución completado y defectos reportados
