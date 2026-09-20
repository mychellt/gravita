package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PaymentTermDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.outbound.persistence.PaymentTermRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePaymentTermAdapterTest {

	@Mock
	private PaymentTermRepositoryPort paymentTermRepositoryPort;

	@Test
	void shouldFailWhenPaymentTermNotFound() {
		UpdatePaymentTermAdapter adapter = new UpdatePaymentTermAdapter(paymentTermRepositoryPort);
		UUID id = UUID.randomUUID();
		PaymentTermDomain command = PaymentTermDomain.builder().id(id).name("30/60/90").installmentIntervalsDays(List.of(30, 60, 90)).build();
		when(paymentTermRepositoryPort.get(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldRejectEmptyInstallmentsOnUpdate() {
		UpdatePaymentTermAdapter adapter = new UpdatePaymentTermAdapter(paymentTermRepositoryPort);
		UUID id = UUID.randomUUID();
		PaymentTermDomain command = PaymentTermDomain.builder().id(id).name("Invalid").installmentIntervalsDays(List.of()).build();

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldUpdateInstallmentCountAndIntervalsFreely() {
		UpdatePaymentTermAdapter adapter = new UpdatePaymentTermAdapter(paymentTermRepositoryPort);
		UUID id = UUID.randomUUID();
		List<Integer> newIntervals = List.of(10, 20, 30, 40, 50);
		PaymentTermDomain command = PaymentTermDomain.builder().id(id).name("5x").installmentIntervalsDays(newIntervals).build();
		when(paymentTermRepositoryPort.get(id)).thenReturn(Optional.of(PaymentTermDomain.builder().id(id).build()));
		when(paymentTermRepositoryPort.save(command)).thenReturn(command);

		PaymentTermDomain result = adapter.execute(new Context(command));

		assertThat(result.getNumberOfInstallments()).isEqualTo(5);
		assertThat(result.getInstallmentIntervalsDays()).containsExactlyElementsOf(newIntervals);
	}
}
