
CREATE TABLE venues (
	id BIGSERIAL PRIMARY KEY,
	code VARCHAR(50) NOT NULL UNIQUE,
	name VARCHAR(150) NOT NULL,
	city VARCHAR(100) NOT NULL,
	address VARCHAR(255) NOT NULL,
	capacity INTEGER NOT NULL CHECK (capacity > 0),
	active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE artists (
	id BIGSERIAL PRIMARY KEY,
	stage_name VARCHAR(150) NOT NULL UNIQUE,    
	country VARCHAR(100) NOT NULL,
	genre VARCHAR(100) NOT NULL,
	active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE users (
	id BIGSERIAL PRIMARY KEY,
	username VARCHAR(50) NOT NULL UNIQUE,
	email VARCHAR(255) NOT NULL UNIQUE,
	active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE events (
	id BIGSERIAL PRIMARY KEY,
	event_code VARCHAR(50) NOT NULL UNIQUE,
	name VARCHAR(200) NOT NULL,
	description TEXT,
	category VARCHAR(30) NOT NULL CHECK (category IN ('MUSIC', 'SPORTS', 'TECHNOLOGY', 'EDUCATION', 'CULTURE', 'ENTERTAINMENT')),
	status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'SOLD_OUT', 'CANCELLED', 'FINISHED')),
	event_date TIMESTAMP NOT NULL,
	minimum_age INTEGER NOT NULL DEFAULT 0 CHECK (minimum_age >= 0),
	venue_id BIGINT NOT NULL REFERENCES venues (id)
);

CREATE TABLE event_artists (
	event_id BIGINT NOT NULL REFERENCES events (id) ON DELETE CASCADE,
	artist_id BIGINT NOT NULL REFERENCES artists (id) ON DELETE CASCADE,
	PRIMARY KEY (event_id, artist_id)
);

CREATE TABLE user_profiles (
	id BIGSERIAL PRIMARY KEY,
	first_name VARCHAR(100) NOT NULL,
	last_name VARCHAR(100) NOT NULL,
	phone VARCHAR(30),
	city VARCHAR(100),
	birth_date DATE,
	user_id BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE tickets (
	id BIGSERIAL PRIMARY KEY,
	ticket_code VARCHAR(50) NOT NULL UNIQUE,
	type VARCHAR(20) NOT NULL CHECK (type IN ('GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT')),
	price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
	status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'PAID', 'CANCELLED', 'USED')),
	purchase_date TIMESTAMP NOT NULL,
	user_id BIGINT NOT NULL REFERENCES users (id),
	event_id BIGINT NOT NULL REFERENCES events (id)
);

CREATE INDEX idx_events_venue_id ON events (venue_id);
CREATE INDEX idx_events_status_event_date ON events (status, event_date);
CREATE INDEX idx_event_artists_artist_id ON event_artists (artist_id);
CREATE INDEX idx_tickets_user_id ON tickets (user_id);
CREATE INDEX idx_tickets_event_status ON tickets (event_id, status);
