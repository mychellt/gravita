package br.gravita.adapters.outbound.persistence.entities;

import br.gravita.core.domain.ContactType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerContactEmbeddable {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ContactType type;

	@Column(nullable = false)
	private String value;
}
