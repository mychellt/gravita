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
public class InboundNfeConferenceItemEmbeddable {

	@Column(name = "item_ref", nullable = false)
	private UUID itemRef;

	@Column(name = "ordered_qty", nullable = false)
	private BigDecimal orderedQty;

	@Column(name = "received_qty", nullable = false)
	private BigDecimal receivedQty;
}
