package br.gravita.core.usercases.purchasing;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestCommand;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import java.util.UUID;

@UseCase
public class CreatePurchaseRequestService implements CreatePurchaseRequestUseCase {

	private final PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;

	public CreatePurchaseRequestService(final PurchaseRequestRepositoryPort purchaseRequestRepositoryPort) {
		this.purchaseRequestRepositoryPort = purchaseRequestRepositoryPort;
	}

	@Override
	public PurchaseRequestId execute(final CreatePurchaseRequestCommand command) {
		final PurchaseRequestId id = PurchaseRequestId.of(UUID.randomUUID());
		final PurchaseRequest purchaseRequest = PurchaseRequest.open(id, command.origin(), command.items(), command.requestedBy());
		final PurchaseRequest saved = purchaseRequestRepositoryPort.save(purchaseRequest);
		return saved.getId();
	}
}
