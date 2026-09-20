package br.gravita.masterdata.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
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
public class SupplierAddressEmbeddable {

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
}
