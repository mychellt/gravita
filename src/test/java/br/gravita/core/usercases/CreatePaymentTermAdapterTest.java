package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentTermDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.outbound.persistence.PaymentTermRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePaymentTermAdapterTest {

	@Mock
	private PaymentTermRepositoryPort paymentTermRepositoryPort;

	@DisplayName("Supports any number of installments with custom intervals")
	@Test
	void shouldSupportAFreeNumberOfInstallmentsWithCustomIntervals() {
		CreatePaymentTermAdapter adapter = new CreatePaymentTermAdapter(paymentTermRepositoryPort);
		List<Integer> intervals = List.of(0, 15, 45, 75, 90, 120, 150);
		PaymentTermDomain command = PaymentTermDomain.builder().name("7x custom").installmentIntervalsDays(intervals).build();
		when(paymentTermRepositoryPort.save(command)).thenAnswer(invocation -> invocation.getArgument(0));

		PaymentTermDomain result = adapter.execute(new Context(command));

		assertThat(result.getId()).isNotNull();
		assertThat(result.getNumberOfInstallments()).isEqualTo(7);
		assertThat(result.getInstallmentIntervalsDays()).containsExactlyElementsOf(intervals);
	}

	@DisplayName("Rejects a payment term that has no installments")
	@Test
	void shouldRejectPaymentTermWithNoInstallments() {
		CreatePaymentTermAdapter adapter = new CreatePaymentTermAdapter(paymentTermRepositoryPort);
		PaymentTermDomain command = PaymentTermDomain.builder().name("Invalid").installmentIntervalsDays(List.of()).build();

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(BusinessRuleException.class);
	}
}
