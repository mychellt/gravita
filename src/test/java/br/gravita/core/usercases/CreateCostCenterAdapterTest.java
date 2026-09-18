package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.persistence.CostCenterRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCostCenterAdapterTest {

	@Mock
	private CostCenterRepositoryPort costCenterRepositoryPort;

	@Test
	void shouldCreateRootCostCenterAndAssignId() {
		CreateCostCenterAdapter adapter = new CreateCostCenterAdapter(costCenterRepositoryPort);
		CostCenterDomain command = CostCenterDomain.builder().code("CC-01").name("Administrative").build();
		when(costCenterRepositoryPort.save(command)).thenAnswer(invocation -> invocation.getArgument(0));

		CostCenterDomain result = adapter.execute(new Context(command));

		assertThat(result.getId()).isNotNull();
		verify(costCenterRepositoryPort).save(command);
	}

	@Test
	void shouldCreateChildCostCenterWhenParentExists() {
		CreateCostCenterAdapter adapter = new CreateCostCenterAdapter(costCenterRepositoryPort);
		UUID parentId = UUID.randomUUID();
		CostCenterDomain command = CostCenterDomain.builder().code("CC-02").name("Sales").parentId(parentId).build();
		when(costCenterRepositoryPort.get(parentId)).thenReturn(Optional.of(CostCenterDomain.builder().id(parentId).build()));
		when(costCenterRepositoryPort.save(command)).thenAnswer(invocation -> invocation.getArgument(0));

		CostCenterDomain result = adapter.execute(new Context(command));

		assertThat(result.getParentId()).isEqualTo(parentId);
	}

	@Test
	void shouldRejectWhenParentCostCenterDoesNotExist() {
		CreateCostCenterAdapter adapter = new CreateCostCenterAdapter(costCenterRepositoryPort);
		UUID parentId = UUID.randomUUID();
		CostCenterDomain command = CostCenterDomain.builder().code("CC-03").name("Marketing").parentId(parentId).build();
		when(costCenterRepositoryPort.get(parentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(BusinessRuleException.class);
	}
}
