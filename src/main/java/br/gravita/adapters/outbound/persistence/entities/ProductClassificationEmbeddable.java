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
public class ProductClassificationEmbeddable {
	@Column(name = "classification_group")
	private String group;

	@Column(name = "classification_subgroup")
	private String subgroup;

	@Column(name = "classification_brand")
	private String brand;

	@Column(name = "classification_section")
	private String section;
}
