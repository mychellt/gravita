package br.gravita.adapters.outbound.persistence.adapters.sales;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.mappers.sales.FollowUpTaskPersistenceMapperImpl;
import br.gravita.core.domain.sales.AlertChannel;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.domain.sales.FollowUpTaskId;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({FollowUpTaskRepositoryAdapter.class, FollowUpTaskPersistenceMapperImpl.class})
class FollowUpTaskRepositoryAdapterTest {

	@Autowired
	private FollowUpTaskRepositoryAdapter repositoryAdapter;

	@Test
	@DisplayName("Matches a previously saved opportunity task when checking existence for a target on a date")
	void existsForTargetOnDateMatchesAPreviouslySavedOpportunityTask() {
		UUID opportunityId = UUID.randomUUID();
		LocalDate dueDate = LocalDate.now();
		repositoryAdapter.save(FollowUpTask.of(FollowUpTaskId.of(UUID.randomUUID()), opportunityId, null, dueDate,
				UUID.randomUUID(), AlertChannel.APP));

		assertThat(repositoryAdapter.existsForTargetOnDate(opportunityId, null, dueDate)).isTrue();
		assertThat(repositoryAdapter.existsForTargetOnDate(opportunityId, null, dueDate.minusDays(1))).isFalse();
		assertThat(repositoryAdapter.existsForTargetOnDate(UUID.randomUUID(), null, dueDate)).isFalse();
	}

	@Test
	@DisplayName("Matches a previously saved customer task when checking existence for a target on a date")
	void existsForTargetOnDateMatchesAPreviouslySavedCustomerTask() {
		UUID customerId = UUID.randomUUID();
		LocalDate dueDate = LocalDate.now();
		repositoryAdapter.save(FollowUpTask.of(FollowUpTaskId.of(UUID.randomUUID()), null, customerId, dueDate,
				UUID.randomUUID(), AlertChannel.EMAIL));

		assertThat(repositoryAdapter.existsForTargetOnDate(null, customerId, dueDate)).isTrue();
		assertThat(repositoryAdapter.existsForTargetOnDate(null, UUID.randomUUID(), dueDate)).isFalse();
	}
}
