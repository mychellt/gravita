package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.ports.inbound.inventory.AdjustInventoryCommand;
import br.gravita.core.ports.outbound.persistence.inventory.PostAdjustmentAccountingEntryPort;
import br.gravita.core.ports.outbound.persistence.inventory.PostAdjustmentAccountingEntryPort.PostAdjustmentAccountingEntryCommand;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import br.gravita.core.usercases.inventory.AdjustInventoryService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdjustInventoryServiceTest {

	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Mock
	private StockMovementRepositoryPort stockMovementRepositoryPort;

	@Mock
	private PostAdjustmentAccountingEntryPort postAdjustmentAccountingEntryPort;

	private AdjustInventoryService service;

	private final UUID productId = UUID.randomUUID();
	private final UUID warehouseId = UUID.randomUUID();
	private final UUID userId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new AdjustInventoryService(stockBalanceRepositoryPort, stockMovementRepositoryPort,
				postAdjustmentAccountingEntryPort);
	}

	@Test
	void ac1_rejectsABlankJustification() {
		AdjustInventoryCommand command = new AdjustInventoryCommand(productId, warehouseId, new BigDecimal("5"), " ",
				userId);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);
		verify(stockBalanceRepositoryPort, never()).save(any());
		verify(postAdjustmentAccountingEntryPort, never()).postAdjustmentEntry(any());
	}

	@Test
	void ac2_onHandReflectsThePositiveOrNegativeDeltaExactly() {
		StockBalance current = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId,
				new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10.00"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(current));

		service.execute(new AdjustInventoryCommand(productId, warehouseId, new BigDecimal("-15"), "Breakage",
				userId));

		ArgumentCaptor<StockBalance> savedBalance = ArgumentCaptor.forClass(StockBalance.class);
		verify(stockBalanceRepositoryPort).save(savedBalance.capture());
		assertThat(savedBalance.getValue().getOnHand()).isEqualByComparingTo("85");
	}

	@Test
	void ac3_postsExactlyOneAccountingEntryPerAdjustment() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());

		service.execute(new AdjustInventoryCommand(productId, warehouseId, new BigDecimal("10"), "Correction",
				userId));

		ArgumentCaptor<PostAdjustmentAccountingEntryCommand> captor = ArgumentCaptor
				.forClass(PostAdjustmentAccountingEntryCommand.class);
		verify(postAdjustmentAccountingEntryPort, times(1)).postAdjustmentEntry(captor.capture());
		assertThat(captor.getValue().justification()).isEqualTo("Correction");
		assertThat(captor.getValue().quantityDelta()).isEqualByComparingTo("10");
	}

	@Test
	void ac4_theResultingMovementIsAnAdjustmentCarryingTheJustification() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());
		when(stockMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		StockMovement movement = service.execute(
				new AdjustInventoryCommand(productId, warehouseId, new BigDecimal("10"), "Correction", userId));

		assertThat(movement.getType().name()).isEqualTo("ADJUSTMENT");
		assertThat(movement.getJustification()).isEqualTo("Correction");
		assertThat(movement.getQuantity()).isEqualByComparingTo("10");
	}
}
