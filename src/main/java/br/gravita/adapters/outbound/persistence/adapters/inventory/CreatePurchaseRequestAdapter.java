package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestCommand;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.CreatePurchaseRequestPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import java.util.List;
import org.springframework.stereotype.Component;

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
