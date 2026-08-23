package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DiscriminatorFormula;

import java.util.UUID;

/**
 * Single table for individuals and companies — no discriminator column;
 * the subtype is derived from the document itself (11-digit CPF or absent
 * = individual, 14-digit CNPJ = company).
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@Table(name = "persons")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when document is not null and length(document) = 14 then 'COMPANY' else 'INDIVIDUAL' end")
public abstract class PersonJpaEntity extends AbstractEntity<UUID> {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "UUID")
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(unique = true, length = 20)
	private String document;
}
