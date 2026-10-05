package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCostCenterAdapterTest {

	@Mock
	private CostCenterRepositoryPort costCenterRepositoryPort;

	@DisplayName("Fails with not found when the cost center to update does not exist")
	@Test
	void shouldFailWhenCostCenterNotFound() {
		final UpdateCostCenterAdapter adapter = new UpdateCostCenterAdapter(costCenterRepositoryPort);
		final UUID id = UUID.randomUUID();
		final CostCenterDomain command = CostCenterDomain.builder().id(id).code("CC-01").name("Administrative").build();
		when(costCenterRepositoryPort.get(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@DisplayName("Rejects setting a cost center as its own parent")
	@Test
	void shouldRejectSelfAsParent() {
		final UpdateCostCenterAdapter adapter = new UpdateCostCenterAdapter(costCenterRepositoryPort);
		final UUID id = UUID.randomUUID();
		final CostCenterDomain command = CostCenterDomain.builder().id(id).code("CC-01").name("Administrative").parentId(id).build();
		lenient().when(costCenterRepositoryPort.get(id)).thenReturn(Optional.of(CostCenterDomain.builder().id(id).build()));

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@DisplayName("Rejects a new parent cost center that does not exist")
	@Test
	void shouldRejectWhenNewParentDoesNotExist() {
		final UpdateCostCenterAdapter adapter = new UpdateCostCenterAdapter(costCenterRepositoryPort);
		final UUID id = UUID.randomUUID();
		final UUID parentId = UUID.randomUUID();
		final CostCenterDomain command = CostCenterDomain.builder().id(id).code("CC-01").name("Administrative").parentId(parentId).build();
		when(costCenterRepositoryPort.get(id)).thenReturn(Optional.of(CostCenterDomain.builder().id(id).build()));
		when(costCenterRepositoryPort.get(parentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@DisplayName("Updates the cost center when the change is valid")
	@Test
	void shouldUpdateWhenValid() {
		final UpdateCostCenterAdapter adapter = new UpdateCostCenterAdapter(costCenterRepositoryPort);
		final UUID id = UUID.randomUUID();
		final CostCenterDomain command = CostCenterDomain.builder().id(id).code("CC-01").name("Renamed").build();
		when(costCenterRepositoryPort.get(id)).thenReturn(Optional.of(CostCenterDomain.builder().id(id).build()));
		when(costCenterRepositoryPort.save(command)).thenReturn(command);

		final CostCenterDomain result = adapter.execute(new Context(command));

		assertThat(result.getName()).isEqualTo("Renamed");
	}
}
