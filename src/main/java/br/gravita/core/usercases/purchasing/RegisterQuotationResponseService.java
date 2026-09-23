package br.gravita.core.usercases.purchasing;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.purchasing.QuotationNotFoundException;
import br.gravita.core.ports.inbound.purchasing.RegisterQuotationResponseCommand;
import br.gravita.core.ports.inbound.purchasing.RegisterQuotationResponseUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.QuotationRepositoryPort;

@UseCase
public class RegisterQuotationResponseService implements RegisterQuotationResponseUseCase {

	private final QuotationRepositoryPort quotationRepositoryPort;

	public RegisterQuotationResponseService(QuotationRepositoryPort quotationRepositoryPort) {
		this.quotationRepositoryPort = quotationRepositoryPort;
	}

	@Override
	public void execute(RegisterQuotationResponseCommand command) {
		Quotation quotation = quotationRepositoryPort.findById(command.quotationId())
				.orElseThrow(() -> new QuotationNotFoundException(command.quotationId().value()));

		Quotation updated = quotation.registerResponse(command.supplierId(), command.itemPrices(), command.deadline());

		quotationRepositoryPort.save(updated);
	}
}
