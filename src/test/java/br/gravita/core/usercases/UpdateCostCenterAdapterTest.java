package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.persistence.CostCenterRepositoryPort;
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

	@Test
	void shouldFailWhenCostCenterNotFound() {
		UpdateCostCenterAdapter adapter = new UpdateCostCenterAdapter(costCenterRepositoryPort);
		UUID id = UUID.randomUUID();
		CostCenterDomain command = CostCenterDomain.builder().id(id).code("CC-01").name("Administrative").build();
		when(costCenterRepositoryPort.get(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldRejectSelfAsParent() {
		UpdateCostCenterAdapter adapter = new UpdateCostCenterAdapter(costCenterRepositoryPort);
		UUID id = UUID.randomUUID();
		CostCenterDomain command = CostCenterDomain.builder().id(id).code("CC-01").name("Administrative").parentId(id).build();
		lenient().when(costCenterRepositoryPort.get(id)).thenReturn(Optional.of(CostCenterDomain.builder().id(id).build()));

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldRejectWhenNewParentDoesNotExist() {
		UpdateCostCenterAdapter adapter = new UpdateCostCenterAdapter(costCenterRepositoryPort);
		UUID id = UUID.randomUUID();
		UUID parentId = UUID.randomUUID();
		CostCenterDomain command = CostCenterDomain.builder().id(id).code("CC-01").name("Administrative").parentId(parentId).build();
		when(costCenterRepositoryPort.get(id)).thenReturn(Optional.of(CostCenterDomain.builder().id(id).build()));
		when(costCenterRepositoryPort.get(parentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(command)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldUpdateWhenValid() {
		UpdateCostCenterAdapter adapter = new UpdateCostCenterAdapter(costCenterRepositoryPort);
		UUID id = UUID.randomUUID();
		CostCenterDomain command = CostCenterDomain.builder().id(id).code("CC-01").name("Renamed").build();
		when(costCenterRepositoryPort.get(id)).thenReturn(Optional.of(CostCenterDomain.builder().id(id).build()));
		when(costCenterRepositoryPort.save(command)).thenReturn(command);

		CostCenterDomain result = adapter.execute(new Context(command));

		assertThat(result.getName()).isEqualTo("Renamed");
	}
}
