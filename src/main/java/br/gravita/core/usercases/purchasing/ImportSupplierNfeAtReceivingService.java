package br.gravita.core.usercases.purchasing;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.purchasing.ConferenceResult;
import br.gravita.core.domain.purchasing.ConferenceResult.ConferenceLine;
import br.gravita.core.domain.purchasing.InstallmentTerm;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.ports.inbound.purchasing.ImportSupplierNfeAtReceivingCommand;
import br.gravita.core.ports.inbound.purchasing.ImportSupplierNfeAtReceivingUseCase;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlCommand;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@UseCase
public class ImportSupplierNfeAtReceivingService implements ImportSupplierNfeAtReceivingUseCase {

	private final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
	private final PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;
	private final ImportSupplierNfeXmlUseCase importSupplierNfeXmlUseCase;

	public ImportSupplierNfeAtReceivingService(final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort,
			final PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort,
			final ImportSupplierNfeXmlUseCase importSupplierNfeXmlUseCase) {
		this.purchaseOrderRepositoryPort = purchaseOrderRepositoryPort;
		this.purchaseReceiptRepositoryPort = purchaseReceiptRepositoryPort;
		this.importSupplierNfeXmlUseCase = importSupplierNfeXmlUseCase;
	}

	@Override
	public ConferenceResult execute(final ImportSupplierNfeAtReceivingCommand command) {
		final PurchaseOrder order = purchaseOrderRepositoryPort.findById(command.orderId())
				.orElseThrow(() -> new PurchaseOrderNotFoundException(command.orderId().value()));
		final PurchaseReceipt receipt = purchaseReceiptRepositoryPort.findById(command.receiptId())
				.orElseThrow(() -> new PurchaseReceiptNotFoundException(command.receiptId().value()));
		if (!receipt.getOrderId().equals(order.getId())) {
			throw new BusinessRuleException(
					"Purchase receipt " + receipt.getId().value() + " does not belong to order " + order.getId().value());
		}

		final InboundNfe inboundNfe = importSupplierNfeXmlUseCase
				.execute(new ImportSupplierNfeXmlCommand(command.companyId(), command.xmlFile()));

		final ConferenceResult conferenceResult = reconcile(order, receipt, inboundNfe);

		final PurchaseReceipt completed = receipt.completeConference(toInstallmentTerms(inboundNfe));
		purchaseReceiptRepositoryPort.save(completed);

		return conferenceResult;
	}

	private ConferenceResult reconcile(final PurchaseOrder order, final PurchaseReceipt receipt, final InboundNfe inboundNfe) {
		final List<ConferenceLine> lines = receipt.getReceivedItems().stream()
				.map(item -> new ConferenceLine(item.productId(), item.orderedQty(), item.receivedQty()))
				.toList();
		return new ConferenceResult(lines, order.totalValue(), inboundNfe.getTotals().totalValue());
	}

	private List<InstallmentTerm> toInstallmentTerms(final InboundNfe inboundNfe) {
		final LocalDate dueDate = inboundNfe.getIssuedAt().atZone(ZoneOffset.UTC).toLocalDate();
		return List.of(new InstallmentTerm(inboundNfe.getTotals().totalValue(), dueDate));
	}
}
