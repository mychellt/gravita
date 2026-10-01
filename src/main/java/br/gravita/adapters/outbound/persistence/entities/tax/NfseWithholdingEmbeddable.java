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

@Getter
@Setter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NfseWithholdingEmbeddable {

	@Enumerated(EnumType.STRING)
	@Column(name = "tax_type", nullable = false, length = 20)
	private TaxType taxType;

	@Column(name = "base", nullable = false)
	private BigDecimal base;

	@Column(name = "rate_percentage", nullable = false)
	private BigDecimal ratePercentage;

	@Column(name = "amount", nullable = false)
	private BigDecimal amount;
}
