package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestCommand;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.CreatePurchaseRequestPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bridges inventory (M5) into purchasing's (M6) real
 * {@link CreatePurchaseRequestUseCase} (UC-M5-11, GRA-88): every low-stock
 * suggestion becomes a {@code MIN_STOCK_TRIGGER} purchase request, unless one
 * is already OPEN for the same product (AC2) - checked here rather than in
 * {@code CreatePurchaseRequestService} so the general-purpose use case keeps
 * always creating what it's asked to create, and this auto-trigger path owns
 * its own dedupe rule.
 */
@Component
class CreatePurchaseRequestAdapter implements CreatePurchaseRequestPort {

	private final CreatePurchaseRequestUseCase createPurchaseRequestUseCase;
	private final PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;

	CreatePurchaseRequestAdapter(CreatePurchaseRequestUseCase createPurchaseRequestUseCase,
			PurchaseRequestRepositoryPort purchaseRequestRepositoryPort) {
		this.createPurchaseRequestUseCase = createPurchaseRequestUseCase;
		this.purchaseRequestRepositoryPort = purchaseRequestRepositoryPort;
	}

	@Override
	public void createIfNotAlreadyOpen(ReorderCommand command) {
		if (purchaseRequestRepositoryPort.existsOpenByOriginAndProductId(PurchaseRequestOrigin.MIN_STOCK_TRIGGER,
				command.productId())) {
			return;
		}
		createPurchaseRequestUseCase.execute(new CreatePurchaseRequestCommand(PurchaseRequestOrigin.MIN_STOCK_TRIGGER,
				List.of(new PurchaseRequestItem(command.productId(), command.quantity())), null));
	}
}
