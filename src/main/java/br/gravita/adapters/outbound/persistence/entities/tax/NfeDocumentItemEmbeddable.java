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

/**
 * One {@code NfeItem} row. Tax amounts are flattened to one column per
 * {@code TaxType} (mirroring {@link InboundNfeItemEmbeddable}) rather than
 * persisting the full {@code TaxLineBreakdown} list - a nested collection
 * inside this collection's own embeddable isn't something plain JPA
 * supports; the final, post-override amount is what accounting/audit needs
 * at rest, while the full breakdown is available in-process during
 * issuance via {@code CalculateTaxUseCase}'s result.
 */
@Getter
@Setter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NfeDocumentItemEmbeddable {

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "quantity", nullable = false)
	private BigDecimal quantity;

	@Column(name = "unit_price", nullable = false)
	private BigDecimal unitPrice;

	@Column(name = "discount_percent", nullable = false)
	private BigDecimal discountPercent;

	@Column(name = "cfop", nullable = false)
	private String cfop;

	@Column(name = "icms_value", nullable = false)
	private BigDecimal icmsValue;

	@Column(name = "icms_st_value", nullable = false)
	private BigDecimal icmsStValue;

	@Column(name = "ipi_value", nullable = false)
	private BigDecimal ipiValue;

	@Column(name = "pis_value", nullable = false)
	private BigDecimal pisValue;

	@Column(name = "cofins_value", nullable = false)
	private BigDecimal cofinsValue;

	@Column(name = "fcp_value", nullable = false)
	private BigDecimal fcpValue;
}
