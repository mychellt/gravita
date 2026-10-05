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
import org.junit.jupiter.api.DisplayName;
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
	@DisplayName("AC1: Rejects an adjustment whose justification is blank")
	void ac1RejectsABlankJustification() {
		final AdjustInventoryCommand command = new AdjustInventoryCommand(productId, warehouseId, new BigDecimal("5"), " ",
				userId);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);
		verify(stockBalanceRepositoryPort, never()).save(any());
		verify(postAdjustmentAccountingEntryPort, never()).postAdjustmentEntry(any());
	}

	@Test
	@DisplayName("AC2: On-hand changes by exactly the positive or negative adjustment delta")
	void ac2OnHandReflectsThePositiveOrNegativeDeltaExactly() {
		final StockBalance current = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId,
				new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10.00"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(current));

		service.execute(new AdjustInventoryCommand(productId, warehouseId, new BigDecimal("-15"), "Breakage",
				userId));

		final ArgumentCaptor<StockBalance> savedBalance = ArgumentCaptor.forClass(StockBalance.class);
		verify(stockBalanceRepositoryPort).save(savedBalance.capture());
		assertThat(savedBalance.getValue().getOnHand()).isEqualByComparingTo("85");
	}

	@Test
	@DisplayName("AC3: Posts exactly one accounting entry for each adjustment")
	void ac3PostsExactlyOneAccountingEntryPerAdjustment() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());

		service.execute(new AdjustInventoryCommand(productId, warehouseId, new BigDecimal("10"), "Correction",
				userId));

		final ArgumentCaptor<PostAdjustmentAccountingEntryCommand> captor = ArgumentCaptor
				.forClass(PostAdjustmentAccountingEntryCommand.class);
		verify(postAdjustmentAccountingEntryPort, times(1)).postAdjustmentEntry(captor.capture());
		assertThat(captor.getValue().justification()).isEqualTo("Correction");
		assertThat(captor.getValue().quantityDelta()).isEqualByComparingTo("10");
	}

	@Test
	@DisplayName("AC4: The resulting stock movement is of type ADJUSTMENT and carries the justification")
	void ac4TheResultingMovementIsAnAdjustmentCarryingTheJustification() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());
		when(stockMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final StockMovement movement = service.execute(
				new AdjustInventoryCommand(productId, warehouseId, new BigDecimal("10"), "Correction", userId));

		assertThat(movement.getType().name()).isEqualTo("ADJUSTMENT");
		assertThat(movement.getJustification()).isEqualTo("Correction");
		assertThat(movement.getQuantity()).isEqualByComparingTo("10");
	}
}
