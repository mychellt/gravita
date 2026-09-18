package br.gravita.core.usercases;

import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.persistence.ChartOfAccountsRepositoryPort;
import br.gravita.core.ports.persistence.FinanceUsageQueryPort;
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

	@Test
	void shouldFailWhenAccountNotFound() {
		DeleteChartOfAccountsAdapter adapter = new DeleteChartOfAccountsAdapter(chartOfAccountsRepositoryPort, financeUsageQueryPort);
		UUID id = UUID.randomUUID();
		when(chartOfAccountsRepositoryPort.get(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldRejectDeletionWhenAccountHasChildren() {
		DeleteChartOfAccountsAdapter adapter = new DeleteChartOfAccountsAdapter(chartOfAccountsRepositoryPort, financeUsageQueryPort);
		UUID id = UUID.randomUUID();
		lenient().when(chartOfAccountsRepositoryPort.get(id)).thenReturn(Optional.of(ChartOfAccountsDomain.builder().id(id).build()));
		when(chartOfAccountsRepositoryPort.existsByParentId(id)).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(id)))
				.isInstanceOf(BusinessRuleException.class);

		verify(chartOfAccountsRepositoryPort, never()).deleteById(id);
	}

	@Test
	void shouldRejectDeletionWhenInUseByFinance() {
		DeleteChartOfAccountsAdapter adapter = new DeleteChartOfAccountsAdapter(chartOfAccountsRepositoryPort, financeUsageQueryPort);
		UUID id = UUID.randomUUID();
		when(chartOfAccountsRepositoryPort.get(id)).thenReturn(Optional.of(ChartOfAccountsDomain.builder().id(id).build()));
		when(chartOfAccountsRepositoryPort.existsByParentId(id)).thenReturn(false);
		when(financeUsageQueryPort.isChartOfAccountInUse(id)).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(id)))
				.isInstanceOf(BusinessRuleException.class);

		verify(chartOfAccountsRepositoryPort, never()).deleteById(id);
	}

	@Test
	void shouldDeleteWhenLeafAndNotInUse() {
		DeleteChartOfAccountsAdapter adapter = new DeleteChartOfAccountsAdapter(chartOfAccountsRepositoryPort, financeUsageQueryPort);
		UUID id = UUID.randomUUID();
		when(chartOfAccountsRepositoryPort.get(id)).thenReturn(Optional.of(ChartOfAccountsDomain.builder().id(id).build()));
		when(chartOfAccountsRepositoryPort.existsByParentId(id)).thenReturn(false);
		when(financeUsageQueryPort.isChartOfAccountInUse(id)).thenReturn(false);

		adapter.execute(new Context(id));

		verify(chartOfAccountsRepositoryPort).deleteById(id);
	}
}
