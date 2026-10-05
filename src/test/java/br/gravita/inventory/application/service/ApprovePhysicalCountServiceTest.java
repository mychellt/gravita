package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.PhysicalCountStatus;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementId;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.ports.inbound.inventory.AdjustInventoryCommand;
import br.gravita.core.ports.inbound.inventory.AdjustInventoryUseCase;
import br.gravita.core.ports.inbound.inventory.ApprovePhysicalCountCommand;
import br.gravita.core.ports.outbound.persistence.inventory.PhysicalCountRepositoryPort;
import br.gravita.core.usercases.inventory.ApprovePhysicalCountService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovePhysicalCountServiceTest {

	@Mock
	private PhysicalCountRepositoryPort physicalCountRepositoryPort;

	@Mock
	private AdjustInventoryUseCase adjustInventoryUseCase;

	private ApprovePhysicalCountService service;

	private final UUID warehouseId = UUID.randomUUID();
	private final UUID startedBy = UUID.randomUUID();
	private final UUID approvedBy = UUID.randomUUID();
	private final PhysicalCountId physicalCountId = PhysicalCountId.of(UUID.randomUUID());

	@BeforeEach
	void setUp() {
		service = new ApprovePhysicalCountService(physicalCountRepositoryPort, adjustInventoryUseCase);
	}

	@Test
	@DisplayName("AC1: Rejects approving a count that is still in progress")
	void ac1RejectsApprovingACountThatIsStillInProgress() {
		final PhysicalCount inProgress = physicalCount(PhysicalCountStatus.IN_PROGRESS, List.of());
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(inProgress));

		assertThatThrownBy(() -> service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy)))
				.isInstanceOf(BusinessRuleException.class);

		verifyNoInteractions(adjustInventoryUseCase);
		verify(physicalCountRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("AC1: Rejects approving a count that is already approved")
	void ac1RejectsApprovingACountThatIsAlreadyApproved() {
		final PhysicalCount approved = physicalCount(PhysicalCountStatus.APPROVED, List.of());
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(approved));

		assertThatThrownBy(() -> service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy)))
				.isInstanceOf(BusinessRuleException.class);

		verifyNoInteractions(adjustInventoryUseCase);
		verify(physicalCountRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects approving a count that does not exist")
	void rejectsApprovingAnUnknownCount() {
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("AC2: Generates one adjustment per divergent product and none for products whose count matches")
	void ac2GeneratesExactlyOneAdjustmentPerDivergentProductAndNoneForMatchingOnes() {
		final UUID divergentProduct = UUID.randomUUID();
		final UUID matchingProduct = UUID.randomUUID();
		final PhysicalCount pending = physicalCount(PhysicalCountStatus.PENDING_APPROVAL,
				List.of(new PhysicalCountLine(divergentProduct, new BigDecimal("10"), new BigDecimal("7")),
						new PhysicalCountLine(matchingProduct, new BigDecimal("20"), new BigDecimal("20"))));
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(pending));
		when(physicalCountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(adjustInventoryUseCase.execute(any())).thenReturn(mockMovement());

		service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy));

		final ArgumentCaptor<AdjustInventoryCommand> captor = ArgumentCaptor.forClass(AdjustInventoryCommand.class);
		verify(adjustInventoryUseCase, times(1)).execute(captor.capture());
		assertThat(captor.getValue().productId()).isEqualTo(divergentProduct);
		assertThat(captor.getValue().quantityDelta()).isEqualByComparingTo("-3");
		assertThat(captor.getValue().warehouseId()).isEqualTo(warehouseId);
		assertThat(captor.getValue().user()).isEqualTo(approvedBy);
	}

	@Test
	@DisplayName("AC3: Each adjustment justification references the physical count that originated it")
	void ac3TheAdjustmentJustificationReferencesThePhysicalCount() {
		final UUID divergentProduct = UUID.randomUUID();
		final PhysicalCount pending = physicalCount(PhysicalCountStatus.PENDING_APPROVAL,
				List.of(new PhysicalCountLine(divergentProduct, new BigDecimal("10"), new BigDecimal("15"))));
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(pending));
		when(physicalCountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(adjustInventoryUseCase.execute(any())).thenReturn(mockMovement());

		service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy));

		final ArgumentCaptor<AdjustInventoryCommand> captor = ArgumentCaptor.forClass(AdjustInventoryCommand.class);
		verify(adjustInventoryUseCase).execute(captor.capture());
		assertThat(captor.getValue().justification()).contains(physicalCountId.value().toString());
	}

	@Test
	@DisplayName("A count with no divergences is still moved to APPROVED without generating adjustments")
	void approvingWithNoDivergencesStillMovesTheCountToApproved() {
		final UUID matchingProduct = UUID.randomUUID();
		final PhysicalCount pending = physicalCount(PhysicalCountStatus.PENDING_APPROVAL,
				List.of(new PhysicalCountLine(matchingProduct, new BigDecimal("20"), new BigDecimal("20"))));
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(pending));
		when(physicalCountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final PhysicalCount result = service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy));

		assertThat(result.getStatus()).isEqualTo(PhysicalCountStatus.APPROVED);
		verifyNoInteractions(adjustInventoryUseCase);
	}

	@Test
	@DisplayName("AC4: If an adjustment fails mid-approval the count is never saved as approved")
	void ac4IfAnAdjustmentFailsMidApprovalTheCountIsNeverSavedAsApproved() {
		final UUID firstProduct = UUID.randomUUID();
		final UUID secondProduct = UUID.randomUUID();
		final PhysicalCount pending = physicalCount(PhysicalCountStatus.PENDING_APPROVAL,
				List.of(new PhysicalCountLine(firstProduct, new BigDecimal("10"), new BigDecimal("12")),
						new PhysicalCountLine(secondProduct, new BigDecimal("5"), new BigDecimal("1"))));
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(pending));
		when(adjustInventoryUseCase.execute(any())).thenReturn(mockMovement())
				.thenThrow(new RuntimeException("accounting system unavailable"));

		assertThatThrownBy(() -> service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy)))
				.isInstanceOf(RuntimeException.class);

		verify(physicalCountRepositoryPort, never()).save(any());
	}

	private PhysicalCount physicalCount(final PhysicalCountStatus status, final List<PhysicalCountLine> lines) {
		return PhysicalCount.of(physicalCountId, PhysicalCountScope.TOTAL, null, warehouseId, status, startedBy,
				Instant.now(), lines);
	}

	private StockMovement mockMovement() {
		return StockMovement.builder()
				.id(StockMovementId.of(UUID.randomUUID()))
				.type(StockMovementType.ADJUSTMENT)
				.productId(UUID.randomUUID())
				.warehouseId(warehouseId)
				.quantity(BigDecimal.ONE)
				.unitCost(BigDecimal.ZERO)
				.lotCode(null)
				.serialNumbers(null)
				.originReference("PHYSICAL_COUNT")
				.justification("justification")
				.user(approvedBy)
				.timestamp(Instant.now())
				.build();
	}
}
