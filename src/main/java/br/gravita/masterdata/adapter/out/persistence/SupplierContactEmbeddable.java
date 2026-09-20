package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.domain.model.ContactType;
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
public class SupplierContactEmbeddable {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ContactType type;

	@Column(name = "contact_value", nullable = false)
	private String value;
}
