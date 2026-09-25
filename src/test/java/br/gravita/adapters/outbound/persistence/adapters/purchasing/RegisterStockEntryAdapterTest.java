package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import br.gravita.core.ports.inbound.inventory.RegisterStockEntryUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.RegisterStockEntryPort.RegisterStockEntryCommand;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GRA-82: proves the adapter bridges M6's {@code RegisterStockEntryPort}
 * command into M5's real {@code RegisterStockEntryUseCase} - replacing the
 * no-op stub from GRA-63 - rather than mocking the port away as the existing
 * {@code ConfirmPurchaseReceiptService} tests do.
 */
@ExtendWith(MockitoExtension.class)
class RegisterStockEntryAdapterTest {

	@Mock
	private RegisterStockEntryUseCase registerStockEntryUseCase;

	@Test
	void delegatesToTheRealUseCaseMappingProductQuantityAndCost() {
		RegisterStockEntryAdapter adapter = new RegisterStockEntryAdapter(registerStockEntryUseCase);
		UUID productId = UUID.randomUUID();
		UUID receiptId = UUID.randomUUID();

		adapter.registerEntry(new RegisterStockEntryCommand(productId, BigDecimal.TEN, new BigDecimal("5.00"), receiptId));

		ArgumentCaptor<br.gravita.core.ports.inbound.inventory.RegisterStockEntryCommand> captor = ArgumentCaptor
				.forClass(br.gravita.core.ports.inbound.inventory.RegisterStockEntryCommand.class);
		verify(registerStockEntryUseCase).execute(captor.capture());

		var command = captor.getValue();
		assertThat(command.productId()).isEqualTo(productId);
		assertThat(command.quantity()).isEqualByComparingTo(BigDecimal.TEN);
		assertThat(command.unitCost()).isEqualByComparingTo(new BigDecimal("5.00"));
		assertThat(command.originReference()).isEqualTo("PURCHASE_RECEIPT:" + receiptId);
		assertThat(command.warehouseId()).isEqualTo(RegisterStockEntryAdapter.DEFAULT_WAREHOUSE_ID);
		assertThat(command.user()).isEqualTo(RegisterStockEntryAdapter.SYSTEM_ACTOR_ID);
		assertThat(command.lot()).isNull();
		assertThat(command.serials()).isEmpty();
	}
}
