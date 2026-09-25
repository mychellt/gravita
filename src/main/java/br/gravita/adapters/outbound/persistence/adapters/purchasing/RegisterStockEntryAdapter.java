package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.inbound.inventory.RegisterStockEntryUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.RegisterStockEntryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Bridges purchasing (M6) into inventory's (M5) real {@link RegisterStockEntryUseCase}
 * (GRA-82; replaces the no-op stub from GRA-63).
 *
 * <p>M6's {@code PurchaseOrder}/{@code PurchaseReceipt} don't carry a
 * destination warehouse or an authenticated actor yet - neither concept
 * exists anywhere in purchasing today - so this adapter targets a single
 * placeholder warehouse and attributes the movement to a system actor until
 * multi-warehouse receiving and per-request actor propagation are modeled.
 * Tracked as a follow-up; not a regression introduced by this wiring, since
 * the stub it replaces never touched inventory at all.
 */
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
