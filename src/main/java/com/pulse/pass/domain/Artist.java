package com.pulse.pass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "artists")
public class Artist {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "stage_name", nullable = false, unique = true, length = 150)
	private String stageName;

	@Column(nullable = false, length = 100)
	private String country;

	@Column(nullable = false, length = 100)
	private String genre;

	@Column(nullable = false)
	private boolean active = true;

	@ManyToMany(mappedBy = "artists")
	private Set<Event> events = new HashSet<>();

	protected Artist() {
	}

	public Long getId() {
		return id;
	}

	public String getStageName() {
		return stageName;
	}

	public void setStageName(String stageName) {
		this.stageName = stageName;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Set<Event> getEvents() {
		return events;
	}
}
