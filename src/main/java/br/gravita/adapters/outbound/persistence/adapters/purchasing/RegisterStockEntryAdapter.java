package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.inbound.inventory.RegisterStockEntryUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.RegisterStockEntryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RegisterStockEntryAdapter implements RegisterStockEntryPort {

	static final UUID DEFAULT_WAREHOUSE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	private final RegisterStockEntryUseCase registerStockEntryUseCase;

	RegisterStockEntryAdapter(RegisterStockEntryUseCase registerStockEntryUseCase) {
		this.registerStockEntryUseCase = registerStockEntryUseCase;
	}

	@Override
	public void registerEntry(RegisterStockEntryCommand command) {
		registerStockEntryUseCase.execute(new br.gravita.core.ports.inbound.inventory.RegisterStockEntryCommand(
				command.productId(), DEFAULT_WAREHOUSE_ID, command.quantity(), command.unitCost(), null, List.of(),
				"PURCHASE_RECEIPT:" + command.sourcePurchaseReceiptId(), SYSTEM_ACTOR_ID));
	}
}
