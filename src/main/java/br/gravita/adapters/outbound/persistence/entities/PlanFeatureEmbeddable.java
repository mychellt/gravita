package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeatureEmbeddable {

	@Column(name = "label", nullable = false)
	private String label;

	@Column(name = "included", nullable = false)
	private boolean included;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;
}
