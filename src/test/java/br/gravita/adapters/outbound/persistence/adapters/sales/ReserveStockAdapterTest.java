package br.gravita.adapters.outbound.persistence.adapters.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import br.gravita.core.ports.inbound.inventory.ReserveStockCommand;
import br.gravita.core.ports.inbound.inventory.ReserveStockUseCase;
import br.gravita.core.ports.outbound.sales.ReserveStockPort.ReserveStockForOrderCommand;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReserveStockAdapterTest {

	@Mock
	private ReserveStockUseCase reserveStockUseCase;

	@Test
	void delegatesToTheRealUseCaseUsingTheDefaultWarehouse() {
		ReserveStockAdapter adapter = new ReserveStockAdapter(reserveStockUseCase);
		UUID orderId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();

		adapter.reserve(new ReserveStockForOrderCommand(orderId, productId, BigDecimal.TEN));

		ArgumentCaptor<ReserveStockCommand> captor = ArgumentCaptor.forClass(ReserveStockCommand.class);
		verify(reserveStockUseCase).execute(captor.capture());

		ReserveStockCommand command = captor.getValue();
		assertThat(command.orderRef()).isEqualTo(orderId);
		assertThat(command.productId()).isEqualTo(productId);
		assertThat(command.quantity()).isEqualByComparingTo(BigDecimal.TEN);
		assertThat(command.warehouseId()).isEqualTo(ReserveStockAdapter.DEFAULT_WAREHOUSE_ID);
	}
}
