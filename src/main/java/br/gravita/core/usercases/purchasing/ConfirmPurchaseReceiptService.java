package br.gravita.core.usercases.purchasing;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseReceiptNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceiptStatus;
import br.gravita.core.ports.inbound.purchasing.ConfirmPurchaseReceiptCommand;
import br.gravita.core.ports.inbound.purchasing.ConfirmPurchaseReceiptUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.GeneratePayableFromReceiptPort;
import br.gravita.core.ports.outbound.persistence.purchasing.GeneratePayableFromReceiptPort.GeneratePayableFromReceiptCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.GeneratePayableFromReceiptPort.GeneratePayableFromReceiptCommand.Installment;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.RegisterStockEntryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.RegisterStockEntryPort.RegisterStockEntryCommand;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@UseCase
public class ConfirmPurchaseReceiptService implements ConfirmPurchaseReceiptUseCase {

	private final PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;
	private final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
	private final RegisterStockEntryPort registerStockEntryPort;
	private final GeneratePayableFromReceiptPort generatePayableFromReceiptPort;

	public ConfirmPurchaseReceiptService(final PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort,
			final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort, final RegisterStockEntryPort registerStockEntryPort,
			final GeneratePayableFromReceiptPort generatePayableFromReceiptPort) {
		this.purchaseReceiptRepositoryPort = purchaseReceiptRepositoryPort;
		this.purchaseOrderRepositoryPort = purchaseOrderRepositoryPort;
		this.registerStockEntryPort = registerStockEntryPort;
		this.generatePayableFromReceiptPort = generatePayableFromReceiptPort;
	}

	@Override
	public void execute(final ConfirmPurchaseReceiptCommand command) {
		final PurchaseReceipt receipt = purchaseReceiptRepositoryPort.findById(command.receiptId())
				.orElseThrow(() -> new PurchaseReceiptNotFoundException(command.receiptId().value()));
		final PurchaseOrder order = purchaseOrderRepositoryPort.findById(receipt.getOrderId())
				.orElseThrow(() -> new PurchaseOrderNotFoundException(receipt.getOrderId().value()));

		final PurchaseReceipt confirmed = receipt.confirm();

		for (final PurchaseReceiptItem item : confirmed.getReceivedItems()) {
			registerStockEntryPort.registerEntry(new RegisterStockEntryCommand(item.productId(), item.receivedQty(),
					resolveUnitCost(order, item.productId()), confirmed.getId().value()));
		}

		generatePayableFromReceiptPort.generatePayables(new GeneratePayableFromReceiptCommand(confirmed.getId().value(),
				order.getSupplierId().value(), toInstallments(confirmed)));

		purchaseReceiptRepositoryPort.save(confirmed);

		final List<PurchaseReceipt> otherConfirmedReceipts = purchaseReceiptRepositoryPort.findByOrderId(order.getId())
				.stream()
				.filter(other -> !other.getId().equals(confirmed.getId()))
				.filter(other -> other.getStatus() == PurchaseReceiptStatus.CONFIRMED)
				.toList();

		final boolean fullyReceived = isFullyReceived(order, confirmed, otherConfirmedReceipts);
		purchaseOrderRepositoryPort.save(order.afterReceiptConfirmed(fullyReceived));
	}

	private BigDecimal resolveUnitCost(final PurchaseOrder order, final UUID productId) {
		return order.getItems().stream()
				.filter(item -> item.productId().equals(productId))
				.map(PurchaseOrderItem::unitPrice)
				.findFirst()
				.orElse(BigDecimal.ZERO);
	}

	private List<Installment> toInstallments(final PurchaseReceipt receipt) {
		return receipt.getInstallmentTerms().stream()
				.map(term -> new Installment(term.amount(), term.dueDate()))
				.toList();
	}

	private boolean isFullyReceived(final PurchaseOrder order, final PurchaseReceipt justConfirmed,
			final List<PurchaseReceipt> otherConfirmedReceipts) {
		final Map<UUID, BigDecimal> receivedByProduct = new HashMap<>();
		accumulate(receivedByProduct, justConfirmed);
		otherConfirmedReceipts.forEach(other -> accumulate(receivedByProduct, other));

		return order.getItems().stream()
				.allMatch(orderItem -> receivedByProduct.getOrDefault(orderItem.productId(), BigDecimal.ZERO)
						.compareTo(orderItem.quantity()) >= 0);
	}

	private void accumulate(final Map<UUID, BigDecimal> receivedByProduct, final PurchaseReceipt receipt) {
		for (final PurchaseReceiptItem item : receipt.getReceivedItems()) {
			receivedByProduct.merge(item.productId(), item.receivedQty(), BigDecimal::add);
		}
	}
}
