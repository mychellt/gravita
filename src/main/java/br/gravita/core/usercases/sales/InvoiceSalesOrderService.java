package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.SalesInvoice;
import br.gravita.core.domain.sales.SalesInvoiceId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderNotFoundException;
import br.gravita.core.ports.inbound.sales.InvoiceSalesOrderCommand;
import br.gravita.core.ports.inbound.sales.InvoiceSalesOrderUseCase;
import br.gravita.core.ports.inbound.sales.SalesInvoiceView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesInvoiceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.sales.GenerateAccountsReceivablePort;
import br.gravita.core.ports.outbound.sales.GenerateAccountsReceivablePort.GenerateAccountsReceivableCommand;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort.IssueFiscalDocumentCommand;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort.IssueFiscalDocumentCommand.Item;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-M7-06. Products dispatch to a single NFe covering all product lines;
 * services would dispatch to NFSe, but M4 isn't built yet, so any service
 * line item currently fails issuance rather than being silently skipped -
 * see {@link IssueFiscalDocumentPort#issueForServices}.
 */
@RequiredArgsConstructor
@UseCase
public class InvoiceSalesOrderService implements InvoiceSalesOrderUseCase {

	private final SalesOrderRepositoryPort salesOrderRepositoryPort;
	private final SalesInvoiceRepositoryPort salesInvoiceRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;
	private final IssueFiscalDocumentPort issueFiscalDocumentPort;
	private final GenerateAccountsReceivablePort generateAccountsReceivablePort;

	@Override
	@Transactional
	public SalesInvoiceView execute(final InvoiceSalesOrderCommand command) {
		final SalesOrder order = salesOrderRepositoryPort.findById(SalesOrderId.of(command.orderId()))
				.orElseThrow(() -> new SalesOrderNotFoundException(command.orderId()));

		// AC1: validated up front, before any fiscal issuance side effect; the
		// resulting instance is only persisted once issuance below succeeds.
		final SalesOrder invoiced = order.invoice();

		final List<Item> productItems = new ArrayList<>();
		final List<Item> serviceItems = new ArrayList<>();
		BigDecimal productTotal = BigDecimal.ZERO;
		BigDecimal serviceTotal = BigDecimal.ZERO;

		for (final SalesOrderItem item : order.getItems()) {
			final ProductDomain product = productRepositoryPort.get(item.productOrServiceId())
					.orElseThrow(
							() -> new ResourceNotFoundException("Product or service not found: "
									+ item.productOrServiceId()));
			final Item fiscalItem = new Item(item.productOrServiceId(), product.getInternalCode(), item.quantity(),
					item.unitPrice(), item.discount());
			if (product.getType() == ProductType.SERVICE) {
				serviceItems.add(fiscalItem);
				serviceTotal = serviceTotal.add(item.lineTotal());
			} else {
				productItems.add(fiscalItem);
				productTotal = productTotal.add(item.lineTotal());
			}
		}

		final List<FiscalDocumentRef> issuedDocuments = new ArrayList<>();
		if (!productItems.isEmpty()) {
			final FiscalDocumentRef productDocument = issueFiscalDocumentPort
					.issueForProducts(new IssueFiscalDocumentCommand(order.getId().value(), order.getCustomerId(),
							productItems));
			issuedDocuments.add(productDocument);
			generateAccountsReceivablePort.generate(new GenerateAccountsReceivableCommand(order.getCustomerId(),
					productDocument, productTotal, LocalDate.now()));
		}
		if (!serviceItems.isEmpty()) {
			final FiscalDocumentRef serviceDocument = issueFiscalDocumentPort
					.issueForServices(new IssueFiscalDocumentCommand(order.getId().value(), order.getCustomerId(),
							serviceItems));
			issuedDocuments.add(serviceDocument);
			generateAccountsReceivablePort.generate(new GenerateAccountsReceivableCommand(order.getCustomerId(),
					serviceDocument, serviceTotal, LocalDate.now()));
		}

		salesOrderRepositoryPort.save(invoiced);
		final SalesInvoice invoice = SalesInvoice.issue(SalesInvoiceId.of(UUID.randomUUID()), order.getId(),
				issuedDocuments);
		final SalesInvoice saved = salesInvoiceRepositoryPort.save(invoice);

		return SalesInvoiceView.from(saved);
	}
}
