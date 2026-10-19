# Fixtures E2E

Datos mínimos para las pruebas manuales. **Solo** para la base dedicada `pulsepass_e2e`.

## Qué siembra `seed-e2e.sql`

| Dato | Detalle |
|---|---|
| Venues | `VEN-E2E-01` (aforo 500), `VEN-E2E-CAP` (aforo 1), `VEN-E2E-OFF` (inactivo). La API no permite crear venues |
| Usuarios | `e2e-staff` (STAFF), `e2e-admin` (ADMIN), `e2e-inactive` (inactivo), todos `@example.test` |
| Eventos | `E2E-DRAFT-01` (DRAFT, para artistas), `E2E-TOPUBLISH-01` (DRAFT, para publicar), `E2E-PUBLISHED-01`, `E2E-PAST-01` (fecha pasada), `E2E-AGE-01` (edad mínima 120), `E2E-FULL-01` (aforo 1) |
| Artistas | Ya sembrados por la migración V3 (id 1 = `Solar Beat`) |

Los usuarios A, B y C **no** se siembran: se crean por la API (`POST /api/users`) con el prefijo `runId`; siempre quedan con rol `USER`.

## Contraseñas y hashes (nada se versiona)

1. Elija contraseñas locales (≥ 12 caracteres, ≤ 72 bytes) para STAFF, ADMIN e inactivo.
2. Genere cada hash BCrypt localmente, por ejemplo con el `BCryptPasswordEncoder` de Spring, o con `htpasswd -bnBC 10 "" '<clave>' | tr -d ':\n' | sed 's/^\$2y/\$2a/'`.
3. Ejecute:

```bash
psql -h localhost -U <usuario> -d pulsepass_e2e \
  -v staff_hash="'<hash_staff>'" -v admin_hash="'<hash_admin>'" -v inactive_hash="'<hash_inactivo>'" \
  -f tests/e2e/fixtures/seed-e2e.sql
```

El script aborta si la base no se llama `pulsepass_e2e` o si falta algún hash. Es idempotente.

## Re-sembrar

Las pruebas modifican estado de forma irreversible (publicar, cancelar, usar, agotar aforo). Para repetir una ejecución completa: recrear la base `pulsepass_e2e`, arrancar la app (Flyway) y volver a sembrar. Además use un `runId` nuevo.
