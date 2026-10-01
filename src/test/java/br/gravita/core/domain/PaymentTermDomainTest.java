package br.gravita.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTermDomainTest {

	@DisplayName("The number of installments is free and not fixed to 30/60/90 days")
	@Test
	void numberOfInstallmentsIsFreeAndNotFixedTo30_60_90() {
		PaymentTermDomain fiveInstallments = PaymentTermDomain.builder()
				.name("5x custom")
				.installmentIntervalsDays(List.of(15, 45, 75, 105, 135))
				.build();

		assertThat(fiveInstallments.getNumberOfInstallments()).isEqualTo(5);
		assertThat(fiveInstallments.getInstallmentIntervalsDays()).containsExactly(15, 45, 75, 105, 135);
	}

	@DisplayName("The number of installments is zero when no intervals are set")
	@Test
	void numberOfInstallmentsIsZeroWhenIntervalsNotSet() {
		PaymentTermDomain term = PaymentTermDomain.builder().name("Undefined").build();

		assertThat(term.getNumberOfInstallments()).isZero();
	}
}
