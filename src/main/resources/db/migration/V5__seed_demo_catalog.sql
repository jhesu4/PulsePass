INSERT INTO venues (code, name, city, address, capacity, active)
VALUES
	('PP-VEN-BOG-01', 'Arena Capital', 'Bogotá', 'Calle 80 # 68-45', 18000, TRUE),
	('PP-VEN-MED-01', 'Foro de las Montañas', 'Medellín', 'Carrera 70 # 1-20', 12000, TRUE),
	('PP-VEN-SMR-01', 'Marina del Caribe', 'Santa Marta', 'Carrera 1 # 22-10', 8000, TRUE),
	('PP-VEN-CAR-01', 'Castillo de la Música', 'Cartagena', 'Calle 31 # 18-22', 6500, TRUE),
	('PP-VEN-CAL-01', 'Teatro del Río', 'Cali', 'Avenida Colombia # 4-50', 5000, TRUE),
	('PP-VEN-BAQ-01', 'Estadio del Norte', 'Barranquilla', 'Calle 72 # 46-10', 25000, TRUE),
	('PP-VEN-BGA-01', 'Centro Cultural Santander', 'Bucaramanga', 'Carrera 27 # 45-18', 3200, TRUE),
	('PP-VEN-PER-01', 'Pabellón del Café', 'Pereira', 'Avenida 30 de Agosto # 40-12', 2800, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO events (
	event_code, name, description, category, status, event_date, minimum_age, venue_id
)
SELECT
	demo.event_code,
	demo.name,
	demo.description,
	demo.category,
	'PUBLISHED',
	DATE_TRUNC('minute', CURRENT_TIMESTAMP) + demo.days_after * INTERVAL '1 day',
	demo.minimum_age,
	venue.id
FROM (
	VALUES
		('PP-DEMO-BOG-MUSIC-01', 'Noches de Neón: Solar Beat', 'Una noche de música electrónica, visuales inmersivos y una puesta en escena diseñada para bailar hasta el último track.', 'MUSIC', 35, 18, 'PP-VEN-BOG-01'),
		('PP-DEMO-MED-MUSIC-01', 'Neon Waves: Ecos de la Ciudad', 'Synth-pop en vivo, luces cinematográficas y una selección de canciones para cantar en comunidad.', 'MUSIC', 55, 16, 'PP-VEN-MED-01'),
		('PP-DEMO-SMR-MUSIC-01', 'Caribbean Sound: Brisa y Tambor', 'Ritmos del Caribe, artistas invitados y una tarde frente al mar llena de sabor y baile.', 'MUSIC', 75, 0, 'PP-VEN-SMR-01'),
		('PP-DEMO-CAR-CULTURE-01', 'Cartagena bajo las Estrellas', 'Música, danza y narración oral se encuentran en una celebración de la cultura del Caribe colombiano.', 'CULTURE', 95, 0, 'PP-VEN-CAR-01'),
		('PP-DEMO-CAL-TECH-01', 'Cali Tech & Creators', 'Encuentro de tecnología creativa con charlas, demostraciones y conversaciones sobre inteligencia artificial y diseño.', 'TECHNOLOGY', 115, 12, 'PP-VEN-CAL-01'),
		('PP-DEMO-BAQ-SPORTS-01', 'Carrera Urbana del Norte', 'Una jornada deportiva abierta con recorridos para distintos niveles, música en ruta y actividades para toda la familia.', 'SPORTS', 135, 0, 'PP-VEN-BAQ-01'),
		('PP-DEMO-BGA-CULTURE-01', 'Festival de Teatro Santander', 'Una selección de obras contemporáneas, teatro de calle y encuentros con compañías regionales.', 'CULTURE', 155, 12, 'PP-VEN-BGA-01'),
		('PP-DEMO-PER-EDU-01', 'Ideas que Transforman', 'Conferencia de educación, innovación y aprendizaje, con talleres prácticos y espacios de intercambio.', 'EDUCATION', 175, 0, 'PP-VEN-PER-01'),
		('PP-DEMO-BOG-ENT-01', 'Ocean Drive: Sesión Acústica', 'Una presentación íntima de indie y canciones originales en formato acústico, con cupos limitados.', 'ENTERTAINMENT', 195, 12, 'PP-VEN-BOG-01'),
		('PP-DEMO-MED-TECH-01', 'Digital Pulse: Future Lab', 'Experiencia interactiva de sonido, código y arte digital con instalaciones participativas y música en directo.', 'TECHNOLOGY', 215, 16, 'PP-VEN-MED-01'),
		('PP-DEMO-SMR-SPORTS-01', 'Vóley de Playa: Copa Marina', 'Torneo de voleibol de playa con equipos invitados, zona de espectadores y actividades deportivas al aire libre.', 'SPORTS', 235, 0, 'PP-VEN-SMR-01'),
		('PP-DEMO-CAL-MUSIC-01', 'Caribbean Music Fest: Edición Pacífico', 'Una celebración de sonidos caribeños y del Pacífico con artistas invitados, gastronomía local y producción en vivo.', 'MUSIC', 255, 18, 'PP-VEN-CAL-01')
) AS demo(event_code, name, description, category, days_after, minimum_age, venue_code)
JOIN venues venue ON venue.code = demo.venue_code AND venue.active = TRUE
WHERE NOT EXISTS (
	SELECT 1 FROM events existing WHERE existing.event_code = demo.event_code
);

INSERT INTO event_artists (event_id, artist_id)
SELECT event.id, artist.id
FROM (
	VALUES
		('PP-DEMO-BOG-MUSIC-01', 'Solar Beat'),
		('PP-DEMO-MED-MUSIC-01', 'Neon Waves'),
		('PP-DEMO-SMR-MUSIC-01', 'Caribbean Sound'),
		('PP-DEMO-CAR-CULTURE-01', 'Caribbean Sound'),
		('PP-DEMO-CAL-TECH-01', 'Digital Pulse'),
		('PP-DEMO-BGA-CULTURE-01', 'Ocean Drive'),
		('PP-DEMO-PER-EDU-01', 'Digital Pulse'),
		('PP-DEMO-BOG-ENT-01', 'Ocean Drive'),
		('PP-DEMO-MED-TECH-01', 'Digital Pulse'),
		('PP-DEMO-SMR-SPORTS-01', 'Caribbean Sound'),
		('PP-DEMO-CAL-MUSIC-01', 'Caribbean Sound'),
		('PP-DEMO-CAL-MUSIC-01', 'Solar Beat'),
		('PP-DEMO-CAL-MUSIC-01', 'Neon Waves')
) AS lineup(event_code, stage_name)
JOIN events event ON event.event_code = lineup.event_code
JOIN artists artist ON artist.stage_name = lineup.stage_name
ON CONFLICT (event_id, artist_id) DO NOTHING;
