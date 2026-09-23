package br.gravita.core.usercases.purchasing;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.ReceivePurchaseOrderCommand;
import br.gravita.core.ports.inbound.purchasing.ReceivePurchaseOrderCommand.ReceivedItem;
import br.gravita.core.ports.inbound.purchasing.ReceivePurchaseOrderUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * UC-M6-06. Records the physical conference of a delivery against an order as
 * a {@code PENDING_CONFERENCE} {@link PurchaseReceipt}; {@code orderedQty} per
 * item is resolved from the order itself since the command only carries what
 * was physically received. Stock/payable side effects only happen later, once
 * {@code ConfirmPurchaseReceiptUseCase} (UC-M6-08) confirms the receipt.
 */
@UseCase
public class ReceivePurchaseOrderService implements ReceivePurchaseOrderUseCase {

	private final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
	private final PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;

	public ReceivePurchaseOrderService(PurchaseOrderRepositoryPort purchaseOrderRepositoryPort,
			PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort) {
		this.purchaseOrderRepositoryPort = purchaseOrderRepositoryPort;
		this.purchaseReceiptRepositoryPort = purchaseReceiptRepositoryPort;
	}

	@Override
	public PurchaseReceiptId execute(ReceivePurchaseOrderCommand command) {
		PurchaseOrder order = purchaseOrderRepositoryPort.findById(command.orderId())
				.orElseThrow(() -> new PurchaseOrderNotFoundException(command.orderId().value()));

		// Guards "order must be OPEN/PARTIALLY_RECEIVED" and "not pending approval"
		// before any receipt item is built.
		order.assertReceivable();

		List<PurchaseReceiptItem> items = command.receivedItems().stream()
				.map(item -> new PurchaseReceiptItem(item.productId(), resolveOrderedQty(order, item), item.receivedQty()))
				.toList();

		PurchaseReceiptId id = PurchaseReceiptId.of(UUID.randomUUID());
		PurchaseReceipt receipt = PurchaseReceipt.pending(id, order.getId(), items);
		PurchaseReceipt saved = purchaseReceiptRepositoryPort.save(receipt);
		return saved.getId();
	}

	private BigDecimal resolveOrderedQty(PurchaseOrder order, ReceivedItem item) {
		return order.getItems().stream()
				.filter(orderItem -> orderItem.productId().equals(item.productId()))
				.map(PurchaseOrderItem::quantity)
				.findFirst()
				.orElseThrow(() -> new BusinessRuleException(
						"Product " + item.productId() + " is not part of order " + order.getId().value()));
	}
}
