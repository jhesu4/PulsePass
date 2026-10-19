-- =============================================================================
-- PulsePass - Semilla de datos para pruebas E2E manuales (Fase 5)
-- SOLO para la base dedicada `pulsepass_e2e`. NUNCA ejecutar en dev/QA/produccion.
--
-- Uso (los hashes BCrypt se pasan por variables; no se versionan secretos):
--   psql -h localhost -U <usuario> -d pulsepass_e2e \
--     -v staff_hash="'<bcrypt_staff>'" \
--     -v admin_hash="'<bcrypt_admin>'" \
--     -v inactive_hash="'<bcrypt_inactivo>'" \
--     -f tests/e2e/fixtures/seed-e2e.sql
--
-- Requisitos: la app ya arranco una vez contra pulsepass_e2e (Flyway V1-V4 aplicadas).
-- Es idempotente: se puede re-ejecutar (ON CONFLICT DO NOTHING).
-- =============================================================================
\set ON_ERROR_STOP on

-- Guardia: aborta si no es la base E2E.
DO $$
BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'Abortado: la base actual es %, se esperaba pulsepass_e2e', current_database();
    END IF;
END $$;

-- Guardia: las tres variables de hash deben venir definidas.
\if :{?staff_hash}
\else
  \echo 'Falta -v staff_hash=...'
  \quit
\endif
\if :{?admin_hash}
\else
  \echo 'Falta -v admin_hash=...'
  \quit
\endif
\if :{?inactive_hash}
\else
  \echo 'Falta -v inactive_hash=...'
  \quit
\endif

BEGIN;

-- ---------- Venues (la API no expone alta de venues) ----------
INSERT INTO venues (code, name, city, address, capacity, active) VALUES
  ('VEN-E2E-01',  'E2E Arena',        'Bogota', 'Calle 1 # 1-01',  500, TRUE),
  ('VEN-E2E-CAP', 'E2E Mini Hall',    'Bogota', 'Calle 2 # 2-02',    1, TRUE),
  ('VEN-E2E-OFF', 'E2E Closed Venue', 'Bogota', 'Calle 3 # 3-03',  100, FALSE)
ON CONFLICT (code) DO NOTHING;

-- ---------- Usuarios STAFF / ADMIN / inactivo (dominio example.test) ----------
INSERT INTO users (username, email, active) VALUES
  ('e2e-staff',    'e2e-staff@example.test',    TRUE),
  ('e2e-admin',    'e2e-admin@example.test',    TRUE),
  ('e2e-inactive', 'e2e-inactive@example.test', FALSE)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_profiles (first_name, last_name, city, birth_date, user_id)
SELECT 'E2E', t.last_name, 'Bogota', DATE '1990-01-01', u.id
FROM (VALUES ('e2e-staff','Staff'), ('e2e-admin','Admin'), ('e2e-inactive','Inactive')) AS t(username, last_name)
JOIN users u ON u.username = t.username
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_credentials (user_id, password_hash, password_changed_at)
SELECT u.id, t.hash, now()
FROM (VALUES ('e2e-staff',    :staff_hash),
             ('e2e-admin',    :admin_hash),
             ('e2e-inactive', :inactive_hash)) AS t(username, hash)
JOIN users u ON u.username = t.username
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT u.id, t.role
FROM (VALUES ('e2e-staff','STAFF'), ('e2e-admin','ADMIN'), ('e2e-inactive','USER')) AS t(username, role)
JOIN users u ON u.username = t.username
ON CONFLICT DO NOTHING;

-- ---------- Eventos (fechas relativas a now() para que no caduquen) ----------
INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id)
SELECT t.code, t.name, 'Fixture E2E', t.category, t.status, t.event_date, t.min_age, v.id
FROM (VALUES
  ('E2E-DRAFT-01',     'E2E Borrador (artistas)',        'MUSIC', 'DRAFT',     now() + interval '365 days',   0, 'VEN-E2E-01'),
  ('E2E-TOPUBLISH-01', 'E2E Para publicar',              'MUSIC', 'DRAFT',     now() + interval '365 days',   0, 'VEN-E2E-01'),
  ('E2E-PUBLISHED-01', 'E2E Publicado',                  'MUSIC', 'PUBLISHED', now() + interval '365 days',   0, 'VEN-E2E-01'),
  ('E2E-PAST-01',      'E2E Publicado en el pasado',     'MUSIC', 'PUBLISHED', now() - interval '30 days',    0, 'VEN-E2E-01'),
  ('E2E-AGE-01',       'E2E Edad minima imposible',      'MUSIC', 'PUBLISHED', now() + interval '365 days', 120, 'VEN-E2E-01'),
  ('E2E-FULL-01',      'E2E Aforo 1',                    'MUSIC', 'PUBLISHED', now() + interval '365 days',   0, 'VEN-E2E-CAP')
) AS t(code, name, category, status, event_date, min_age, venue_code)
JOIN venues v ON v.code = t.venue_code
ON CONFLICT (event_code) DO NOTHING;

COMMIT;

\echo 'Semilla E2E aplicada.'
