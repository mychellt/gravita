package br.gravita.adapters.outbound.persistence.entities;

import br.gravita.core.domain.PermissionAction;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@Embeddable
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionJpaEntity {
	@Column(nullable = false)
	private String module;

	@Column(nullable = false)
	private String screen;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private PermissionAction action;
}
