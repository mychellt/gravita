package br.gravita.adapters.outbound.persistence.entities;

import br.gravita.core.domain.AddressType;
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
public class CustomerAddressEmbeddable {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AddressType type;

	@Column(nullable = false)
	private String street;

	private String number;

	private String complement;

	@Column(nullable = false)
	private String neighborhood;

	@Column(nullable = false)
	private String city;

	@Column(nullable = false, length = 2)
	private String state;

	@Column(name = "zip_code", nullable = false, length = 10)
	private String zipCode;

	@Column(name = "is_default", nullable = false)
	private boolean isDefault;
}
