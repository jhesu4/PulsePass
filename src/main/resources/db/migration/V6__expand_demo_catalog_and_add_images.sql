ALTER TABLE artists ADD COLUMN image_url VARCHAR(1000);
ALTER TABLE events ADD COLUMN image_url VARCHAR(1000);

UPDATE artists artist
SET image_url = seed.image_url
FROM (
	VALUES
		('Solar Beat', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=800&q=85'),
		('Neon Waves', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=85'),
		('Caribbean Sound', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=800&q=85'),
		('Ocean Drive', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=800&q=85'),
		('Digital Pulse', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=85')
) AS seed(stage_name, image_url)
WHERE artist.stage_name = seed.stage_name;

INSERT INTO artists (stage_name, country, genre, active, image_url)
VALUES
	('Valeria Luz', 'Colombia', 'Pop latino', TRUE, 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=85'),
	('Vértigo Solar', 'Colombia', 'Rock alternativo', TRUE, 'https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?auto=format&fit=crop&w=800&q=85'),
	('Son de Barrio', 'Colombia', 'Salsa', TRUE, 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=800&q=85'),
	('La Marea', 'Colombia', 'Reggaetón', TRUE, 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=800&q=85'),
	('Juan de la Sierra', 'Colombia', 'Folk', TRUE, 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=800&q=85'),
	('Aurora Nómada', 'México', 'Indie', TRUE, 'https://images.unsplash.com/photo-1531123897727-8f129e1688ce?auto=format&fit=crop&w=800&q=85'),
	('Bombo y Mar', 'Colombia', 'Caribbean', TRUE, 'https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?auto=format&fit=crop&w=800&q=85'),
	('Prisma Club', 'Colombia', 'Electronic', TRUE, 'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?auto=format&fit=crop&w=800&q=85')
ON CONFLICT (stage_name) DO UPDATE
SET image_url = COALESCE(artists.image_url, EXCLUDED.image_url);

UPDATE events event
SET image_url = seed.image_url
FROM (
	VALUES
		('PP-DEMO-BOG-MUSIC-01', 'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-MED-MUSIC-01', 'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-SMR-MUSIC-01', 'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-CAR-CULTURE-01', 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-CAL-TECH-01', 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-BAQ-SPORTS-01', 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-BGA-CULTURE-01', 'https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-PER-EDU-01', 'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-BOG-ENT-01', 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-MED-TECH-01', 'https://images.unsplash.com/photo-1507874457470-272b3c8d8ee2?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-SMR-SPORTS-01', 'https://images.unsplash.com/photo-1516280440614-37939bbacd81?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-CAL-MUSIC-01', 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=1400&q=85')
) AS seed(event_code, image_url)
WHERE event.event_code = seed.event_code;

INSERT INTO venues (code, name, city, address, capacity, active)
VALUES
	('PP-VEN-MAN-01', 'Plaza Cultural Los Nevados', 'Manizales', 'Avenida Santander #65-01', 4500, TRUE),
	('PP-VEN-VIL-01', 'Parque de la Cultura Llanera', 'Villavicencio', 'Carrera 40 #26C-10', 7000, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO events (
	event_code, name, description, category, status, event_date, minimum_age, venue_id, image_url
)
SELECT
	demo.event_code,
	demo.name,
	demo.description,
	demo.category,
	'PUBLISHED',
	DATE_TRUNC('minute', CURRENT_TIMESTAMP) + demo.days_after * INTERVAL '1 day',
	demo.minimum_age,
	venue.id,
	demo.image_url
FROM (
	VALUES
		('PP-DEMO-MAN-MUSIC-01', 'Cumbre Sonora: Ritmos de Altura', 'Un encuentro de electrónica y música en vivo entre montañas, con artistas invitados, visuales envolventes y una noche para bailar.', 'MUSIC', 275, 16, 'PP-VEN-MAN-01', 'https://images.unsplash.com/photo-1501612780327-45045538702b?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-VIL-CULTURE-01', 'Llanos Vivos: Noche de Joropo', 'Música llanera, baile y tradición en una celebración familiar con agrupaciones de la región y muestra gastronómica.', 'CULTURE', 285, 0, 'PP-VEN-VIL-01', 'https://images.unsplash.com/photo-1487180144351-b8472da7d491?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-BOG-MUSIC-02', 'Valeria Luz: Cerca de Ti', 'Valeria Luz presenta sus nuevas canciones en un concierto íntimo de pop latino, con invitados especiales y una puesta en escena luminosa.', 'MUSIC', 295, 12, 'PP-VEN-BOG-01', 'https://images.unsplash.com/photo-1478737270239-2f02b77fc618?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-MED-SPORTS-01', 'Medellín Corre de Noche', 'Carrera urbana nocturna con rutas para principiantes y avanzados, estaciones de hidratación y música en vivo en la meta.', 'SPORTS', 305, 0, 'PP-VEN-MED-01', 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-CAL-CULTURE-01', 'Pacífico en Escena', 'Danzas, percusión y relatos del Pacífico se encuentran en una tarde de cultura, talento local y celebración comunitaria.', 'CULTURE', 315, 0, 'PP-VEN-CAL-01', 'https://images.unsplash.com/photo-1468359601543-843bfaef291a?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-BAQ-MUSIC-02', 'Bajo el Sol: La Marea en Vivo', 'Una fiesta de ritmos urbanos junto al Caribe, con La Marea, artistas invitados y una producción de luces y sonido de gran formato.', 'MUSIC', 325, 18, 'PP-VEN-BAQ-01', 'https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-CAR-MUSIC-01', 'Son de Barrio: Salsa para Todos', 'Una noche de salsa para bailar y cantar, con músicos en vivo, pista abierta y clásicos del repertorio latino.', 'MUSIC', 335, 12, 'PP-VEN-CAR-01', 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-BGA-EDU-01', 'Santander Crea: Laboratorio Creativo', 'Talleres y conversaciones sobre diseño, tecnología y emprendimiento creativo, con actividades prácticas para jóvenes y adultos.', 'EDUCATION', 345, 12, 'PP-VEN-BGA-01', 'https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-PER-SPORTS-01', 'Desafío Cafetero de Montaña', 'Recorrido deportivo entre paisajes cafeteros, con distancias para diferentes niveles y una jornada de bienestar al aire libre.', 'SPORTS', 355, 14, 'PP-VEN-PER-01', 'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-MAN-TECH-01', 'Ideas de Altura: Foro de Innovación', 'Un foro cercano sobre inteligencia artificial, sostenibilidad y negocios digitales, con charlas, talleres y espacios para conectar.', 'TECHNOLOGY', 365, 16, 'PP-VEN-MAN-01', 'https://images.unsplash.com/photo-1531058020387-3be344556be6?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-VIL-MUSIC-01', 'Bombo y Mar: Tambor Llanero', 'Bombo y Mar mezcla sonidos del Caribe y los Llanos en un espectáculo enérgico con percusión, baile y músicos invitados.', 'MUSIC', 375, 0, 'PP-VEN-VIL-01', 'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-BOG-CULTURE-01', 'Noche de Teatro: Historias de Ciudad', 'Una selección de obras cortas y nuevas voces escénicas para descubrir historias urbanas, humor y emoción en una misma noche.', 'CULTURE', 385, 12, 'PP-VEN-BOG-01', 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-MED-MUSIC-02', 'Aurora Nómada: Nuevos Horizontes', 'Una velada indie con guitarras, sintetizadores y canciones para cantar juntos, presentada por Aurora Nómada y artistas invitados.', 'MUSIC', 395, 12, 'PP-VEN-MED-01', 'https://images.unsplash.com/photo-1503428593586-e225b39bddfe?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-SMR-TECH-01', 'Mar y Ciencia: Festival Azul', 'Experiencias interactivas y charlas sobre océanos, biodiversidad y tecnología para conocer y cuidar los ecosistemas marinos.', 'EDUCATION', 405, 0, 'PP-VEN-SMR-01', 'https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-CAL-SPORTS-01', 'Cali sobre Ruedas', 'Encuentro recreativo de ciclismo urbano con recorridos guiados, música, recomendaciones de seguridad y actividades para toda la familia.', 'SPORTS', 415, 0, 'PP-VEN-CAL-01', 'https://images.unsplash.com/photo-1528605248644-14dd04022da1?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-BAQ-CULTURE-01', 'Caribe de Colores: Festival Cultural', 'Una jornada de danza, música y sabores del Caribe con presentaciones en vivo y actividades para compartir en familia.', 'CULTURE', 425, 0, 'PP-VEN-BAQ-01', 'https://images.unsplash.com/photo-1505373877841-8d25f7d46678?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-CAR-TECH-01', 'Creatividad Digital Cartagena', 'Creadores, desarrolladores y artistas exploran nuevas formas de contar historias con tecnología, talleres y demostraciones en vivo.', 'TECHNOLOGY', 435, 14, 'PP-VEN-CAR-01', 'https://images.unsplash.com/photo-1505236858219-8359eb29e329?auto=format&fit=crop&w=1400&q=85'),
		('PP-DEMO-MAN-MUSIC-02', 'Vértigo Solar: Ruido y Luz', 'Rock alternativo en directo con Vértigo Solar, guitarras poderosas y una noche de energía compartida con bandas invitadas.', 'MUSIC', 445, 16, 'PP-VEN-MAN-01', 'https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=1400&q=85')
) AS demo(event_code, name, description, category, days_after, minimum_age, venue_code, image_url)
JOIN venues venue ON venue.code = demo.venue_code AND venue.active = TRUE
WHERE NOT EXISTS (
	SELECT 1 FROM events existing WHERE existing.event_code = demo.event_code
);

INSERT INTO event_artists (event_id, artist_id)
SELECT event.id, artist.id
FROM (
	VALUES
		('PP-DEMO-MAN-MUSIC-01', 'Prisma Club'),
		('PP-DEMO-MAN-MUSIC-01', 'Solar Beat'),
		('PP-DEMO-VIL-CULTURE-01', 'Juan de la Sierra'),
		('PP-DEMO-VIL-CULTURE-01', 'Bombo y Mar'),
		('PP-DEMO-BOG-MUSIC-02', 'Valeria Luz'),
		('PP-DEMO-BOG-MUSIC-02', 'Neon Waves'),
		('PP-DEMO-MED-SPORTS-01', 'Digital Pulse'),
		('PP-DEMO-CAL-CULTURE-01', 'Bombo y Mar'),
		('PP-DEMO-CAL-CULTURE-01', 'Son de Barrio'),
		('PP-DEMO-BAQ-MUSIC-02', 'La Marea'),
		('PP-DEMO-CAR-MUSIC-01', 'Son de Barrio'),
		('PP-DEMO-BGA-EDU-01', 'Digital Pulse'),
		('PP-DEMO-PER-SPORTS-01', 'Juan de la Sierra'),
		('PP-DEMO-MAN-TECH-01', 'Prisma Club'),
		('PP-DEMO-VIL-MUSIC-01', 'Bombo y Mar'),
		('PP-DEMO-VIL-MUSIC-01', 'Caribbean Sound'),
		('PP-DEMO-BOG-CULTURE-01', 'Aurora Nómada'),
		('PP-DEMO-MED-MUSIC-02', 'Aurora Nómada'),
		('PP-DEMO-MED-MUSIC-02', 'Ocean Drive'),
		('PP-DEMO-SMR-TECH-01', 'Digital Pulse'),
		('PP-DEMO-CAL-SPORTS-01', 'Vértigo Solar'),
		('PP-DEMO-BAQ-CULTURE-01', 'La Marea'),
		('PP-DEMO-BAQ-CULTURE-01', 'Caribbean Sound'),
		('PP-DEMO-CAR-TECH-01', 'Prisma Club'),
		('PP-DEMO-MAN-MUSIC-02', 'Vértigo Solar'),
		('PP-DEMO-MAN-MUSIC-02', 'Valeria Luz')
) AS lineup(event_code, stage_name)
JOIN events event ON event.event_code = lineup.event_code
JOIN artists artist ON artist.stage_name = lineup.stage_name
ON CONFLICT (event_id, artist_id) DO NOTHING;
