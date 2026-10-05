package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
public abstract class PersonJpaEntity extends AbstractEntity<UUID> {

	/** Assigned by the domain (e.g. {@code CompanyId}), not generated, so a preset id is persisted as given. */
	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	/** The person's tax document; a subclass may store it under its own column (a company keeps it as {@code cnpj}). */
	@Column(unique = true, length = 20)
	private String document;
}
