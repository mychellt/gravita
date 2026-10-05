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

@UseCase
public class ReceivePurchaseOrderService implements ReceivePurchaseOrderUseCase {

	private final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;
	private final PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;

	public ReceivePurchaseOrderService(final PurchaseOrderRepositoryPort purchaseOrderRepositoryPort,
			final PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort) {
		this.purchaseOrderRepositoryPort = purchaseOrderRepositoryPort;
		this.purchaseReceiptRepositoryPort = purchaseReceiptRepositoryPort;
	}

	@Override
	public PurchaseReceiptId execute(final ReceivePurchaseOrderCommand command) {
		final PurchaseOrder order = purchaseOrderRepositoryPort.findById(command.orderId())
				.orElseThrow(() -> new PurchaseOrderNotFoundException(command.orderId().value()));

		order.assertReceivable();

		final List<PurchaseReceiptItem> items = command.receivedItems().stream()
				.map(item -> new PurchaseReceiptItem(item.productId(), resolveOrderedQty(order, item), item.receivedQty()))
				.toList();

		final PurchaseReceiptId id = PurchaseReceiptId.of(UUID.randomUUID());
		final PurchaseReceipt receipt = PurchaseReceipt.pending(id, order.getId(), items);
		final PurchaseReceipt saved = purchaseReceiptRepositoryPort.save(receipt);
		return saved.getId();
	}

	private BigDecimal resolveOrderedQty(final PurchaseOrder order, final ReceivedItem item) {
		return order.getItems().stream()
				.filter(orderItem -> orderItem.productId().equals(item.productId()))
				.map(PurchaseOrderItem::quantity)
				.findFirst()
				.orElseThrow(() -> new BusinessRuleException(
						"Product " + item.productId() + " is not part of order " + order.getId().value()));
	}
}
