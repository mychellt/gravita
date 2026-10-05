package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Payment extends AbstractDomain {
	private BigDecimal amount;
	private LocalDate paymentDate;

	@Builder.Default
	private PaymentStatus status = PaymentStatus.PENDING;

	public static Payment request(final BigDecimal amount) {
		return Payment.builder()
				.amount(amount)
				.status(PaymentStatus.PENDING)
				.build();
	}

	public void confirm() {
		if (status != PaymentStatus.PENDING) {
			throw new BusinessRuleException("Only a pending payment can be confirmed");
		}
		this.status = PaymentStatus.CONFIRMED;
		this.paymentDate = LocalDate.now();
	}

	public void fail() {
		if (status != PaymentStatus.PENDING) {
			throw new BusinessRuleException("Only a pending payment can fail");
		}
		this.status = PaymentStatus.FAILED;
	}
}
