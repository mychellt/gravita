package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.persistence.CostCenterRepositoryPort;
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
class DeleteCostCenterAdapterTest {

	@Mock
	private CostCenterRepositoryPort costCenterRepositoryPort;

	@Mock
	private FinanceUsageQueryPort financeUsageQueryPort;

	@Test
	void shouldFailWhenCostCenterNotFound() {
		DeleteCostCenterAdapter adapter = new DeleteCostCenterAdapter(costCenterRepositoryPort, financeUsageQueryPort);
		UUID id = UUID.randomUUID();
		when(costCenterRepositoryPort.get(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldRejectDeletionWhenCostCenterHasChildren() {
		DeleteCostCenterAdapter adapter = new DeleteCostCenterAdapter(costCenterRepositoryPort, financeUsageQueryPort);
		UUID id = UUID.randomUUID();
		lenient().when(costCenterRepositoryPort.get(id)).thenReturn(Optional.of(CostCenterDomain.builder().id(id).build()));
		when(costCenterRepositoryPort.existsByParentId(id)).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(id)))
				.isInstanceOf(BusinessRuleException.class);

		verify(costCenterRepositoryPort, never()).deleteById(id);
	}

	@Test
	void shouldRejectDeletionWhenInUseByFinance() {
		DeleteCostCenterAdapter adapter = new DeleteCostCenterAdapter(costCenterRepositoryPort, financeUsageQueryPort);
		UUID id = UUID.randomUUID();
		when(costCenterRepositoryPort.get(id)).thenReturn(Optional.of(CostCenterDomain.builder().id(id).build()));
		when(costCenterRepositoryPort.existsByParentId(id)).thenReturn(false);
		when(financeUsageQueryPort.isCostCenterInUse(id)).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(id)))
				.isInstanceOf(BusinessRuleException.class);

		verify(costCenterRepositoryPort, never()).deleteById(id);
	}

	@Test
	void shouldDeleteWhenLeafAndNotInUse() {
		DeleteCostCenterAdapter adapter = new DeleteCostCenterAdapter(costCenterRepositoryPort, financeUsageQueryPort);
		UUID id = UUID.randomUUID();
		when(costCenterRepositoryPort.get(id)).thenReturn(Optional.of(CostCenterDomain.builder().id(id).build()));
		when(costCenterRepositoryPort.existsByParentId(id)).thenReturn(false);
		when(financeUsageQueryPort.isCostCenterInUse(id)).thenReturn(false);

		adapter.execute(new Context(id));

		verify(costCenterRepositoryPort).deleteById(id);
	}
}
