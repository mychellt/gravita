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
	void ac1_rejectsApprovingACountThatIsStillInProgress() {
		PhysicalCount inProgress = physicalCount(PhysicalCountStatus.IN_PROGRESS, List.of());
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(inProgress));

		assertThatThrownBy(() -> service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy)))
				.isInstanceOf(BusinessRuleException.class);

		verifyNoInteractions(adjustInventoryUseCase);
		verify(physicalCountRepositoryPort, never()).save(any());
	}

	@Test
	void ac1_rejectsApprovingACountThatIsAlreadyApproved() {
		PhysicalCount approved = physicalCount(PhysicalCountStatus.APPROVED, List.of());
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(approved));

		assertThatThrownBy(() -> service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy)))
				.isInstanceOf(BusinessRuleException.class);

		verifyNoInteractions(adjustInventoryUseCase);
		verify(physicalCountRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsApprovingAnUnknownCount() {
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void ac2_generatesExactlyOneAdjustmentPerDivergentProductAndNoneForMatchingOnes() {
		UUID divergentProduct = UUID.randomUUID();
		UUID matchingProduct = UUID.randomUUID();
		PhysicalCount pending = physicalCount(PhysicalCountStatus.PENDING_APPROVAL,
				List.of(new PhysicalCountLine(divergentProduct, new BigDecimal("10"), new BigDecimal("7")),
						new PhysicalCountLine(matchingProduct, new BigDecimal("20"), new BigDecimal("20"))));
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(pending));
		when(physicalCountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(adjustInventoryUseCase.execute(any())).thenReturn(mockMovement());

		service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy));

		ArgumentCaptor<AdjustInventoryCommand> captor = ArgumentCaptor.forClass(AdjustInventoryCommand.class);
		verify(adjustInventoryUseCase, times(1)).execute(captor.capture());
		assertThat(captor.getValue().productId()).isEqualTo(divergentProduct);
		assertThat(captor.getValue().quantityDelta()).isEqualByComparingTo("-3");
		assertThat(captor.getValue().warehouseId()).isEqualTo(warehouseId);
		assertThat(captor.getValue().user()).isEqualTo(approvedBy);
	}

	@Test
	void ac3_theAdjustmentJustificationReferencesThePhysicalCount() {
		UUID divergentProduct = UUID.randomUUID();
		PhysicalCount pending = physicalCount(PhysicalCountStatus.PENDING_APPROVAL,
				List.of(new PhysicalCountLine(divergentProduct, new BigDecimal("10"), new BigDecimal("15"))));
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(pending));
		when(physicalCountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(adjustInventoryUseCase.execute(any())).thenReturn(mockMovement());

		service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy));

		ArgumentCaptor<AdjustInventoryCommand> captor = ArgumentCaptor.forClass(AdjustInventoryCommand.class);
		verify(adjustInventoryUseCase).execute(captor.capture());
		assertThat(captor.getValue().justification()).contains(physicalCountId.value().toString());
	}

	@Test
	void approvingWithNoDivergencesStillMovesTheCountToApproved() {
		UUID matchingProduct = UUID.randomUUID();
		PhysicalCount pending = physicalCount(PhysicalCountStatus.PENDING_APPROVAL,
				List.of(new PhysicalCountLine(matchingProduct, new BigDecimal("20"), new BigDecimal("20"))));
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(pending));
		when(physicalCountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		PhysicalCount result = service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy));

		assertThat(result.getStatus()).isEqualTo(PhysicalCountStatus.APPROVED);
		verifyNoInteractions(adjustInventoryUseCase);
	}

	@Test
	void ac4_ifAnAdjustmentFailsMidApprovalTheCountIsNeverSavedAsApproved() {
		UUID firstProduct = UUID.randomUUID();
		UUID secondProduct = UUID.randomUUID();
		PhysicalCount pending = physicalCount(PhysicalCountStatus.PENDING_APPROVAL,
				List.of(new PhysicalCountLine(firstProduct, new BigDecimal("10"), new BigDecimal("12")),
						new PhysicalCountLine(secondProduct, new BigDecimal("5"), new BigDecimal("1"))));
		when(physicalCountRepositoryPort.findById(physicalCountId)).thenReturn(Optional.of(pending));
		when(adjustInventoryUseCase.execute(any())).thenReturn(mockMovement())
				.thenThrow(new RuntimeException("accounting system unavailable"));

		assertThatThrownBy(() -> service.execute(new ApprovePhysicalCountCommand(physicalCountId, approvedBy)))
				.isInstanceOf(RuntimeException.class);

		verify(physicalCountRepositoryPort, never()).save(any());
	}

	private PhysicalCount physicalCount(PhysicalCountStatus status, List<PhysicalCountLine> lines) {
		return PhysicalCount.of(physicalCountId, PhysicalCountScope.TOTAL, null, warehouseId, status, startedBy,
				Instant.now(), lines);
	}

	private StockMovement mockMovement() {
		return StockMovement.of(StockMovementId.of(UUID.randomUUID()), StockMovementType.ADJUSTMENT,
				UUID.randomUUID(), warehouseId, BigDecimal.ONE, BigDecimal.ZERO, null, null, "PHYSICAL_COUNT",
				"justification", approvedBy, Instant.now());
	}
}
