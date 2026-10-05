package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.outbound.persistence.CostCenterRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

	@DisplayName("Creates a root cost center and assigns it an id")
	@Test
	void shouldCreateRootCostCenterAndAssignId() {
		final CreateCostCenterAdapter adapter = new CreateCostCenterAdapter(costCenterRepositoryPort);
		final CostCenterDomain command = CostCenterDomain.builder().code("CC-01").name("Administrative").build();
		when(costCenterRepositoryPort.save(command)).thenAnswer(invocation -> invocation.getArgument(0));

		final CostCenterDomain result = adapter.execute(new Context(command));

		assertThat(result.getId()).isNotNull();
		verify(costCenterRepositoryPort).save(command);
	}

	@DisplayName("Creates a child cost center when its parent exists")
	@Test
	void shouldCreateChildCostCenterWhenParentExists() {
		final CreateCostCenterAdapter adapter = new CreateCostCenterAdapter(costCenterRepositoryPort);
		final UUID parentId = UUID.randomUUID();
		final CostCenterDomain command = CostCenterDomain.builder().code("CC-02").name("Sales").parentId(parentId).build();
		when(costCenterRepositoryPort.get(parentId)).thenReturn(Optional.of(CostCenterDomain.builder().id(parentId).build()));
		when(costCenterRepositoryPort.save(command)).thenAnswer(invocation -> invocation.getArgument(0));

		final CostCenterDomain result = adapter.execute(new Context(command));

		assertThat(result.getParentId()).isEqualTo(parentId);
	}

	@DisplayName("Rejects creating a cost center under a parent that does not exist")
	@Test
	void shouldRejectWhenParentCostCenterDoesNotExist() {
		final CreateCostCenterAdapter adapter = new CreateCostCenterAdapter(costCenterRepositoryPort);
		final UUID parentId = UUID.randomUUID();
		final CostCenterDomain command = CostCenterDomain.builder().code("CC-03").name("Marketing").parentId(parentId).build();
		when(costCenterRepositoryPort.get(parentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(BusinessRuleException.class);
	}
}
