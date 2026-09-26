package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.core.domain.PaymentMethodType;
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
public class PaymentEmbeddable {

	@Enumerated(EnumType.STRING)
	@Column(name = "method", nullable = false, length = 20)
	private PaymentMethodType method;

	@Column(name = "amount", nullable = false)
	private BigDecimal amount;
}
