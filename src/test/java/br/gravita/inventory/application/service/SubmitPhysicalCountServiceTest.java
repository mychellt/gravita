package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.PhysicalCountStatus;
import br.gravita.core.ports.inbound.inventory.SubmitPhysicalCountCommand;
import br.gravita.core.ports.outbound.persistence.inventory.PhysicalCountRepositoryPort;
import br.gravita.core.usercases.inventory.SubmitPhysicalCountService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmitPhysicalCountServiceTest {

	@Mock
	private PhysicalCountRepositoryPort physicalCountRepositoryPort;

	private SubmitPhysicalCountService service;

	private final UUID warehouseId = UUID.randomUUID();
	private final UUID startedBy = UUID.randomUUID();
	private final UUID submittedBy = UUID.randomUUID();
	private final PhysicalCountId physicalCountId = PhysicalCountId.of(UUID.randomUUID());

	@BeforeEach
	void setUp() {
		service = new SubmitPhysicalCountService(physicalCountRepositoryPort);
	}

	@Test
	@DisplayName("Submitting counts for every line moves the count to PENDING_APPROVAL")
	void submittingCountsForEveryLineMovesTheCountToPendingApproval() {
		final UUID product = UUID.randomUUID();
		final PhysicalCount inProgress = physicalCount(List.of(new PhysicalCountLine(product, new BigDecimal("10"))));
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(inProgress));
		when(physicalCountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final PhysicalCount result = service.execute(
				new SubmitPhysicalCountCommand(physicalCountId, Map.of(product, new BigDecimal("9")), submittedBy));

		assertThat(result.getStatus()).isEqualTo(PhysicalCountStatus.PENDING_APPROVAL);
	}

	@Test
	@DisplayName("Rejects submitting counts for a count that does not exist")
	void rejectsSubmittingCountsForAnUnknownCount() {
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new SubmitPhysicalCountCommand(physicalCountId,
				Map.of(UUID.randomUUID(), BigDecimal.TEN), submittedBy)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("Rejects submitting counts for a count that is not in progress")
	void rejectsSubmittingCountsForACountThatIsNotInProgress() {
		final UUID product = UUID.randomUUID();
		final PhysicalCount pending = PhysicalCount.of(physicalCountId, PhysicalCountScope.TOTAL, null, warehouseId,
				PhysicalCountStatus.PENDING_APPROVAL, startedBy, Instant.now(),
				List.of(new PhysicalCountLine(product, new BigDecimal("10"))));
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(pending));

		assertThatThrownBy(() -> service.execute(
				new SubmitPhysicalCountCommand(physicalCountId, Map.of(product, BigDecimal.TEN), submittedBy)))
				.isInstanceOf(BusinessRuleException.class);
	}

	private PhysicalCount physicalCount(final List<PhysicalCountLine> lines) {
		return PhysicalCount.of(physicalCountId, PhysicalCountScope.TOTAL, null, warehouseId,
				PhysicalCountStatus.IN_PROGRESS, startedBy, Instant.now(), lines);
	}
}
