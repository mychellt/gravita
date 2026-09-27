package br.gravita.adapters.outbound.persistence.entities.purchasing;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class QuotationResponseLineEmbeddable {

	@Column(name = "supplier_id", nullable = false)
	private UUID supplierId;

	@Column(name = "deadline", nullable = false)
	private LocalDate deadline;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "unit_price", nullable = false)
	private BigDecimal unitPrice;
}
