package br.gravita.adapters.outbound.persistence.entities.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;
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
public class PhysicalCountLineEmbeddable {

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "system_quantity", nullable = false, precision = 14, scale = 4)
	private BigDecimal systemQuantity;
}
