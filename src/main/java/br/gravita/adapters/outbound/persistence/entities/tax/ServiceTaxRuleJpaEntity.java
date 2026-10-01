package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.WithholdingMode;
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
@Table(name = "service_tax_rules")
public class ServiceTaxRuleJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "service_code", nullable = false, length = 5)
	private String serviceCode;

	/** {@code null} = the rule applies to every municipality. */
	@Column(name = "municipality_ibge", length = 7)
	private String municipalityIbge;

	/** {@code null} = the rule applies to every provider tax regime. */
	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private TaxRegime regime;

	@Enumerated(EnumType.STRING)
	@Column(name = "tax_type", nullable = false, length = 20)
	private TaxType taxType;

	@Column(name = "rate_percentage", nullable = false, precision = 7, scale = 4)
	private BigDecimal ratePercentage;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WithholdingMode withholding;
}
