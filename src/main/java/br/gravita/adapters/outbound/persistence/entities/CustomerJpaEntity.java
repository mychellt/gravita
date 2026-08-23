package br.gravita.adapters.outbound.persistence.entities;

import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.PersonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Setter
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customers")
public class CustomerJpaEntity extends AbstractEntity<UUID> {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "UUID")
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
}
