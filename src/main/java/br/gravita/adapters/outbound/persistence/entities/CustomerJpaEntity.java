package br.gravita.adapters.outbound.persistence.entities;

import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.PersonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "customers")
public class CustomerJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private PersonType personType;

	@Column(nullable = false, unique = true)
	private String document;

	private String email;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private CustomerStatus status;

	protected CustomerJpaEntity() {
	}

	public CustomerJpaEntity(UUID id, String name, PersonType personType, String document, String email, CustomerStatus status) {
		this.id = id;
		this.name = name;
		this.personType = personType;
		this.document = document;
		this.email = email;
		this.status = status;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public PersonType getPersonType() {
		return personType;
	}

	public String getDocument() {
		return document;
	}

	public String getEmail() {
		return email;
	}

	public CustomerStatus getStatus() {
		return status;
	}
}
