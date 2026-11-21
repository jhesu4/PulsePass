package com.pulse.pass.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class SecurityMigrationUpgradeTest {

	@Container
	private static final PostgreSQLContainer postgres = new PostgreSQLContainer(
			DockerImageName.parse("postgres:16-alpine"));

	@Test
	void shouldApplyLatestMigrationsToExistingV3Schema() throws SQLException {
		Flyway v3Flyway = createFlyway().target(MigrationVersion.fromVersion("3")).load();
		v3Flyway.migrate();
		assertThat(v3Flyway.info().current().getVersion().getVersion()).isEqualTo("3");

		Flyway latestFlyway = createFlyway().load();
		latestFlyway.migrate();
		assertThat(latestFlyway.info().current().getVersion().getVersion()).isEqualTo("6");

		try (Connection connection = DriverManager.getConnection(
				postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
			DatabaseMetaData metadata = connection.getMetaData();
			assertTableExists(metadata, "user_credentials");
			assertTableExists(metadata, "user_roles");
			try (var statement = connection.createStatement();
					var venues = statement.executeQuery("SELECT COUNT(*) FROM venues WHERE code LIKE 'PP-VEN-%'")) {
				assertThat(venues.next()).isTrue();
				assertThat(venues.getInt(1)).isEqualTo(10);
			}
			try (var statement = connection.createStatement();
					var events = statement.executeQuery("SELECT COUNT(*) FROM events WHERE event_code LIKE 'PP-DEMO-%'")) {
				assertThat(events.next()).isTrue();
				assertThat(events.getInt(1)).isEqualTo(30);
			}
			try (var statement = connection.createStatement();
					var artists = statement.executeQuery("SELECT COUNT(*) FROM artists WHERE image_url IS NOT NULL")) {
				assertThat(artists.next()).isTrue();
				assertThat(artists.getInt(1)).isEqualTo(13);
			}
			try (var statement = connection.createStatement();
					var events = statement.executeQuery("SELECT COUNT(*) FROM events WHERE event_code LIKE 'PP-DEMO-%' AND image_url IS NOT NULL")) {
				assertThat(events.next()).isTrue();
				assertThat(events.getInt(1)).isEqualTo(30);
			}
		}
	}

	private FluentConfiguration createFlyway() {
		return Flyway.configure()
				.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
				.locations("classpath:db/migration");
	}

	private void assertTableExists(DatabaseMetaData metadata, String tableName) throws SQLException {
		try (var tables = metadata.getTables(null, null, tableName, new String[] { "TABLE" })) {
			assertThat(tables.next()).as("table %s exists after Flyway migration", tableName).isTrue();
		}
	}
}