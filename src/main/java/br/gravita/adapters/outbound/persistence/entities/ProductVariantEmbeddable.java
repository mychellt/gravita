package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantEmbeddable {
	@Column(name = "color")
	private String color;

	@Column(name = "size")
	private String size;

	@Column(name = "barcode")
	private String barcode;
}
