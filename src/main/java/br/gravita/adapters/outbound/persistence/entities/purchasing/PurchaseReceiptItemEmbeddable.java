package br.gravita.adapters.outbound.persistence.entities.purchasing;

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
public class PurchaseReceiptItemEmbeddable {

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "ordered_qty", nullable = false)
	private BigDecimal orderedQty;

	@Column(name = "received_qty", nullable = false)
	private BigDecimal receivedQty;
}
