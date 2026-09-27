package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.core.ports.inbound.inventory.RegisterStockEntryUseCase;
import br.gravita.core.ports.outbound.sales.RegisterStockEntryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RegisterSalesReturnStockEntryAdapter implements RegisterStockEntryPort {

	static final UUID DEFAULT_WAREHOUSE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	private final RegisterStockEntryUseCase registerStockEntryUseCase;

	RegisterSalesReturnStockEntryAdapter(RegisterStockEntryUseCase registerStockEntryUseCase) {
		this.registerStockEntryUseCase = registerStockEntryUseCase;
	}

	@Override
	public void registerEntry(RegisterStockEntryCommand command) {
		registerStockEntryUseCase.execute(new br.gravita.core.ports.inbound.inventory.RegisterStockEntryCommand(
				command.productOrServiceId(), DEFAULT_WAREHOUSE_ID, command.quantity(), command.unitCost(), null,
				List.of(), "SALES_RETURN:" + command.sourceSalesReturnId(), SYSTEM_ACTOR_ID));
	}
}
