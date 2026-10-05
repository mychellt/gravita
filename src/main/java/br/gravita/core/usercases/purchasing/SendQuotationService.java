package br.gravita.core.usercases.purchasing;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestNotFoundException;
import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.domain.purchasing.QuotationItem;
import br.gravita.core.ports.inbound.purchasing.SendQuotationCommand;
import br.gravita.core.ports.inbound.purchasing.SendQuotationUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.QuotationRepositoryPort;
import java.util.List;
import java.util.UUID;

@UseCase
public class SendQuotationService implements SendQuotationUseCase {

	private final PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;
	private final QuotationRepositoryPort quotationRepositoryPort;

	public SendQuotationService(final PurchaseRequestRepositoryPort purchaseRequestRepositoryPort,
			final QuotationRepositoryPort quotationRepositoryPort) {
		this.purchaseRequestRepositoryPort = purchaseRequestRepositoryPort;
		this.quotationRepositoryPort = quotationRepositoryPort;
	}

	@Override
	public QuotationId execute(final SendQuotationCommand command) {
		final PurchaseRequest purchaseRequest = purchaseRequestRepositoryPort.findById(command.requestId())
				.orElseThrow(() -> new PurchaseRequestNotFoundException(command.requestId().value()));

		final PurchaseRequest quoted = purchaseRequest.quote();

		final List<QuotationItem> items = purchaseRequest.getItems().stream()
				.map(item -> new QuotationItem(item.productId(), item.quantity()))
				.toList();
		final Quotation quotation = Quotation.send(QuotationId.of(UUID.randomUUID()), command.requestId(), items,
				command.suppliers());
		final Quotation saved = quotationRepositoryPort.save(quotation);

		purchaseRequestRepositoryPort.save(quoted);

		return saved.getId();
	}
}
