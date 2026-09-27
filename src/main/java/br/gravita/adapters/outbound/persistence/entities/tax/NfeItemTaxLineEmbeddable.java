package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.core.domain.tax.TaxType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One {@link br.gravita.core.domain.tax.TaxLineBreakdown} for one item, kept
 * flat (joined by {@code itemIndex} rather than nested inside
 * {@link NfeItemEmbeddable}) since JPA doesn't support a collection inside an
 * {@code @Embeddable} collection element. {@code TaxCalculationTotals} is not
 * persisted separately - it's rebuilt from these rows at read time via
 * {@code TaxCalculationTotals.from(...)}.
 */
@Getter
@Setter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NfeItemTaxLineEmbeddable {

	@Column(name = "item_index", nullable = false)
	private Integer itemIndex;

	@Enumerated(EnumType.STRING)
	@Column(name = "tax_type", nullable = false, length = 20)
	private TaxType taxType;

	@Column(name = "base", nullable = false)
	private BigDecimal base;

	@Column(name = "rate_percentage", nullable = false)
	private BigDecimal ratePercentage;

	@Column(name = "computed_amount", nullable = false)
	private BigDecimal computedAmount;

	@Column(name = "final_amount", nullable = false)
	private BigDecimal finalAmount;

	@Column(name = "overridden", nullable = false)
	private boolean overridden;

	@Column(name = "override_justification")
	private String overrideJustification;
}
