package br.gravita.adapters.outbound.persistence.entities.sales;

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
public class SalesOrderItemEmbeddable {

	@Column(name = "product_or_service_id", nullable = false)
	private UUID productOrServiceId;

	@Column(name = "quantity", nullable = false, precision = 14, scale = 4)
	private BigDecimal quantity;

	@Column(name = "unit_price", nullable = false, precision = 14, scale = 4)
	private BigDecimal unitPrice;

	@Column(name = "discount", nullable = false, precision = 14, scale = 4)
	private BigDecimal discount;
}
