package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ProductKitComponentEmbeddable {
	@Column(name = "component_product_id", nullable = false)
	private UUID productId;

	@Column(name = "quantity", nullable = false, precision = 12, scale = 3)
	private BigDecimal quantity;
}
