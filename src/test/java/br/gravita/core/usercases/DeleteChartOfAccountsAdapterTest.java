package br.gravita.core.usercases;

import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.outbound.persistence.ChartOfAccountsRepositoryPort;
import br.gravita.core.ports.outbound.persistence.FinanceUsageQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteChartOfAccountsAdapterTest {

	@Mock
	private ChartOfAccountsRepositoryPort chartOfAccountsRepositoryPort;

	@Mock
	private FinanceUsageQueryPort financeUsageQueryPort;

	@DisplayName("Fails with not found when the chart-of-accounts entry to delete does not exist")
	@Test
	void shouldFailWhenAccountNotFound() {
		final DeleteChartOfAccountsAdapter adapter = new DeleteChartOfAccountsAdapter(chartOfAccountsRepositoryPort, financeUsageQueryPort);
		final UUID id = UUID.randomUUID();
		when(chartOfAccountsRepositoryPort.get(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@DisplayName("Rejects deleting an account that still has child accounts")
	@Test
	void shouldRejectDeletionWhenAccountHasChildren() {
		final DeleteChartOfAccountsAdapter adapter = new DeleteChartOfAccountsAdapter(chartOfAccountsRepositoryPort, financeUsageQueryPort);
		final UUID id = UUID.randomUUID();
		lenient().when(chartOfAccountsRepositoryPort.get(id)).thenReturn(Optional.of(ChartOfAccountsDomain.builder().id(id).build()));
		when(chartOfAccountsRepositoryPort.existsByParentId(id)).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(id)))
				.isInstanceOf(BusinessRuleException.class);

		verify(chartOfAccountsRepositoryPort, never()).deleteById(id);
	}

	@DisplayName("Rejects deleting an account that Finance reports as in use")
	@Test
	void shouldRejectDeletionWhenInUseByFinance() {
		final DeleteChartOfAccountsAdapter adapter = new DeleteChartOfAccountsAdapter(chartOfAccountsRepositoryPort, financeUsageQueryPort);
		final UUID id = UUID.randomUUID();
		when(chartOfAccountsRepositoryPort.get(id)).thenReturn(Optional.of(ChartOfAccountsDomain.builder().id(id).build()));
		when(chartOfAccountsRepositoryPort.existsByParentId(id)).thenReturn(false);
		when(financeUsageQueryPort.isChartOfAccountInUse(id)).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(id)))
				.isInstanceOf(BusinessRuleException.class);

		verify(chartOfAccountsRepositoryPort, never()).deleteById(id);
	}

	@DisplayName("Deletes a leaf account that is not in use by Finance")
	@Test
	void shouldDeleteWhenLeafAndNotInUse() {
		final DeleteChartOfAccountsAdapter adapter = new DeleteChartOfAccountsAdapter(chartOfAccountsRepositoryPort, financeUsageQueryPort);
		final UUID id = UUID.randomUUID();
		when(chartOfAccountsRepositoryPort.get(id)).thenReturn(Optional.of(ChartOfAccountsDomain.builder().id(id).build()));
		when(chartOfAccountsRepositoryPort.existsByParentId(id)).thenReturn(false);
		when(financeUsageQueryPort.isChartOfAccountInUse(id)).thenReturn(false);

		adapter.execute(new Context(id));

		verify(chartOfAccountsRepositoryPort).deleteById(id);
	}
}
