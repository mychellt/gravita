package br.gravita.adapters.outbound.persistence.entities.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class CostCenterShareEmbeddable {

	@Column(name = "cost_center_id", nullable = false)
	private UUID costCenterId;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal percent;
}
