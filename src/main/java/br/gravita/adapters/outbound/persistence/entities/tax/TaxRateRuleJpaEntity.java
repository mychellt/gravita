package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.domain.tax.TaxType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tax_rate_rules")
public class TaxRateRuleJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(nullable = false, length = 8)
	private String ncm;

	@Column(name = "origin_state", nullable = false, length = 2)
	private String originState;

	@Column(name = "destination_state", nullable = false, length = 2)
	private String destinationState;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaxRegime regime;

	@Column(name = "operation_type", nullable = false, length = 30)
	private String operationType;

	@Enumerated(EnumType.STRING)
	@Column(name = "tax_type", nullable = false, length = 10)
	private TaxType taxType;

	@Column(name = "rate_percentage", nullable = false, precision = 7, scale = 4)
	private BigDecimal ratePercentage;

	@Column(name = "base_reduction_percentage", nullable = false, precision = 7, scale = 4)
	private BigDecimal baseReductionPercentage;

	@Column(name = "mva_percentage", nullable = false, precision = 7, scale = 4)
	private BigDecimal mvaPercentage;
}
