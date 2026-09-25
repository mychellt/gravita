package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockTransfer;
import br.gravita.core.domain.inventory.StockTransferId;
import br.gravita.core.ports.inbound.inventory.ConfirmTransferCommand;
import br.gravita.core.ports.inbound.inventory.InitiateTransferCommand;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockTransferRepositoryPort;
import br.gravita.core.usercases.inventory.TransferStockService;
import java.math.BigDecimal;
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
class TransferStockServiceTest {

	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Mock
	private StockMovementRepositoryPort stockMovementRepositoryPort;

	@Mock
	private StockTransferRepositoryPort stockTransferRepositoryPort;

	private TransferStockService service;

	private final UUID productId = UUID.randomUUID();
	private final UUID sourceWarehouseId = UUID.randomUUID();
	private final UUID destinationWarehouseId = UUID.randomUUID();
	private final UUID userId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new TransferStockService(stockBalanceRepositoryPort, stockMovementRepositoryPort,
				stockTransferRepositoryPort);
	}

	private StockBalance sourceBalance(BigDecimal onHand) {
		return StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, sourceWarehouseId, onHand,
				BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10.00"));
	}

	@Test
	void ac1_initiatingNeverExceedsTheSourceAvailableQuantity() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, sourceWarehouseId))
				.thenReturn(Optional.of(sourceBalance(new BigDecimal("10"))));

		InitiateTransferCommand command = new InitiateTransferCommand(productId, sourceWarehouseId,
				destinationWarehouseId, new BigDecimal("20"), null, List.of(), userId);

		assertThatThrownBy(() -> service.initiate(command)).isInstanceOf(BusinessRuleException.class);
		verify(stockTransferRepositoryPort, never()).save(any());
	}

	@Test
	void ac2_initiatingMovesOnHandIntoInTransitOnTheSource() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, sourceWarehouseId))
				.thenReturn(Optional.of(sourceBalance(new BigDecimal("100"))));
		when(stockTransferRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(stockMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		InitiateTransferCommand command = new InitiateTransferCommand(productId, sourceWarehouseId,
				destinationWarehouseId, new BigDecimal("30"), null, List.of(), userId);
		StockMovement movement = service.initiate(command);

		ArgumentCaptor<StockBalance> savedBalance = ArgumentCaptor.forClass(StockBalance.class);
		verify(stockBalanceRepositoryPort).save(savedBalance.capture());
		assertThat(savedBalance.getValue().getOnHand()).isEqualByComparingTo("70");
		assertThat(savedBalance.getValue().getInTransit()).isEqualByComparingTo("30");
		assertThat(movement.getType().name()).isEqualTo("TRANSFER");
	}

	@Test
	void ac3_confirmingATransferThatWasNeverInitiatedIsRejected() {
		UUID unknownId = UUID.randomUUID();
		when(stockTransferRepositoryPort.findById(StockTransferId.of(unknownId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.confirm(new ConfirmTransferCommand(unknownId, userId)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void ac3_confirmingTwiceIsRejected() {
		UUID transferId = UUID.randomUUID();
		StockTransfer alreadyConfirmed = StockTransfer.initiate(StockTransferId.of(transferId), productId,
				sourceWarehouseId, destinationWarehouseId, new BigDecimal("30")).confirm();
		when(stockTransferRepositoryPort.findById(StockTransferId.of(transferId)))
				.thenReturn(Optional.of(alreadyConfirmed));

		assertThatThrownBy(() -> service.confirm(new ConfirmTransferCommand(transferId, userId)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void ac2AndAc4_confirmingReleasesInTransitAndIncreasesDestinationOnHand() {
		UUID transferId = UUID.randomUUID();
		StockTransfer pending = StockTransfer.initiate(StockTransferId.of(transferId), productId, sourceWarehouseId,
				destinationWarehouseId, new BigDecimal("30"));
		when(stockTransferRepositoryPort.findById(StockTransferId.of(transferId))).thenReturn(Optional.of(pending));

		StockBalance source = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, sourceWarehouseId,
				new BigDecimal("70"), BigDecimal.ZERO, new BigDecimal("30"), new BigDecimal("10.00"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, sourceWarehouseId))
				.thenReturn(Optional.of(source));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, destinationWarehouseId))
				.thenReturn(Optional.empty());
		when(stockTransferRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(stockMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		StockMovement movement = service.confirm(new ConfirmTransferCommand(transferId, userId));

		ArgumentCaptor<StockBalance> savedBalance = ArgumentCaptor.forClass(StockBalance.class);
		verify(stockBalanceRepositoryPort, org.mockito.Mockito.times(2)).save(savedBalance.capture());
		List<StockBalance> saved = savedBalance.getAllValues();
		assertThat(saved.get(0).getInTransit()).isEqualByComparingTo("0");
		assertThat(saved.get(1).getOnHand()).isEqualByComparingTo("30");
		assertThat(movement.getWarehouseId()).isEqualTo(destinationWarehouseId);
	}
}
