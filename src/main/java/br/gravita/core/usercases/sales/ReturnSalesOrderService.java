package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.SalesInvoice;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderNotFoundException;
import br.gravita.core.domain.sales.SalesReturn;
import br.gravita.core.domain.sales.SalesReturnId;
import br.gravita.core.domain.sales.SalesReturnItem;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.ReturnSalesOrderCommand;
import br.gravita.core.ports.inbound.sales.ReturnSalesOrderUseCase;
import br.gravita.core.ports.inbound.sales.SalesReturnView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesInvoiceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesReturnRepositoryPort;
import br.gravita.core.ports.outbound.sales.AdjustReceivableForReturnPort;
import br.gravita.core.ports.outbound.sales.AdjustReceivableForReturnPort.AdjustReceivableForReturnCommand;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort.IssueFiscalDocumentCommand.Item;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort.IssueReturnFiscalDocumentCommand;
import br.gravita.core.ports.outbound.sales.RegisterStockEntryPort;
import br.gravita.core.ports.outbound.sales.RegisterStockEntryPort.RegisterStockEntryCommand;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-M7-07. Items are matched back to the order by {@code productOrServiceId}
 * (a {@link SalesOrderItem} has no separate line id) and the return NFe
 * always references the single NFe {@link InvoiceSalesOrderService} issues
 * for product lines - service returns aren't reachable since NFSe issuance
 * (M4) isn't implemented, so an order with service items never reaches
 * {@code INVOICED} in the first place.
 */
@RequiredArgsConstructor
@UseCase
public class ReturnSalesOrderService implements ReturnSalesOrderUseCase {

	private final SalesOrderRepositoryPort salesOrderRepositoryPort;
	private final SalesInvoiceRepositoryPort salesInvoiceRepositoryPort;
	private final SalesReturnRepositoryPort salesReturnRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;
	private final IssueFiscalDocumentPort issueFiscalDocumentPort;
	private final RegisterStockEntryPort registerStockEntryPort;
	private final AdjustReceivableForReturnPort adjustReceivableForReturnPort;

	@Override
	@Transactional
	public SalesReturnView execute(ReturnSalesOrderCommand command) {
		SalesOrder order = salesOrderRepositoryPort.findById(SalesOrderId.of(command.orderId()))
				.orElseThrow(() -> new SalesOrderNotFoundException(command.orderId()));

		List<SalesReturnItem> items = command.items().stream()
				.map(item -> new SalesReturnItem(item.productOrServiceId(), item.quantity()))
				.toList();
		List<SalesReturn> previousReturns = salesReturnRepositoryPort.findByOrderId(order.getId());

		// AC1: validated up front, before any fiscal/stock/finance side effect below.
		SalesReturn salesReturn = SalesReturn.forOrder(SalesReturnId.of(UUID.randomUUID()), order, items,
				previousReturns);

		SalesInvoice invoice = salesInvoiceRepositoryPort.findByOrderId(order.getId())
				.orElseThrow(() -> new BusinessRuleException(
						"Order " + order.getId().value() + " is INVOICED but has no linked SalesInvoice"));
		FiscalDocumentRef originalDocument = invoice.getFiscalDocuments().get(0);

		List<Item> fiscalItems = new ArrayList<>();
		BigDecimal returnedAmount = BigDecimal.ZERO;
		for (SalesReturnItem item : salesReturn.getItems()) {
			SalesOrderItem orderItem = findOrderItem(order, item.productOrServiceId());
			ProductDomain product = productRepositoryPort.get(item.productOrServiceId())
					.orElseThrow(() -> new ResourceNotFoundException(
							"Product or service not found: " + item.productOrServiceId()));

			BigDecimal netUnitPrice = orderItem.lineTotal().divide(orderItem.quantity(), 4, RoundingMode.HALF_UP);
			fiscalItems.add(new Item(item.productOrServiceId(), product.getInternalCode(), item.quantity(),
					netUnitPrice, BigDecimal.ZERO));
			returnedAmount = returnedAmount.add(netUnitPrice.multiply(item.quantity()));

			BigDecimal unitCost = product.getAverageCost() == null ? BigDecimal.ZERO : product.getAverageCost();
			registerStockEntryPort.registerEntry(new RegisterStockEntryCommand(item.productOrServiceId(),
					item.quantity(), unitCost, salesReturn.getId().value()));
		}

		FiscalDocumentRef returnNfeRef = issueFiscalDocumentPort.issueForReturn(new IssueReturnFiscalDocumentCommand(
				order.getId().value(), order.getCustomerId(), originalDocument, fiscalItems));

		adjustReceivableForReturnPort.adjust(new AdjustReceivableForReturnCommand(salesReturn.getId().value(),
				order.getCustomerId(), returnedAmount));

		SalesReturn saved = salesReturnRepositoryPort.save(salesReturn.withNfeRef(returnNfeRef));
		return SalesReturnView.from(saved);
	}

	private SalesOrderItem findOrderItem(SalesOrder order, UUID productOrServiceId) {
		return order.getItems().stream().filter(orderItem -> orderItem.productOrServiceId().equals(productOrServiceId))
				.findFirst()
				.orElseThrow(() -> new BusinessRuleException(
						"Product or service " + productOrServiceId + " was not part of the original order"));
	}
}
