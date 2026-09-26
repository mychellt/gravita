package br.gravita.adapters.outbound.persistence.entities.tax;

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
public class SaleItemEmbeddable {

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "quantity", nullable = false)
	private BigDecimal quantity;

	@Column(name = "unit_price", nullable = false)
	private BigDecimal unitPrice;

	@Column(name = "item_discount", nullable = false)
	private BigDecimal itemDiscount;
}
