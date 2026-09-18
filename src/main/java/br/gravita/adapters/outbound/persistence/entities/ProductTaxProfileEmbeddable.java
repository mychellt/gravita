package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ProductTaxProfileEmbeddable {
	@Column(name = "tax_icms_rate", precision = 7, scale = 4)
	private BigDecimal icmsRate;

	@Column(name = "tax_ipi_rate", precision = 7, scale = 4)
	private BigDecimal ipiRate;

	@Column(name = "tax_pis_rate", precision = 7, scale = 4)
	private BigDecimal pisRate;

	@Column(name = "tax_cofins_rate", precision = 7, scale = 4)
	private BigDecimal cofinsRate;

	@Column(name = "tax_icms_st_rate", precision = 7, scale = 4)
	private BigDecimal icmsStRate;

	@Column(name = "tax_fcp_rate", precision = 7, scale = 4)
	private BigDecimal fcpRate;
}
