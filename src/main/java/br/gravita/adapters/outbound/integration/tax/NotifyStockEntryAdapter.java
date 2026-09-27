package br.gravita.adapters.outbound.integration.tax;

import br.gravita.core.ports.inbound.inventory.RegisterStockEntryCommand;
import br.gravita.core.ports.inbound.inventory.RegisterStockEntryUseCase;
import br.gravita.core.ports.outbound.tax.NotifyStockEntryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Bridges tax (M2) into inventory's (M5) real {@link RegisterStockEntryUseCase},
 * the same way purchasing's own {@code RegisterStockEntryAdapter} does for
 * M6, but kept as a separate adapter/port pair (see {@link NotifyStockEntryPort}).
 *
 * <p>Like purchasing's bridge, {@code InboundNfe} doesn't carry a destination
 * warehouse or an authenticated actor - neither concept exists in the NFe
 * inbound flow yet - so this adapter targets a single placeholder warehouse
 * and attributes the movement to a system actor until multi-warehouse
 * receiving and per-request actor propagation are modeled.
 */
@Component
class NotifyStockEntryAdapter implements NotifyStockEntryPort {

	static final UUID DEFAULT_WAREHOUSE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	private final RegisterStockEntryUseCase registerStockEntryUseCase;

	NotifyStockEntryAdapter(RegisterStockEntryUseCase registerStockEntryUseCase) {
		this.registerStockEntryUseCase = registerStockEntryUseCase;
	}

	@Override
	public void notifyEntry(NotifyStockEntryCommand command) {
		registerStockEntryUseCase.execute(new RegisterStockEntryCommand(command.productId(), DEFAULT_WAREHOUSE_ID,
				command.quantity(), command.unitCost(), null, List.of(),
				"INBOUND_NFE:" + command.sourceInboundNfeId(), SYSTEM_ACTOR_ID));
	}
}
