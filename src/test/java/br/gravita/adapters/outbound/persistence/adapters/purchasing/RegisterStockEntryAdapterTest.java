package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import br.gravita.core.ports.inbound.inventory.RegisterStockEntryUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.RegisterStockEntryPort.RegisterStockEntryCommand;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterStockEntryAdapterTest {

	@Mock
	private RegisterStockEntryUseCase registerStockEntryUseCase;

	@Test
	@DisplayName("Delegates to the real use case, mapping product, quantity and cost")
	void delegatesToTheRealUseCaseMappingProductQuantityAndCost() {
		final RegisterStockEntryAdapter adapter = new RegisterStockEntryAdapter(registerStockEntryUseCase);
		final UUID productId = UUID.randomUUID();
		final UUID receiptId = UUID.randomUUID();

		adapter.registerEntry(new RegisterStockEntryCommand(productId, BigDecimal.TEN, new BigDecimal("5.00"), receiptId));

		final ArgumentCaptor<br.gravita.core.ports.inbound.inventory.RegisterStockEntryCommand> captor = ArgumentCaptor
				.forClass(br.gravita.core.ports.inbound.inventory.RegisterStockEntryCommand.class);
		verify(registerStockEntryUseCase).execute(captor.capture());

		final var command = captor.getValue();
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
